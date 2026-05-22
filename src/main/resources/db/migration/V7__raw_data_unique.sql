DELETE r1 FROM raw_data r1
    INNER JOIN raw_data r2
    ON r1.collection_log_id = r2.collection_log_id AND r1.id < r2.id;

ALTER TABLE raw_data
    DROP INDEX idx_raw_log,
    ADD UNIQUE KEY uk_collection_log (collection_log_id);
