CREATE TABLE turbine_metrics_5m (
    time TIMESTAMPTZ NOT NULL,
    asset_id VARCHAR(255) NOT NULL,
    avg_vibration DOUBLE PRECISION,
    max_temperature DOUBLE PRECISION,
    UNIQUE (time, asset_id)
);

SELECT create_hypertable('turbine_metrics_5m', 'time');
