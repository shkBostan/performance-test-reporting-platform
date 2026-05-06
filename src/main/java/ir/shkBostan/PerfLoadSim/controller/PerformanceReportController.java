package ir.shkBostan.PerfLoadSim.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.shkBostan.PerfLoadSim.entity.RequestResultEntity;
import ir.shkBostan.PerfLoadSim.entity.TestRunEntity;
import ir.shkBostan.PerfLoadSim.repository.RequestResultRepository;
import ir.shkBostan.PerfLoadSim.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for querying historical performance test reports.
 * <p>
 * All endpoints are read‑only and return data already persisted in the database.
 * They support pagination, filtering by success status, and success‑rate calculation.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 */
@RestController
@RequestMapping("/api/performance/reports")
@RequiredArgsConstructor
@Tag(name = "Performance Reports", description = "Retrieve historical performance test runs and detailed results")
public class PerformanceReportController {

    private final TestRunRepository testRunRepository;
    private final RequestResultRepository requestResultRepository;

    /**
     * Retrieves a paginated list of all performance test runs.
     *
     * @param page    page number (0‑based), defaults to 0
     * @param size    number of items per page, defaults to 20
     * @param sortBy  field to sort by (default: startTime)
     * @param sortDir sorting direction (asc or desc), defaults to asc
     * @return a Mono emitting a Page of TestRunEntity
     */
    @GetMapping("/runs")
    @Operation(summary = "List all test runs", description = "Returns a paginated list of performance test executions.")
    public Mono<Page<TestRunEntity>> getAllTestRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy != null ? sortBy : "startTime");
        Pageable pageable = PageRequest.of(page, size, sort);
        return Mono.just(testRunRepository.findAll(pageable));
    }

    /**
     * Returns a summary of a single test run.
     *
     * @param testId the UUID of the test run
     * @return a Mono emitting the TestRunEntity, or 404 if not found
     */
    @GetMapping("/runs/{testId}")
    @Operation(summary = "Get test run summary")
    public Mono<TestRunEntity> getTestRunSummary(
            @Parameter(description = "UUID of the test run", required = true)
            @PathVariable UUID testId) {
        return Mono.justOrEmpty(testRunRepository.findById(testId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run not found")));
    }

    /**
     * Retrieves detailed per‑request results for a test run, optionally filtered by success.
     *
     * @param testId  the UUID of the test run
     * @param success optional filter (true = only successful, false = only failed)
     * @param page    page number, defaults to 0
     * @param size    page size, defaults to 50
     * @return a Mono emitting a Page of RequestResultEntity
     */
    @GetMapping("/runs/{testId}/results")
    @Operation(summary = "Get detailed request results", description = "Paginated list of individual requests with response times and statuses.")
    public Mono<Page<RequestResultEntity>> getDetailedResults(
            @Parameter(description = "UUID of the test run", required = true)
            @PathVariable UUID testId,
            @Parameter(description = "Filter by success: true (successful), false (failed), or omit for all")
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<RequestResultEntity> resultPage;
        if (success != null) {
            resultPage = requestResultRepository.findByTestRunIdAndSuccess(testId, success, pageable);
        } else {
            resultPage = requestResultRepository.findByTestRunId(testId, pageable);
        }
        return Mono.just(resultPage);
    }

    /**
     * Calculates the success rate for a given test run.
     *
     * @param testId the UUID of the test run
     * @return a Mono emitting a map containing totalRequests, successCount, failCount, successRate
     */
    @GetMapping("/runs/{testId}/success-rate")
    @Operation(summary = "Get success rate", description = "Returns total requests, successful, failed, and percentage.")
    public Mono<Map<String, Object>> getSuccessRate(
            @Parameter(description = "UUID of the test run", required = true)
            @PathVariable UUID testId) {
        long total = requestResultRepository.countByTestRunId(testId);
        long success = requestResultRepository.countByTestRunIdAndSuccess(testId, true);
        return Mono.just(Map.of(
                "testId", testId,
                "totalRequests", total,
                "successCount", success,
                "failCount", total - success,
                "successRate", total == 0 ? 0 : (success * 100.0 / total)
        ));
    }
}