package ir.shkBostan.PerfLoadSim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.shkBostan.PerfLoadSim.model.*;
import ir.shkBostan.PerfLoadSim.service.CustomLoadTestService;
import ir.shkBostan.PerfLoadSim.service.ReportPersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for executing performance tests based on predefined scenarios.
 * <p>
 * This controller provides endpoints to start asynchronous load tests, check their
 * status, retrieve real‑time reports, and send single validation requests.
 * All endpoints are grouped under {@code /api/performance/tests}.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 */
@Slf4j
@RestController
@RequestMapping("/api/performance/tests")
@RequiredArgsConstructor
@Tag(name = "Performance Test Execution", description = "Run, monitor and validate performance tests using predefined scenarios")
public class ScenarioPerformanceTestController {

    private final CustomLoadTestService loadTestService;
    private final ReportPersistenceService reportPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("classpath:scenarios.json")
    private Resource scenariosResource;

    private AllScenarios allScenarios;

    private final Map<UUID, CompletableFuture<DetailedTestReport>> runningTests = new ConcurrentHashMap<>();
    private final Map<UUID, CustomMetricsReport> completedReports = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadScenarios() throws Exception {
        log.info("Loading scenarios from scenarios.json");
        try (InputStream is = scenariosResource.getInputStream()) {
            allScenarios = objectMapper.readValue(is, AllScenarios.class);
            log.info("Loaded {} scenario(s)", allScenarios.getScenarios().size());
        }
    }

