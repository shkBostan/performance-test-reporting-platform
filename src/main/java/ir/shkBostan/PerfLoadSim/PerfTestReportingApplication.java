package ir.shkBostan.PerfLoadSim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Performance Load Simulation application.
 * <p>
 * This Spring Boot application provides REST APIs to start, monitor,
 * and retrieve reports for Gatling-based load tests.
 * </p>
 *
 * @author s Bostan
 * @since Apr, 2026
 */
@SpringBootApplication
@EnableAsync
public class PerfTestReportingApplication {

	/**
	 * Launches the Spring Boot application.
	 *
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
		SpringApplication.run(PerfTestReportingApplication.class, args);
	}

}
