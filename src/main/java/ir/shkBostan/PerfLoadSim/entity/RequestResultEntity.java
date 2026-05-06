package ir.shkBostan.PerfLoadSim.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ir.shkBostan.PerfLoadSim.repository.RequestResultRepository;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents the detailed result of a single HTTP request performed during a load test.
 * <p>
 * Each instance of this entity corresponds to one request that was sent as part of a
 * load test run. It contains the response time, HTTP status code, success/failure
 * flag, an optional error message, and the timestamp when the request was executed.
 * </p>
 * <p>
 * The entity is linked to a parent {@link TestRunEntity} via a many‑to‑one relationship.
 * To avoid lazy loading issues during JSON serialisation, the {@code testRun} field
 * is annotated with {@code @JsonIgnore}. Clients that need test run metadata should
 * query the parent entity separately.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see TestRunEntity
 * @see RequestResultRepository
 */
@Entity
@Table(name = "request_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResultEntity {

    /**
     * Primary key – auto‑generated identity column.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent load test run that this request belongs to.
     * <p>
     * The relationship is lazily loaded to avoid fetching the entire test run
     * every time a result is accessed. The field is ignored during JSON
     * serialisation to prevent Hibernate lazy initialisation exceptions.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    @JsonIgnore
    private TestRunEntity testRun;

    /**
     * Response time of the request in milliseconds.
     */
    private long responseTimeMs;

    /**
     * Indicates whether the request succeeded ({@code true} for HTTP status 2xx,
     * {@code false} for any non‑2xx status or network error).
     */
    private boolean success;

    /**
     * The actual HTTP status code returned by the server (e.g., 200, 404, 500).
     * May be {@code null} if the request failed before receiving a response
     * (e.g., connection refused).
     */
    private Integer httpStatusCode;

    /**
     * Detailed error message in case of failure.
     * <p>
     * For HTTP errors, contains a preview of the response body (first 200 characters).
     * For network/connection errors, contains the exception message.
     * </p>
     */
    private String errorMessage;

    /**
     * Timestamp (with nanosecond precision) when the request was executed.
     * Stored in the system time zone (typically UTC).
     */
    private LocalDateTime requestTimestamp;
}