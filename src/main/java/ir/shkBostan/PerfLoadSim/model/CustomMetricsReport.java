package ir.shkBostan.PerfLoadSim.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Performance metrics report generated after executing a custom load test.
 * <p>
 * Contains aggregated statistics about the test run, including total request count,
 * success/failure counts, response time percentiles, and mean/max response times.
 * This report is produced by the {@code CustomLoadTestService} after a test completes.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 */
@Data
@Builder
@Schema(description = "Aggregated performance metrics of a load test run")
public class CustomMetricsReport {

    /**
     * Total number of HTTP requests sent during the test.
     */
    @Schema(description = "Total number of HTTP requests sent", example = "1000")
    private long totalRequests;

    /**
     * Number of successful requests (HTTP status code 2xx).
     */
    @Schema(description = "Number of successful requests (HTTP 2xx)", example = "998")
    private long successCount;

    /**
     * Number of failed requests (non-2xx status code or network errors).
     */
    @Schema(description = "Number of failed requests (non-2xx or network error)", example = "2")
    private long failedCount;

    /**
     * Mean (average) response time in milliseconds across all successful requests.
     */
    @Schema(description = "Mean response time in milliseconds", example = "45.2")
    private double meanResponseTimeMs;

    /**
     * Maximum response time observed during the test (in milliseconds).
     */
    @Schema(description = "Maximum response time observed (ms)", example = "320")
    private long maxResponseTimeMs;

    /**
     * 95th percentile response time (in milliseconds), meaning 95% of requests
     * completed within this duration.
     */
    @Schema(description = "95th percentile response time (ms)", example = "210")
    private long percentile95;

    /**
     * Returns a formatted string representation containing all metrics
     * for logging and debugging purposes.
     *
     * @return a human-readable string with all report fields
     */
    @Override
    public String toString() {
        return String.format("CustomMetricsReport{totalRequests=%d, successCount=%d, failedCount=%d, "
                        + "meanResponseTimeMs=%.2f, maxResponseTimeMs=%d, percentile95=%d}",
                totalRequests, successCount, failedCount, meanResponseTimeMs, maxResponseTimeMs, percentile95);
    }
}