package ir.shkBostan.PerfLoadSim.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.shkBostan.PerfLoadSim.entity.RequestResultEntity;
import ir.shkBostan.PerfLoadSim.model.CustomLoadTestRequest;
import ir.shkBostan.PerfLoadSim.model.CustomMetricsReport;
import ir.shkBostan.PerfLoadSim.model.DetailedTestReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Core service for executing custom load tests using Java's built-in {@link HttpClient}.
 * <p>
 * This service orchestrates high‑performance, asynchronous HTTP requests against a
 * specified endpoint with precise rate limiting (RPS – requests per second) and
 * configurable duration. It collects real‑time metrics such as response times,
 * success/failure counts, and computes percentiles and averages.
 * </p>
 * <p>
 * The load test is executed entirely in a non‑blocking manner using a thread pool,
 * and a semaphore controls the injection rate. All results are aggregated into a
 * {@link DetailedTestReport} containing both a {@link CustomMetricsReport} summary
 * and a list of per‑request {@link RequestResultEntity} objects.
 * </p>
 * <p>
 * The service also provides a convenience method {@link #sendSingleRequestAndGetBody}
 * for sending a single request (e.g., for validation purposes) using exactly the same
 * client and request building logic.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see CustomLoadTestRequest
 * @see DetailedTestReport
 * @see CustomMetricsReport
 * @see RequestResultEntity
 */
@Service
@Slf4j
public class CustomLoadTestService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Runs a full load test according to the provided request parameters.
     * <p>
     * The test sends a fixed number of requests ({@code targetRps * durationSeconds})
     * asynchronously, respecting the target RPS by using a rate‑limiting semaphore
     * that replenishes permits every second. Each request is executed via a pooled
     * thread, and its result (response time, status, error message) is recorded.
     * After all requests complete, the method computes aggregated statistics and
     * returns a detailed report.
     * </p>
     *
     * @param request the configuration of the test (target URL, headers, body, RPS, duration, etc.)
     * @return a {@link DetailedTestReport} containing both metrics and per‑request details
     * @throws Exception if an unrecoverable error occurs (e.g., I/O issues, thread interruption)
     */
    public DetailedTestReport runTest(CustomLoadTestRequest request) throws Exception {
        log.info("Starting custom load test: targetRps={}, duration={}s, url={}",
                request.getTargetRps(), request.getDurationSeconds(),
                request.getBaseUrl() + request.getEndpoint());

        // Build full URL with query parameters
        final String baseUrl = request.getBaseUrl();
        final String endpoint = request.getEndpoint();
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append(endpoint);
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            String query = request.getQueryParams().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            urlBuilder.append("?").append(query);
        }
        final String fullUrl = urlBuilder.toString();

        // Serialize request body to JSON string (if present)
        String bodyString = null;
        if (request.getBody() != null) {
            bodyString = objectMapper.writeValueAsString(request.getBody());
        }

        // Build the HTTP request
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofMillis(request.getMaxResponseTimeMs()));

        if (request.getHeaders() != null) {
            request.getHeaders().forEach(reqBuilder::header);
        }

        String method = request.getMethod().toUpperCase();
        if ("GET".equals(method)) {
            reqBuilder.GET();
        } else if ("POST".equals(method)) {
            reqBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyString != null ? bodyString : ""))
                    .header("Content-Type", "application/json");
        }
        final HttpRequest httpRequest = reqBuilder.build();

        // Create HTTP client
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        final int targetRps = request.getTargetRps();
        final int durationSec = request.getDurationSeconds();
        final long totalRequestsToSend = (long) targetRps * durationSec;
        log.info("Total requests to send: {}", totalRequestsToSend);

        // Rate limiter
        final Semaphore rateLimiter = new Semaphore(targetRps);
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> rateLimiter.release(targetRps), 1, 1, TimeUnit.SECONDS);

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        final List<CompletableFuture<RequestResultEntity>> futures = new ArrayList<>();
        final AtomicLong successCount = new AtomicLong(0);
        final AtomicLong failCount = new AtomicLong(0);
        final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        // Submit all requests
        for (long i = 0; i < totalRequestsToSend; i++) {
            rateLimiter.acquire(); // block until permit available
            CompletableFuture<RequestResultEntity> future = CompletableFuture.supplyAsync(() -> {
                long start = System.nanoTime();
                try {
                    HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    responseTimes.add(durationMs);
                    boolean isSuccess = response.statusCode() >= 200 && response.statusCode() < 300;
                    if (isSuccess) successCount.incrementAndGet();
                    else failCount.incrementAndGet();

                    RequestResultEntity result = RequestResultEntity.builder()
                            .responseTimeMs(durationMs)
                            .success(isSuccess)
                            .httpStatusCode(response.statusCode())
                            .requestTimestamp(LocalDateTime.now())
                            .build();
                    if (!isSuccess && response.body() != null && !response.body().isEmpty()) {
                        String errorPreview = response.body().substring(0, Math.min(200, response.body().length()));
                        result.setErrorMessage(errorPreview);
                    }
                    return result;
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    return RequestResultEntity.builder()
                            .success(false)
                            .errorMessage(e.getMessage())
                            .requestTimestamp(LocalDateTime.now())
                            .build();
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        scheduler.shutdown();
        executor.shutdown();

        // Collect detailed results
        List<RequestResultEntity> detailedResults = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return RequestResultEntity.builder()
                                .success(false)
                                .errorMessage("Failed to retrieve future result: " + e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());

        // Compute statistics
        long total = successCount.get() + failCount.get();
        double mean = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long max = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long p95 = calculatePercentile(responseTimes, 95);

        CustomMetricsReport metrics = CustomMetricsReport.builder()
                .totalRequests(total)
                .successCount(successCount.get())
                .failedCount(failCount.get())
                .meanResponseTimeMs(mean)
                .maxResponseTimeMs(max)
                .percentile95(p95)
                .build();

        log.info("Load test finished. Metrics: {}", metrics);
        return DetailedTestReport.builder()
                .metrics(metrics)
                .detailedResults(detailedResults)
                .build();
    }

    /**
     * Calculates the given percentile from a list of response times.
     * Uses the nearest‑rank method.
     *
     * @param values     list of response times in milliseconds
     * @param percentile the desired percentile (e.g., 95 for 95th percentile)
     * @return the value at the given percentile, or 0 if the list is empty
     */
    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    /**
     * Sends a single request and returns the raw response body.
     * <p>
     * Uses the exact same HTTP client and request building logic as the load test.
     * This method does not affect the load test metrics and is intended for post‑test
     * validation or smoke testing of the API before running a full load test.
     * </p>
     *
     * @param request the request configuration (URL, headers, body, etc.)
     * @return the response body as a string
     * @throws Exception if the request fails or the response status is not 2xx
     */
    public String sendSingleRequestAndGetBody(CustomLoadTestRequest request) throws Exception {
        log.info("Sending single validation request to: {}{}", request.getBaseUrl(), request.getEndpoint());

        // Build full URL (same logic as in runTest)
        String fullUrl = request.getBaseUrl() + request.getEndpoint();
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            String query = request.getQueryParams().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            fullUrl += "?" + query;
        }
        log.debug("Full URL: {}", fullUrl);

        // Serialize body if present
        String bodyString = null;
        if (request.getBody() != null) {
            bodyString = objectMapper.writeValueAsString(request.getBody());
            log.trace("Serialized body: {}", bodyString);
        }

        // Build HTTP request
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofMillis(request.getMaxResponseTimeMs()));

        if (request.getHeaders() != null) {
            request.getHeaders().forEach(reqBuilder::header);
        }

        String method = request.getMethod().toUpperCase();
        if ("GET".equals(method)) {
            reqBuilder.GET();
        } else if ("POST".equals(method)) {
            reqBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyString != null ? bodyString : ""))
                    .header("Content-Type", "application/json");
        } else {
            throw new UnsupportedOperationException("HTTP method " + method + " not supported for single request");
        }

        HttpRequest httpRequest = reqBuilder.build();

        // Create HTTP client (same as in runTest)
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            log.info("Single request succeeded with status {}", statusCode);
            return responseBody;
        } else {
            log.warn("Single request failed with status {}: {}", statusCode, responseBody);
            throw new RuntimeException("Request failed with status " + statusCode + ": " + responseBody);
        }
    }
}