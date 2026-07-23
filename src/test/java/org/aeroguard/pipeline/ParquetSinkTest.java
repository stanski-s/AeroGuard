package org.aeroguard.pipeline;

import org.aeroguard.model.Telemetry;
import org.aeroguard.model.TelemetryRecord;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.apache.flink.api.common.serialization.BulkWriter;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParquetSinkTest {

    @Test
    void testTelemetryRecordSchemaGeneration() {
        Schema schema = ReflectData.get().getSchema(TelemetryRecord.class);
        assertNotNull(schema);
        assertNotNull(schema.getField("assetId"));
        assertNotNull(schema.getField("sensorId"));
        assertNotNull(schema.getField("timestamp"));
        assertEquals(Schema.Type.LONG, schema.getField("timestamp").schema().getType());
        assertNotNull(schema.getField("vibration"));
        assertNotNull(schema.getField("temperature"));
    }

    @Test
    void testTelemetryRecordConversion() {
        Instant now = Instant.parse("2026-07-23T20:00:00Z");
        Telemetry telemetry = new Telemetry("asset-01", "sensor-01", now, 0.042, 78.5);
        TelemetryRecord record = TelemetryRecord.fromTelemetry(telemetry);

        assertEquals("asset-01", record.getAssetId());
        assertEquals("sensor-01", record.getSensorId());
        assertEquals(now.toEpochMilli(), record.getTimestamp());
        assertEquals(0.042, record.getVibration(), 0.0001);
        assertEquals(78.5, record.getTemperature(), 0.0001);
    }

    @Test
    void testWriteTelemetryRecordToParquet(@TempDir Path tempDir) throws Exception {
        Path filePath = tempDir.resolve("test-telemetry.parquet");
        org.apache.flink.core.fs.Path flinkPath = new org.apache.flink.core.fs.Path(filePath.toUri());

        BulkWriter.Factory<TelemetryRecord> writerFactory = ParquetAvroWriters.forReflectRecord(TelemetryRecord.class);
        org.apache.flink.core.fs.FSDataOutputStream outputStream = flinkPath.getFileSystem().create(flinkPath, org.apache.flink.core.fs.FileSystem.WriteMode.OVERWRITE);

        BulkWriter<TelemetryRecord> writer = writerFactory.create(outputStream);
        Telemetry telemetry = new Telemetry("asset-01", "sensor-01", Instant.now(), 0.05, 75.5);
        writer.addElement(TelemetryRecord.fromTelemetry(telemetry));
        writer.flush();
        writer.finish();
        outputStream.close();

        assertTrue(java.nio.file.Files.exists(filePath));
        assertTrue(java.nio.file.Files.size(filePath) > 0);
    }

    @Test
    void testFileSinkBuilderCreation() {
        Configuration flinkConfig = new Configuration();
        flinkConfig.setString("fs.s3a.endpoint", "http://localhost:9000");
        flinkConfig.setString("fs.s3a.access.key", "minioadmin");
        flinkConfig.setString("fs.s3a.secret.key", "minioadmin");
        flinkConfig.setString("fs.s3a.path.style.access", "true");

        FileSink<TelemetryRecord> sink = FileSink
                .forBulkFormat(new org.apache.flink.core.fs.Path("s3a://aeroguard-telemetry/raw"), ParquetAvroWriters.forReflectRecord(TelemetryRecord.class))
                .build();
        assertNotNull(sink);
    }
}
