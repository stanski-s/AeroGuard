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
