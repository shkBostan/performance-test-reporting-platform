package ir.shkBostan.PerfLoadSim.config;

import ir.shkBostan.PerfLoadSim.service.ReportPersistenceService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for asynchronous task execution in the application.
 * <p>
 * This class implements {@link AsyncConfigurer} to provide a custom
 * {@link Executor} for handling asynchronous method calls (e.g., the
 * non‑blocking saving of load test reports). The executor is configured
 * with a bounded thread pool, a queue for pending tasks, and a meaningful
 * thread name prefix for easier monitoring and debugging.
 * </p>
 * <p>
 * Using a dedicated thread pool prevents the accumulation of report‑saving
 * tasks from interfering with the main test execution threads, thus
 * preserving the responsiveness of the load test APIs.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 * @see org.springframework.scheduling.annotation.Async
 * @see ReportPersistenceService#saveTestReportAsync
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Creates and configures the task executor that will be used for all
     * {@code @Async} methods in the Spring context.
     * <p>
     * The executor is a {@link ThreadPoolTaskExecutor} with the following
     * properties:
     * <ul>
     *   <li><b>core pool size:</b> 2 (always alive threads)</li>
     *   <li><b>maximum pool size:</b> 5 (upper bound when the queue is full)</li>
     *   <li><b>queue capacity:</b> 100 (number of waiting tasks before creating
     *       new threads up to {@code maxPoolSize})</li>
     *   <li><b>thread name prefix:</b> {@code "report-saver-"} (helps identify
     *       threads in logs and debuggers)</li>
     * </ul>
     * </p>
     *
     * @return a fully initialised {@link Executor} instance ready to submit
     *         asynchronous tasks
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-saver-");
        executor.initialize();
        return executor;
    }
}