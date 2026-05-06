package ir.shkBostan.PerfLoadSim.model;

import ir.shkBostan.PerfLoadSim.entity.RequestResultEntity;
import ir.shkBostan.PerfLoadSim.service.CustomLoadTestService;
import ir.shkBostan.PerfLoadSim.service.ReportPersistenceService;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Aggregated container for a complete load test report including both
 * high‑level metrics and per‑request details.
 * <p>
 * This DTO is produced by {@link CustomLoadTestService#runTest}
 * after a load test finishes. It bundles:
 * <ul>
 *   <li>a {@link CustomMetricsReport} – summary statistics (total requests,
 *       success/failure counts, response time percentiles, etc.)</li>
 *   <li>a {@code List} of {@link RequestResultEntity} – detailed information
 *       for every single HTTP request that was sent during the test</li>
 * </ul>
 * </p>
 * <p>
 * The object is later used by {@link ReportPersistenceService}
 * to asynchronously store both the summary and the detailed results into the database.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see CustomMetricsReport
 * @see RequestResultEntity
 * @see CustomLoadTestService#runTest
 * @see ReportPersistenceService
 */
@Data
@Builder
public class DetailedTestReport {
    /**
     * Aggregate performance statistics of the test.
     */
    private CustomMetricsReport metrics;

    /**
     * Detailed result for each individual request executed during the test.
     * <p>
     * Contains response times, HTTP status codes, success/failure flags,
     * error messages, and request timestamps.
     * </p>
     */
    private List<RequestResultEntity> detailedResults;
}