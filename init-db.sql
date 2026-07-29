CREATE TABLE IF NOT EXISTS turbine_metrics_1m (
    time TIMESTAMPTZ NOT NULL,
    asset_id VARCHAR(255) NOT NULL,
    avg_vibration DOUBLE PRECISION,
    max_temperature DOUBLE PRECISION,
    avg_power_output_mw DOUBLE PRECISION,
    avg_pitch_angle_deg DOUBLE PRECISION,
    avg_rotor_speed_rpm DOUBLE PRECISION,
    max_nacelle_temp_c DOUBLE PRECISION,
    PRIMARY KEY (time, asset_id)
);

SELECT create_hypertable('turbine_metrics_1m', 'time', if_not_exists => TRUE);

CREATE TABLE IF NOT EXISTS alerts (
    alert_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    asset_id VARCHAR(255) NOT NULL,
    alert_type VARCHAR(100) NOT NULL,
    trigger_value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    vibration DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    power_output_mw DOUBLE PRECISION,
    pitch_angle_deg DOUBLE PRECISION,
    rotor_speed_rpm DOUBLE PRECISION,
    nacelle_temp_c DOUBLE PRECISION,
    message TEXT,
    action_id VARCHAR(255),
    action_title VARCHAR(255),
    PRIMARY KEY (timestamp, alert_id)
);

ALTER TABLE alerts DROP COLUMN IF EXISTS sensor_id;

SELECT create_hypertable('alerts', 'timestamp', if_not_exists => TRUE);
