package ir.shkBostan.PerfLoadSim.service;

import ir.shkBostan.PerfLoadSim.entity.RequestResultEntity;
import ir.shkBostan.PerfLoadSim.entity.TestRunEntity;
import ir.shkBostan.PerfLoadSim.model.CustomLoadTestRequest;
import ir.shkBostan.PerfLoadSim.model.DetailedTestReport;
import ir.shkBostan.PerfLoadSim.config.AsyncConfig;
import ir.shkBostan.PerfLoadSim.repository.RequestResultRepository;
import ir.shkBostan.PerfLoadSim.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service responsible for asynchronously persisting load test reports to the database.
 * <p>
 * This service receives a {@link DetailedTestReport} (containing both aggregated metrics
 * and per‑request details) after a load test finishes and writes it into the underlying
 * PostgreSQL database using Spring Data JPA. The operation runs asynchronously on a
 * dedicated thread pool to avoid blocking the test execution thread or the HTTP response
 * of the test‑starting endpoint.
 * </p>
 * <p>
 * The persistence flow consists of two steps:
 * <ol>
 *   <li>Saving the {@link TestRunEntity} summary record.</li>
 *   <li>Associating each {@link RequestResultEntity} with that test run and saving all
 *       details in a batch.</li>
 * </ol>
 * </p>
 * <p>
 * The method is annotated with {@link Async} and {@link Transactional} to ensure that
 * the whole operation is executed in a separate thread and within a database transaction.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see TestRunEntity
 * @see RequestResultEntity
 * @see DetailedTestReport
 * @see AsyncConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPersistenceService {

    private final TestRunRepository testRunRepository;
    private final RequestResultRepository requestResultRepository;

    /**
     * Asynchronously saves a complete load test report (summary + details) into the database.
     * <p>
     * This method is called after a test finishes. It first creates a {@link TestRunEntity}
     * from the provided test metadata and aggregated metrics, saves it, then associates
     * every per‑request {@link RequestResultEntity} with that test run and saves all
     * details in a single batch operation. Any exception during the process is logged
     * but does not propagate (the test execution itself has already completed).
     * </p>
     * <p>
     * The method is marked as {@code @Async} so that it runs in a separate thread,
     * managed by the {@link AsyncConfig} thread pool.
     * </p>
     *
     * @param testId       the unique identifier of the test (same as returned to the client)
     * @param scenarioName the name of the scenario that was executed
     * @param request      the original load test request (contains configuration such as URL, RPS, duration)
     * @param report       the detailed report containing both metrics and per‑request results
     * @param startTime    the timestamp when the test started
     * @param endTime      the timestamp when the test finished
     * @return a {@link CompletableFuture} that completes when the persistence operation finishes
     *         (normally or with an exception)
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveTestReportAsync(UUID testId,
                                                       String scenarioName,
                                                       CustomLoadTestRequest request,
                                                       DetailedTestReport report,
                                                       LocalDateTime startTime,
                                                       LocalDateTime endTime) {
        log.info("Async saving test {} to database...", testId);
        try {
            // Build the summary entity
            TestRunEntity testRun = TestRunEntity.builder()
                    .id(testId)
                    .scenarioName(scenarioName)
                    .targetUrl(request.getBaseUrl() + request.getEndpoint())
                    .targetRps(request.getTargetRps())
                    .durationSeconds(request.getDurationSeconds())
                    .maxResponseTimeMs(request.getMaxResponseTimeMs())
                    .startTime(startTime)
                    .endTime(endTime)
                    .totalRequests(report.getMetrics().getTotalRequests())
                    .successCount(report.getMetrics().getSuccessCount())
                    .failedCount(report.getMetrics().getFailedCount())
                    .meanResponseTimeMs(report.getMetrics().getMeanResponseTimeMs())
                    .maxResponseTimeMsObserved(report.getMetrics().getMaxResponseTimeMs())
                    .percentile95(report.getMetrics().getPercentile95())
                    .status("COMPLETED")
                    .build();

            testRunRepository.save(testRun);

            // Associate each detail with the test run and save all in batch
            var results = report.getDetailedResults().stream()
                    .map(r -> {
                        r.setTestRun(testRun);
                        return r;
                    })
                    .collect(Collectors.toList());

            requestResultRepository.saveAll(results);
            log.info("Async save completed for test {}, saved {} details", testId, results.size());
        } catch (Exception e) {
            log.error("Async save failed for test {}", testId, e);
        }
        return CompletableFuture.completedFuture(null);
    }
}