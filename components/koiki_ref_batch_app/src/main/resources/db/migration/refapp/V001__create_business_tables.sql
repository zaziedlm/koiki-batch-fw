-- Business tables for the customer-import reference job.
-- customer_input is the staging source; customer is the validated destination.

CREATE TABLE customer_input (
  id BIGINT PRIMARY KEY,
  external_id VARCHAR(64),
  email VARCHAR(255),
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE customer (
  id BIGINT PRIMARY KEY,
  external_id VARCHAR(64) NOT NULL,
  email VARCHAR(255) NOT NULL
);
