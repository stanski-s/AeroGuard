package org.aeroguard.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aeroguard.model.Alert;
import org.aeroguard.model.AssetEvent;
import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.ConfigEvent;
import org.aeroguard.model.DiagnosticActionRule;
import org.aeroguard.model.Telemetry;
import org.aeroguard.model.ThresholdConfig;
import org.aeroguard.util.JsonMapperUtil;
import org.aeroguard.model.TelemetryRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class TelemetryPipeline {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryPipeline.class);
    
    private static final String TOPIC = "telemetry.raw";
    private static final String DLQ_TOPIC = "telemetry.dlq";
    private static final String THRESHOLD_TOPIC = "config.thresholds";
    private static final String DIAGNOSTIC_ACTIONS_TOPIC = "config.diagnostic-actions";
    private static final String OPERATING_MODE_TOPIC = "events.status";
    private static final String ALERT_TOPIC = "alerts.critical";
    private static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/aeroguard");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "admin");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password");
    private static final String S3_ENDPOINT = System.getenv().getOrDefault("S3_ENDPOINT", "http://localhost:9000");
    private static final String S3_ACCESS_KEY = System.getenv().getOrDefault("S3_ACCESS_KEY", "minioadmin");
    private static final String S3_SECRET_KEY = System.getenv().getOrDefault("S3_SECRET_KEY", "minioadmin");
    private static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET", "aeroguard-telemetry");
    private static final String S3_PATH = System.getenv().getOrDefault("S3_PATH", "file:///tmp/" + S3_BUCKET + "/raw");

    public static final OutputTag<String> DLQ_TAG = new OutputTag<>("telemetry-dlq", Types.STRING);

    private static void ensureTopicsExist() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        try (AdminClient adminClient = AdminClient.create(props)) {
            List<String> requiredTopics = List.of(TOPIC, DLQ_TOPIC, THRESHOLD_TOPIC, DIAGNOSTIC_ACTIONS_TOPIC, OPERATING_MODE_TOPIC, ALERT_TOPIC);
            Set<String> existingTopics = adminClient.listTopics().names().get();
            List<NewTopic> newTopics = requiredTopics.stream()
                    .filter(t -> !existingTopics.contains(t))
                    .map(t -> new NewTopic(t, 1, (short) 1))
                    .collect(Collectors.toList());
            if (!newTopics.isEmpty()) {
                adminClient.createTopics(newTopics).all().get();
                logger.info("Successfully created missing Kafka topics: {}", newTopics);
            }
        } catch (Exception e) {
            logger.warn("Could not pre-create Kafka topics via AdminClient: {}", e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        ensureTopicsExist();

        System.setProperty("HADOOP_USER_NAME", "minioadmin");
        Configuration flinkConfig = new Configuration();
        flinkConfig.setString("metrics.reporter.prom.factory.class", "org.apache.flink.metrics.prometheus.PrometheusReporterFactory");
        flinkConfig.setString("metrics.reporter.prom.port", System.getenv().getOrDefault("PROMETHEUS_REPORTER_PORT", "9249"));
        flinkConfig.setString("security.delegation.token.provider.hadoopfs.enabled", "false");
        flinkConfig.setString("security.delegation.token.provider.hbase.enabled", "false");
        flinkConfig.setString("fs.s3a.endpoint", S3_ENDPOINT);
        flinkConfig.setString("fs.s3a.access.key", S3_ACCESS_KEY);
        flinkConfig.setString("fs.s3a.secret.key", S3_SECRET_KEY);
        flinkConfig.setString("fs.s3a.path.style.access", "true");
        flinkConfig.setString("fs.s3a.ssl.enabled", "false");
        flinkConfig.setString("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        flinkConfig.setString("fs.allowed-fallback-filesystems", "s3a");
        org.apache.flink.core.fs.FileSystem.initialize(flinkConfig, null);

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(flinkConfig);
        long checkpointInterval = Long.parseLong(System.getenv().getOrDefault("CHECKPOINT_INTERVAL_MS", "30000"));
        env.enableCheckpointing(checkpointInterval);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(TOPIC)
                .setGroupId("telemetry-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        SingleOutputStreamOperator<Telemetry> parsedTelemetryStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Telemetry Source")
                .process(new TelemetryParsingProcessFunction())
                .name("Parse Telemetry JSON with DLQ");

        DataStream<String> dlqStream = parsedTelemetryStream.getSideOutput(DLQ_TAG);

        KafkaSink<String> dlqKafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<String>builder()
                                .setTopic(DLQ_TOPIC)
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .build();

        dlqStream.sinkTo(dlqKafkaSink).name("Telemetry → Kafka DLQ");

        DataStream<Telemetry> telemetryStream = parsedTelemetryStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Telemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withIdleness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp().toEpochMilli())
                );

        FileSink<TelemetryRecord> parquetSink = FileSink
                .forBulkFormat(new org.apache.flink.core.fs.Path(S3_PATH), ParquetAvroWriters.forReflectRecord(TelemetryRecord.class))
                .withBucketAssigner(new DateTimeBucketAssigner<>("yyyy-MM-dd-HH"))
                .build();

        telemetryStream
                .map(TelemetryRecord::fromTelemetry)
                .sinkTo(parquetSink);

        KafkaSource<String> thresholdSource = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(THRESHOLD_TOPIC)
                .setGroupId("threshold-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<ConfigEvent> thresholdStream = env
                .fromSource(thresholdSource, WatermarkStrategy.noWatermarks(), "Kafka Threshold Source")
                .map(new ThresholdDeserializer())
                .map(ConfigEvent::fromThreshold);

        KafkaSource<String> diagnosticActionSource = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(DIAGNOSTIC_ACTIONS_TOPIC)
                .setGroupId("diagnostic-action-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<ConfigEvent> diagnosticActionStream = env
                .fromSource(diagnosticActionSource, WatermarkStrategy.noWatermarks(), "Kafka Diagnostic Actions Source")
                .map(new DiagnosticActionRuleDeserializer())
                .map(ConfigEvent::fromDiagnosticActionRule);

        DataStream<ConfigEvent> configStream = thresholdStream.union(diagnosticActionStream);

        KafkaSource<String> operatingModeSource = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(OPERATING_MODE_TOPIC)
                .setGroupId("operating-mode-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<AssetOperatingModeEvent> operatingModeStream = env
                .fromSource(operatingModeSource, WatermarkStrategy.noWatermarks(), "Kafka Operating Mode Source")
                .map(new AssetOperatingModeDeserializer())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<AssetOperatingModeEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withIdleness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getTimestamp() != null ? event.getTimestamp().toEpochMilli() : System.currentTimeMillis())
                );

        BroadcastStream<ConfigEvent> broadcastConfig = configStream
                .broadcast(
                        ThermalSpikeProcessFunction.THRESHOLD_STATE_DESCRIPTOR,
                        ThermalSpikeProcessFunction.DIAGNOSTIC_ACTION_STATE_DESCRIPTOR
                );

        long allowedLatenessSec = Long.parseLong(System.getenv().getOrDefault("ALLOWED_LATENESS_SECONDS", "10"));

        DataStream<AssetMetric> aggregatedStream = telemetryStream
                .keyBy(Telemetry::getAssetId)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .allowedLateness(Time.seconds(allowedLatenessSec))
                .aggregate(new TelemetryAggregator());

        aggregatedStream.addSink(JdbcSink.sink(
                "INSERT INTO turbine_metrics_1m (time, asset_id, avg_vibration, max_temperature, avg_power_output_mw, avg_pitch_angle_deg, avg_rotor_speed_rpm, max_nacelle_temp_c) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (time, asset_id) DO UPDATE SET " +
                        "avg_vibration = EXCLUDED.avg_vibration, " +
                        "max_temperature = EXCLUDED.max_temperature, " +
                        "avg_power_output_mw = EXCLUDED.avg_power_output_mw, " +
                        "avg_pitch_angle_deg = EXCLUDED.avg_pitch_angle_deg, " +
                        "avg_rotor_speed_rpm = EXCLUDED.avg_rotor_speed_rpm, " +
                        "max_nacelle_temp_c = EXCLUDED.max_nacelle_temp_c",
                (statement, metric) -> {
                    statement.setTimestamp(1, Timestamp.from(metric.time));
                    statement.setString(2, metric.assetId);
                    statement.setDouble(3, metric.avgVibration);
                    statement.setDouble(4, metric.maxTemperature);
                    statement.setDouble(5, metric.avgPowerOutputMw);
                    statement.setDouble(6, metric.avgPitchAngleDeg);
                    statement.setDouble(7, metric.avgRotorSpeedRpm);
                    statement.setDouble(8, metric.maxNacelleTempC);
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(100)
                        .withBatchIntervalMs(200)
                        .withMaxRetries(5)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(DB_URL)
                        .withDriverName("org.postgresql.Driver")
                        .withUsername(DB_USER)
                        .withPassword(DB_PASSWORD)
                        .build()
        ));

        DataStream<AssetEvent> telemetryEvents = telemetryStream.map(AssetEvent::fromTelemetry);
        DataStream<AssetEvent> modeEvents = operatingModeStream.map(AssetEvent::fromOperatingMode);

        DataStream<AssetEvent> combinedAssetStream = telemetryEvents.union(modeEvents);

        DataStream<Alert> alertStream = combinedAssetStream
                .keyBy(AssetEvent::getAssetId)
                .connect(broadcastConfig)
                .process(new ThermalSpikeProcessFunction(80.0));

        KafkaSink<String> alertKafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<String>builder()
                                .setTopic(ALERT_TOPIC)
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .build();

        alertStream
                .map(new AlertSerializer())
                .sinkTo(alertKafkaSink);

        alertStream.addSink(JdbcSink.sink(
                "INSERT INTO alerts (alert_id, timestamp, asset_id, alert_type, trigger_value, threshold, vibration, temperature, power_output_mw, pitch_angle_deg, rotor_speed_rpm, nacelle_temp_c, message, action_id, action_title) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (timestamp, alert_id) DO NOTHING",
                (statement, alert) -> {
                    statement.setString(1, alert.getAlertId());
                    statement.setTimestamp(2, Timestamp.from(alert.getTimestamp() != null ? alert.getTimestamp() : Instant.now()));
                    statement.setString(3, alert.getAssetId());
                    statement.setString(4, alert.getAlertType());
                    statement.setDouble(5, alert.getTriggerValue());
                    statement.setDouble(6, alert.getThreshold());
                    statement.setDouble(7, alert.getVibration());
                    statement.setDouble(8, alert.getTemperature());
                    statement.setDouble(9, alert.getPowerOutputMw());
                    statement.setDouble(10, alert.getPitchAngleDeg());
                    statement.setDouble(11, alert.getRotorSpeedRpm());
                    statement.setDouble(12, alert.getNacelleTempC());
                    statement.setString(13, alert.getMessage());
                    statement.setString(14, alert.getDiagnosticAction() != null ? alert.getDiagnosticAction().getActionId() : null);
                    statement.setString(15, alert.getDiagnosticAction() != null ? alert.getDiagnosticAction().getTitle() : null);
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(1)
                        .withBatchIntervalMs(100)
                        .withMaxRetries(5)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(DB_URL)
                        .withDriverName("org.postgresql.Driver")
                        .withUsername(DB_USER)
                        .withPassword(DB_PASSWORD)
                        .build()
        ));

        env.execute("End-to-end Telemetry Ingestion");
    }

    public static class TelemetryParsingProcessFunction extends ProcessFunction<String, Telemetry> {
        private transient ObjectMapper mapper;
        private transient org.apache.flink.metrics.Counter successCounter;
        private transient org.apache.flink.metrics.Counter malformedCounter;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
            successCounter = getRuntimeContext().getMetricGroup().counter("telemetry_deserialized_success");
            malformedCounter = getRuntimeContext().getMetricGroup().counter("malformed_json_records");
        }

        @Override
        public void processElement(String value, Context ctx, Collector<Telemetry> out) throws Exception {
            try {
                Telemetry t = mapper.readValue(value, Telemetry.class);
                if (t != null && t.getAssetId() != null && t.getTimestamp() != null) {
                    out.collect(t);
                    if (successCounter != null) {
                        successCounter.inc();
                    }
                } else {
                    if (malformedCounter != null) {
                        malformedCounter.inc();
                    }
                    ctx.output(DLQ_TAG, value);
                }
            } catch (Exception e) {
                if (malformedCounter != null) {
                    malformedCounter.inc();
                }
                logger.warn("Malformed JSON received in telemetry.raw, routing to DLQ: {}", e.getMessage());
                ctx.output(DLQ_TAG, value);
            }
        }
    }

    public static class TelemetryDeserializer extends RichMapFunction<String, Telemetry> {
        private transient ObjectMapper mapper;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
        }

        @Override
        public Telemetry map(String json) throws Exception {
            return mapper.readValue(json, Telemetry.class);
        }
    }

    public static class ThresholdDeserializer extends RichMapFunction<String, ThresholdConfig> {
        private transient ObjectMapper mapper;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
        }

        @Override
        public ThresholdConfig map(String json) throws Exception {
            return mapper.readValue(json, ThresholdConfig.class);
        }
    }

    public static class DiagnosticActionRuleDeserializer extends RichMapFunction<String, DiagnosticActionRule> {
        private transient ObjectMapper mapper;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
        }

        @Override
        public DiagnosticActionRule map(String json) throws Exception {
            return mapper.readValue(json, DiagnosticActionRule.class);
        }
    }

    public static class AssetOperatingModeDeserializer extends RichMapFunction<String, AssetOperatingModeEvent> {
        private transient ObjectMapper mapper;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
        }

        @Override
        public AssetOperatingModeEvent map(String json) throws Exception {
            return mapper.readValue(json, AssetOperatingModeEvent.class);
        }
    }

    public static class AlertSerializer extends RichMapFunction<Alert, String> {
        private transient ObjectMapper mapper;

        @Override
        public void open(Configuration parameters) throws Exception {
            mapper = JsonMapperUtil.getMapper();
        }

        @Override
        public String map(Alert alert) throws Exception {
            return mapper.writeValueAsString(alert);
        }
    }

    public static class AssetMetric {
        public Instant time;
        public String assetId;
        public double avgVibration;
        public double maxTemperature;
        public double avgPowerOutputMw;
        public double avgPitchAngleDeg;
        public double avgRotorSpeedRpm;
        public double maxNacelleTempC;

        public AssetMetric() {}

        public AssetMetric(Instant time, String assetId, double avgVibration, double maxTemperature,
                           double avgPowerOutputMw, double avgPitchAngleDeg, double avgRotorSpeedRpm, double maxNacelleTempC) {
            this.time = time;
            this.assetId = assetId;
            this.avgVibration = avgVibration;
            this.maxTemperature = maxTemperature;
            this.avgPowerOutputMw = avgPowerOutputMw;
            this.avgPitchAngleDeg = avgPitchAngleDeg;
            this.avgRotorSpeedRpm = avgRotorSpeedRpm;
            this.maxNacelleTempC = maxNacelleTempC;
        }
    }

    public static class Accumulator {
        public String assetId;
        public double sumVibration = 0;
        public long countVibration = 0;
        public double maxTemperature = Double.MIN_VALUE;

        public double sumPowerOutputMw = 0;
        public long countPowerOutputMw = 0;

        public double sumPitchAngleDeg = 0;
        public long countPitchAngleDeg = 0;

        public double sumRotorSpeedRpm = 0;
        public long countRotorSpeedRpm = 0;

        public double maxNacelleTempC = Double.MIN_VALUE;
        public Instant windowEnd;
    }

    public static class TelemetryAggregator implements AggregateFunction<Telemetry, Accumulator, AssetMetric> {
        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public Accumulator add(Telemetry value, Accumulator accumulator) {
            accumulator.assetId = value.getAssetId();
            accumulator.sumVibration += value.getVibration();
            accumulator.countVibration++;
            accumulator.maxTemperature = Math.max(accumulator.maxTemperature, value.getTemperature());

            accumulator.sumPowerOutputMw += value.getPowerOutputMw();
            accumulator.countPowerOutputMw++;

            accumulator.sumPitchAngleDeg += value.getPitchAngleDeg();
            accumulator.countPitchAngleDeg++;

            accumulator.sumRotorSpeedRpm += value.getRotorSpeedRpm();
            accumulator.countRotorSpeedRpm++;

            accumulator.maxNacelleTempC = Math.max(accumulator.maxNacelleTempC, value.getNacelleTempC());

            long epoch = value.getTimestamp().toEpochMilli();
            long windowSizeMs = 1 * 60 * 1000;
            long windowStart = (epoch / windowSizeMs) * windowSizeMs;
            accumulator.windowEnd = Instant.ofEpochMilli(windowStart);
            return accumulator;
        }

        @Override
        public AssetMetric getResult(Accumulator accumulator) {
            return new AssetMetric(
                    accumulator.windowEnd,
                    accumulator.assetId,
                    accumulator.countVibration == 0 ? 0 : accumulator.sumVibration / accumulator.countVibration,
                    accumulator.maxTemperature == Double.MIN_VALUE ? 0 : accumulator.maxTemperature,
                    accumulator.countPowerOutputMw == 0 ? 0 : accumulator.sumPowerOutputMw / accumulator.countPowerOutputMw,
                    accumulator.countPitchAngleDeg == 0 ? 0 : accumulator.sumPitchAngleDeg / accumulator.countPitchAngleDeg,
                    accumulator.countRotorSpeedRpm == 0 ? 0 : accumulator.sumRotorSpeedRpm / accumulator.countRotorSpeedRpm,
                    accumulator.maxNacelleTempC == Double.MIN_VALUE ? 0 : accumulator.maxNacelleTempC
            );
        }

        @Override
        public Accumulator merge(Accumulator a, Accumulator b) {
            Accumulator acc = new Accumulator();
            acc.assetId = a.assetId != null ? a.assetId : b.assetId;
            acc.sumVibration = a.sumVibration + b.sumVibration;
            acc.countVibration = a.countVibration + b.countVibration;
            acc.maxTemperature = Math.max(a.maxTemperature, b.maxTemperature);

            acc.sumPowerOutputMw = a.sumPowerOutputMw + b.sumPowerOutputMw;
            acc.countPowerOutputMw = a.countPowerOutputMw + b.countPowerOutputMw;

            acc.sumPitchAngleDeg = a.sumPitchAngleDeg + b.sumPitchAngleDeg;
            acc.countPitchAngleDeg = a.countPitchAngleDeg + b.countPitchAngleDeg;

            acc.sumRotorSpeedRpm = a.sumRotorSpeedRpm + b.sumRotorSpeedRpm;
            acc.countRotorSpeedRpm = a.countRotorSpeedRpm + b.countRotorSpeedRpm;

            acc.maxNacelleTempC = Math.max(a.maxNacelleTempC, b.maxNacelleTempC);

            acc.windowEnd = a.windowEnd != null ? a.windowEnd : b.windowEnd;
            return acc;
        }
    }
}
