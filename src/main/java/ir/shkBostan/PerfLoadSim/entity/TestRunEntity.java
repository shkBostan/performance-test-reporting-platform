package ir.shkBostan.PerfLoadSim.entity;

import ir.shkBostan.PerfLoadSim.repository.TestRunRepository;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a complete load test execution summary.
 * <p>
 * Each instance of this entity corresponds to one run of a load test scenario.
 * It stores both the test configuration (target URL, RPS, duration, etc.) and the
 * aggregated performance metrics (total requests, success/failure counts, response
 * time statistics). The entity is the parent of all per‑request details stored in
 * {@link RequestResultEntity}.
 * </p>
 * <p>
 * Instances are created when a test finishes and are persisted asynchronously to
 * avoid blocking the test execution thread. The {@code status} field indicates
 * whether the test is still running, has completed normally, or failed unexpectedly.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see RequestResultEntity
 * @see TestRunRepository
 */
@Entity
@Table(name = "test_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRunEntity {

    /**
     * Unique identifier of the test run.
     * <p>
     * This is the same UUID that is returned immediately when a test is started
     * and later used to query status, reports, and detailed results.
     * </p>
     */
    @Id
    private UUID id;

    /**
     * Name of the scenario that was executed (e.g., "search", "bulk", "bulkQs").
     */
    private String scenarioName;

    /**
     * Full URL of the target API endpoint that was tested.
     */
    private String targetUrl;

    /**
     * Target number of requests per second (RPS) that the test tried to sustain.
     */
    private int targetRps;

    /**
     * Duration of the test in seconds.
     */
    private int durationSeconds;

    /**
     * Maximum allowed response time in milliseconds. This value was used
     * as a timeout for each request; requests exceeding this threshold are
     * considered failed.
     */
    private int maxResponseTimeMs;

    /**
     * Timestamp when the test started (with nanosecond precision).
     */
    private LocalDateTime startTime;

    /**
     * Timestamp when the test finished.
     */
    private LocalDateTime endTime;

    /**
     * Total number of HTTP requests sent during the test.
     * <p>
     * This is typically equal to {@code targetRps * durationSeconds},
     * but may be slightly less if the test was interrupted.
     * </p>
     */
    private long totalRequests;

    /**
     * Number of successful requests (HTTP status code 2xx).
     */
    private long successCount;

    /**
     * Number of failed requests (non‑2xx status code or network errors).
     */
    private long failedCount;

    /**
     * Mean (average) response time in milliseconds across all successful requests.
     */
    private double meanResponseTimeMs;

    /**
     * Maximum response time observed during the test (in milliseconds).
     */
    private long maxResponseTimeMsObserved;

    /**
     * 95th percentile response time (in milliseconds).
     * <p>
     * This value means that 95% of the requests completed within this duration.
     * </p>
     */
    private long percentile95;

    /**
     * Current status of the test run.
     * <p>
     * Possible values:
     * <ul>
     *   <li>{@code RUNNING} – the test is still executing</li>
     *   <li>{@code COMPLETED} – the test finished normally and all data are saved</li>
     *   <li>{@code FAILED} – the test encountered an unrecoverable error</li>
     * </ul>
     * </p>
     */
    private String status;
}