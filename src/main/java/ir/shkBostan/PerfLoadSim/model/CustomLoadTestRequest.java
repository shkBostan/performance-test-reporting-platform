package ir.shkBostan.PerfLoadSim.model;

import lombok.Data;
import java.util.Map;

/**
 * Represents a request to execute a custom load test.
 * <p>
 * Contains all parameters required to run a performance test against a specific API endpoint,
 * including target URL, HTTP method, headers, request body, query parameters,
 * and load configuration (RPS, duration, max response time).
 * </p>
 * <p>
 * The {@code body} field is of type {@link Object} to accept both raw JSON strings
 * and structured objects (e.g., {@link Map}), offering flexibility for different
 * input sources such as configuration files or direct API calls.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 */
@Data
public class CustomLoadTestRequest {

    /**
     * Base URL of the target API (e.g., {@code http://localhost:8080}).
     */
    private String baseUrl;

    /**
     * Endpoint path relative to the base URL (e.g., {@code /api/search}).
     */
    private String endpoint;

    /**
     * HTTP method to be used (e.g., GET, POST, PUT, DELETE). Defaults to "GET".
     */
    private String method = "GET";

    /**
     * Custom HTTP headers to include in each request,
     * such as authentication tokens or content type definitions.
     */
    private Map<String, String> headers;

    /**
     * Request body payload. Can be a JSON string, a {@link Map}, or any serializable object.
     * The service will convert it to the appropriate format before sending.
     */
    private Object body;

    /**
     * URL query parameters as key-value pairs.
     */
    private Map<String, String> queryParams;

    /**
     * Target number of requests per second (RPS) to sustain during the test.
     * Default value is 10.
     */
    private int targetRps = 10;

    /**
     * Duration of the test in seconds. Default value is 30.
     */
    private int durationSeconds = 30;

    /**
     * Maximum allowed response time in milliseconds. Exceeding this threshold
     * may cause the test to be considered failed (for assertion purposes).
     * Default value is 5000 ms (5 seconds).
     */
    private int maxResponseTimeMs = 5000;
}