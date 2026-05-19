CREATE TABLE countries (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL
);

CREATE TABLE customer_status (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL
);

CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    external_id TEXT UNIQUE NOT NULL,
    name TEXT,
    email TEXT,
    country_id BIGINT REFERENCES countries(id),
    status_id BIGINT REFERENCES customer_status(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_external_id ON customers (external_id);
CREATE INDEX idx_customers_country_id ON customers (country_id);
CREATE INDEX idx_customers_status_id ON customers (status_id);

INSERT INTO countries (code, name)
VALUES
    ('US', 'United States'),
    ('IN', 'India'),
    ('UK', 'United Kingdom');

INSERT INTO customer_status (code, name)
VALUES
    ('ACTIVE', 'Active'),
    ('INACTIVE', 'Inactive');
