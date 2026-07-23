package org.aeroguard.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aeroguard.model.Alert;
import org.aeroguard.model.AssetEvent;
import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.Telemetry;
import org.aeroguard.model.ThresholdConfig;
import org.aeroguard.util.JsonMapperUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class TelemetryPipeline {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryPipeline.class);
    
    private static final String TOPIC = "telemetry.raw";
    private static final String THRESHOLD_TOPIC = "config.thresholds";
    private static final String STATUS_TOPIC = "events.status";
    private static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/aeroguard");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "admin");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password");

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(TOPIC)
                .setGroupId("telemetry-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<Telemetry> telemetryStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Telemetry Source")
                .map(new TelemetryDeserializer())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Telemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp().toEpochMilli())
                );

        KafkaSource<String> thresholdSource = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(THRESHOLD_TOPIC)
                .setGroupId("threshold-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<ThresholdConfig> thresholdStream = env
                .fromSource(thresholdSource, WatermarkStrategy.noWatermarks(), "Kafka Threshold Source")
                .map(new ThresholdDeserializer());

        KafkaSource<String> operatingModeSource = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(STATUS_TOPIC)
                .setGroupId("operating-mode-pipeline-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<AssetOperatingModeEvent> operatingModeStream = env
                .fromSource(operatingModeSource, WatermarkStrategy.noWatermarks(), "Kafka Operating Mode Source")
                .map(new AssetOperatingModeDeserializer())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<AssetOperatingModeEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, timestamp) ->
                                        event.getTimestamp() != null ? event.getTimestamp().toEpochMilli() : System.currentTimeMillis())
                );

        BroadcastStream<ThresholdConfig> broadcastThresholds = thresholdStream
                .broadcast(ThermalSpikeProcessFunction.THRESHOLD_STATE_DESCRIPTOR);

        DataStream<AssetMetric> aggregatedStream = telemetryStream
                .keyBy(Telemetry::getAssetId)
                .window(TumblingEventTimeWindows.of(Time.minutes(5)))
                .aggregate(new TelemetryAggregator());

        aggregatedStream.addSink(JdbcSink.sink(
                "INSERT INTO turbine_metrics_5m (time, asset_id, avg_vibration, max_temperature) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT (time, asset_id) DO UPDATE SET " +
                        "avg_vibration = EXCLUDED.avg_vibration, " +
                        "max_temperature = EXCLUDED.max_temperature",
                (statement, metric) -> {
                    statement.setTimestamp(1, Timestamp.from(metric.time));
                    statement.setString(2, metric.assetId);
                    statement.setDouble(3, metric.avgVibration);
                    statement.setDouble(4, metric.maxTemperature);
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
                .connect(broadcastThresholds)
                .process(new ThermalSpikeProcessFunction(80.0));

        KafkaSink<String> alertKafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<String>builder()
                                .setTopic("alerts.critical")
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .build();

        alertStream
                .map(new AlertSerializer())
                .sinkTo(alertKafkaSink);

        env.execute("End-to-end Telemetry Ingestion");
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

        public AssetMetric() {}

        public AssetMetric(Instant time, String assetId, double avgVibration, double maxTemperature) {
            this.time = time;
            this.assetId = assetId;
            this.avgVibration = avgVibration;
            this.maxTemperature = maxTemperature;
        }
    }

    public static class Accumulator {
        public String assetId;
        public double sumVibration = 0;
        public long countVibration = 0;
        public double maxTemperature = Double.MIN_VALUE;
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
            long epoch = value.getTimestamp().toEpochMilli();
            long windowSizeMs = 5 * 60 * 1000;
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
                    accumulator.maxTemperature == Double.MIN_VALUE ? 0 : accumulator.maxTemperature
            );
        }

        @Override
        public Accumulator merge(Accumulator a, Accumulator b) {
            Accumulator acc = new Accumulator();
            acc.assetId = a.assetId != null ? a.assetId : b.assetId;
            acc.sumVibration = a.sumVibration + b.sumVibration;
            acc.countVibration = a.countVibration + b.countVibration;
            acc.maxTemperature = Math.max(a.maxTemperature, b.maxTemperature);
            acc.windowEnd = a.windowEnd != null ? a.windowEnd : b.windowEnd;
            return acc;
        }
    }
}
