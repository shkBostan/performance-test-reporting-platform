package ir.shkBostan.PerfLoadSim.repository;

import ir.shkBostan.PerfLoadSim.entity.TestRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for accessing and managing load test run summaries.
 * <p>
 * Extends {@link JpaRepository} to inherit standard CRUD, pagination, and sorting
 * capabilities. Custom query methods are defined to retrieve test runs by scenario
 * name in descending chronological order, as well as a paginated view of all runs.
 * </p>
 * <p>
 * The implementation is generated automatically by Spring Data JPA at runtime.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see TestRunEntity
 * @see RequestResultRepository
 */
public interface TestRunRepository extends JpaRepository<TestRunEntity, UUID> {

    /**
     * Retrieves all test runs for a given scenario, ordered by start time descending
     * (most recent first).
     * <p>
     * This method is useful for quickly showing the latest executions of a particular
     * scenario without pagination.
     * </p>
     *
     * @param scenarioName the name of the scenario (e.g., "search", "bulk")
     * @return a list of {@link TestRunEntity} objects for the specified scenario,
     *         sorted by {@code startTime} descending
     */
    List<TestRunEntity> findByScenarioNameOrderByStartTimeDesc(String scenarioName);

    /**
     * Retrieves a paginated and sorted list of all test run summaries.
     * <p>
     * This method overrides the default {@code findAll(Pageable)} to enable
     * custom sorting and pagination for the report endpoints. It is typically
     * used with page and size parameters.
     * </p>
     *
     * @param pageable pagination and sorting information (page number, page size, sort field)
     * @return a page of {@link TestRunEntity} objects according to the requested pagination
     */
    Page<TestRunEntity> findAll(Pageable pageable);
}