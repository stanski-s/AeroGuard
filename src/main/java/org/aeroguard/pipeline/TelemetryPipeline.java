package org.aeroguard.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aeroguard.model.Alert;
import org.aeroguard.model.Telemetry;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;

public class TelemetryPipeline {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryPipeline.class);
    
    private static final String TOPIC = "telemetry.raw";
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

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        DataStream<Telemetry> telemetryStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .map(json -> mapper.readValue(json, Telemetry.class))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Telemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp().toEpochMilli())
                );

        DataStream<TurbineMetric> aggregatedStream = telemetryStream
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

        DataStream<Alert> alertStream = telemetryStream
                .keyBy(Telemetry::getAssetId)
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
                .map(alert -> mapper.writeValueAsString(alert))
                .sinkTo(alertKafkaSink);

        env.execute("End-to-end Telemetry Ingestion");
    }

    public static class TurbineMetric {
        public Instant time;
        public String assetId;
        public double avgVibration;
        public double maxTemperature;

        public TurbineMetric() {}

        public TurbineMetric(Instant time, String assetId, double avgVibration, double maxTemperature) {
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

    public static class TelemetryAggregator implements AggregateFunction<Telemetry, Accumulator, TurbineMetric> {
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
            // Rough window time estimation for simplicity, Flink usually passes Window object in WindowFunction.
            // Using a simple AggregateFunction, we'll assign the truncated time to nearest 5m.
            long epoch = value.getTimestamp().toEpochMilli();
            long windowSizeMs = 5 * 60 * 1000;
            long windowStart = (epoch / windowSizeMs) * windowSizeMs;
            accumulator.windowEnd = Instant.ofEpochMilli(windowStart);
            return accumulator;
        }

        @Override
        public TurbineMetric getResult(Accumulator accumulator) {
            return new TurbineMetric(
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
