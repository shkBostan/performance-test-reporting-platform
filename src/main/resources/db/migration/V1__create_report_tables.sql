-- =============================================================================
-- Flyway Migration V1__create_report_tables.sql
-- =============================================================================
-- Creates the database schema for storing load test reports and per‑request details.
--
-- This migration is executed automatically by Flyway on application startup.
-- It defines two tables:
--   - test_run         : stores aggregated test run summaries
--   - request_result   : stores detailed results for each individual HTTP request
--
-- Indexes are created on frequently queried columns to ensure optimal performance
-- when retrieving historical test data.
--
-- @author s Bostan
-- @since Apr, 2026
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: test_run
-- Stores aggregated metrics and metadata for a complete load test execution.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS test_run (
                                        id                          UUID PRIMARY KEY,          -- Unique test identifier (same as returned to client)
                                        scenario_name               VARCHAR(100) NOT NULL,     -- Name of the scenario (e.g., 'search', 'bulk')
    target_url                  TEXT,                      -- Full URL of the tested endpoint
    target_rps                  INT,                       -- Target requests per second during the test
    duration_seconds            INT,                       -- Test duration in seconds
    max_response_time_ms        INT,                       -- Maximum allowed response time (timeout)
    start_time                  TIMESTAMP NOT NULL,        -- Test start timestamp (with nanosecond precision)
    end_time                    TIMESTAMP,                 -- Test end timestamp (nullable if test failed to finish)
    total_requests              BIGINT,                    -- Total number of HTTP requests sent
    success_count               BIGINT,                    -- Number of successful requests (HTTP 2xx)
    failed_count                BIGINT,                    -- Number of failed requests (non‑2xx or network error)
    mean_response_time_ms       FLOAT,                     -- Average response time in milliseconds
    max_response_time_ms_observed BIGINT,                  -- Maximum response time observed (ms)
    percentile95                BIGINT,                    -- 95th percentile response time (ms)
    status                      VARCHAR(20)                -- Test status: RUNNING, COMPLETED, FAILED
    );

-- -----------------------------------------------------------------------------
-- Table: request_result
-- Stores individual request details for a specific test run.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS request_result (
                                              id                          BIGSERIAL PRIMARY KEY,     -- Auto‑incrementing primary key
                                              test_run_id                 UUID NOT NULL              -- Foreign key to test_run(id)
                                              REFERENCES test_run(id) ON DELETE CASCADE,
    response_time_ms            BIGINT,                    -- Response time of this request (ms)
    success                     BOOLEAN,                   -- true if HTTP status is 2xx, false otherwise
    http_status_code            INT,                       -- Actual HTTP status code (e.g., 200, 404, 500)
    error_message               TEXT,                      -- Error details (response preview or exception message)
    request_timestamp           TIMESTAMP                  -- When the request was executed
    );

-- -----------------------------------------------------------------------------
-- Indexes for performance optimisation
-- -----------------------------------------------------------------------------

-- Sorts test runs by start time (most recent first) – used in report listing
CREATE INDEX IF NOT EXISTS idx_test_run_start_time ON test_run(start_time DESC);

-- Speeds up queries filtering request results by test_run_id (most common access pattern)
CREATE INDEX IF NOT EXISTS idx_request_result_test_run_id ON request_result(test_run_id);

-- Allows fast counting/filtering on success status (e.g., only failed requests)
CREATE INDEX IF NOT EXISTS idx_request_result_success ON request_result(success);