-- placeholder migration for reference app
CREATE TABLE IF NOT EXISTS customer_work (
  id BIGINT PRIMARY KEY,
  external_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
