CREATE TABLE IF NOT EXISTS processed_events (
                                                id         BIGSERIAL PRIMARY KEY,
                                                source     VARCHAR(255),
    event_type VARCHAR(255),
    payload    JSONB,
    created_at TIMESTAMP DEFAULT NOW()
    );