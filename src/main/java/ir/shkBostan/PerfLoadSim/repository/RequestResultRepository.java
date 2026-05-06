package ir.shkBostan.PerfLoadSim.repository;

import ir.shkBostan.PerfLoadSim.entity.RequestResultEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository interface for {@link RequestResultEntity} providing database operations
 * for per‑request load test results.
 * <p>
 * Extends {@link JpaRepository} to gain standard CRUD and pagination capabilities.
 * Custom query methods are defined to retrieve detailed results filtered by test run
 * and success status, as well as counting methods for performance analysis.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see RequestResultEntity
 * @see TestRunRepository
 */
public interface RequestResultRepository extends JpaRepository<RequestResultEntity, Long> {

    /**
     * Retrieves a page of detailed results for a specific test run.
     *
     * @param testRunId the unique identifier of the parent test run
     * @param pageable  pagination and sorting information
     * @return a page of {@link RequestResultEntity} belonging to the given test run
     */
    Page<RequestResultEntity> findByTestRunId(UUID testRunId, Pageable pageable);

    /**
     * Retrieves a page of detailed results for a specific test run, filtered by success status.
     *
     * @param testRunId the unique identifier of the parent test run
     * @param success   {@code true} for successful requests, {@code false} for failed ones
     * @param pageable  pagination and sorting information
     * @return a page of matching {@link RequestResultEntity}
     */
    Page<RequestResultEntity> findByTestRunIdAndSuccess(UUID testRunId, boolean success, Pageable pageable);

    /**
     * Counts the total number of requests (both successful and failed) for a test run.
     *
     * @param testRunId the unique identifier of the parent test run
     * @return total request count
     */
    long countByTestRunId(UUID testRunId);

    /**
     * Counts the number of requests with a specific success status for a test run.
     *
     * @param testRunId the unique identifier of the parent test run
     * @param success   {@code true} for successful requests, {@code false} for failed ones
     * @return number of matching requests
     */
    long countByTestRunIdAndSuccess(UUID testRunId, boolean success);
}