    /**
     * Starts an asynchronous performance test based on a named scenario.
     *
     * @param scenarioName the scenario key (e.g., "search", "bulk")
     * @param override     optional overrides for RPS, duration, or max response time
     * @return a Mono emitting a map containing the generated testId
     */
    @PostMapping("/scenarios/{scenarioName}/run")
    @Operation(summary = "Start a performance test", description = "Executes a load test according to the named scenario. Returns a testId immediately.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Test started successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"testId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid scenario name or parameters")
    })
    public Mono<Map<String, UUID>> startPerformanceTest(
            @Parameter(description = "Scenario name (e.g., search, bulk)", required = true, example = "search")
            @PathVariable String scenarioName,
            @Parameter(description = "Optional overrides for targetRps, durationSeconds, maxResponseTimeMs")
            @RequestBody(required = false) CustomLoadTestRequest override) {

        ScenarioConfig config = allScenarios.getScenarios().get(scenarioName);
        if (config == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioName);
        }

        CustomLoadTestRequest request = new CustomLoadTestRequest();
        request.setBaseUrl(config.getBaseUrl());
        request.setEndpoint(config.getEndpoint());
        request.setMethod(config.getMethod());
        request.setHeaders(config.getHeaders());
        request.setBody(config.getBody());
        request.setQueryParams(config.getQueryParams());

        request.setTargetRps(override != null && override.getTargetRps() > 0
                ? override.getTargetRps() : config.getDefaultTargetRps());
        request.setDurationSeconds(override != null && override.getDurationSeconds() > 0
                ? override.getDurationSeconds() : config.getDefaultDurationSeconds());
        request.setMaxResponseTimeMs(override != null && override.getMaxResponseTimeMs() > 0
                ? override.getMaxResponseTimeMs() : config.getDefaultMaxResponseTimeMs());

        UUID testId = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.now();

        CompletableFuture<DetailedTestReport> future = CompletableFuture.supplyAsync(() -> {
            try {
                return loadTestService.runTest(request);
            } catch (Exception e) {
                log.error("Test failed for id {}", testId, e);
                return DetailedTestReport.builder()
                        .metrics(CustomMetricsReport.builder().build())
                        .detailedResults(List.of())
                        .build();
            }
        });

        runningTests.put(testId, future);
        future.whenComplete((report, ex) -> {
            LocalDateTime endTime = LocalDateTime.now();
            if (report != null && report.getMetrics() != null) {
                reportPersistenceService.saveTestReportAsync(testId, scenarioName, request, report, startTime, endTime);
                completedReports.put(testId, report.getMetrics());
            } else {
                log.warn("Test {} produced no report", testId);
            }
            runningTests.remove(testId);
        });

        return Mono.just(Map.of("testId", testId));
    }

    /**
     * Sends a single request for a scenario (smoke test) and returns the raw response body.
     *
     * @param scenarioName the scenario key
     * @param override     optional overrides (maxResponseTimeMs is respected)
     * @return a Mono emitting a map with success flag and response body or error
     */
    @PostMapping("/scenarios/{scenarioName}/single")
    @Operation(summary = "Send a single validation request", description = "Useful for smoke‑testing the scenario configuration before a full run.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid scenario name"),
            @ApiResponse(responseCode = "500", description = "Request failed")
    })
    public Mono<Map<String, Object>> singleScenarioRequest(
            @Parameter(description = "Scenario name", required = true, example = "search")
            @PathVariable String scenarioName,
            @Parameter(description = "Optional overrides (only maxResponseTimeMs is used)")
            @RequestBody(required = false) CustomLoadTestRequest override) {

        ScenarioConfig config = allScenarios.getScenarios().get(scenarioName);
        if (config == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioName);
        }

        CustomLoadTestRequest request = new CustomLoadTestRequest();
        request.setBaseUrl(config.getBaseUrl());
        request.setEndpoint(config.getEndpoint());
        request.setMethod(config.getMethod());
        request.setHeaders(config.getHeaders());
        request.setBody(config.getBody());
        request.setQueryParams(config.getQueryParams());

        int maxTime = (override != null && override.getMaxResponseTimeMs() > 0)
                ? override.getMaxResponseTimeMs() : config.getDefaultMaxResponseTimeMs();
        request.setMaxResponseTimeMs(maxTime);

        log.info("Single request for scenario '{}' to: {}{}", scenarioName, request.getBaseUrl(), request.getEndpoint());
        try {
            String responseBody = loadTestService.sendSingleRequestAndGetBody(request);
            return Mono.just(Map.of(
                    "success", true,
                    "statusCode", 200,
                    "responseBody", responseBody
            ));
        } catch (Exception e) {
            log.error("Single request failed for '{}'", scenarioName, e);
            return Mono.just(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Returns the current status of a running or completed test.
     *
     * @param testId the test identifier
     * @return a Mono emitting a map with status: RUNNING, COMPLETED, or NOT_FOUND
     */
    @GetMapping("/{testId}/status")
    @Operation(summary = "Get test status")
    public Mono<Map<String, String>> getTestStatus(
            @Parameter(description = "UUID of the test", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID testId) {
        if (runningTests.containsKey(testId) && !runningTests.get(testId).isDone()) {
            return Mono.just(Map.of("status", "RUNNING"));
        }
        if (completedReports.containsKey(testId)) {
            return Mono.just(Map.of("status", "COMPLETED"));
        }
        return Mono.just(Map.of("status", "NOT_FOUND"));
    }

    /**
     * Retrieves the aggregated performance report for a test (if completed).
     *
     * @param testId the test identifier
     * @return a Mono emitting the metrics report, or an empty report if not found
     */
    @GetMapping("/{testId}/report")
    @Operation(summary = "Get test report", description = "Returns the aggregated metrics once the test has finished.")
    public Mono<CustomMetricsReport> getTestReport(
            @Parameter(description = "UUID of the test", required = true)
            @PathVariable UUID testId) {
        CompletableFuture<DetailedTestReport> running = runningTests.get(testId);
        if (running != null && running.isDone()) {
            try {
                return Mono.just(running.get().getMetrics());
            } catch (Exception e) {
                log.warn("Failed to get report from running test", e);
                return Mono.just(CustomMetricsReport.builder().build());
            }
        }
        CustomMetricsReport completed = completedReports.get(testId);
        if (completed != null) {
            return Mono.just(completed);
        }
        return Mono.just(CustomMetricsReport.builder().build());
    }
}