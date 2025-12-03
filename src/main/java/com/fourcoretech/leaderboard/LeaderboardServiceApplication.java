package com.fourcoretech.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LeaderboardService - API Latency and P99 Tuning Lab
 *
 * This application demonstrates common performance issues that cause high API latency
 * and poor P99 response times. It's designed as a learning lab where you'll identify
 * and fix these issues step by step.
 *
 * INTENTIONAL PROBLEMS IN THIS LAB:
 * 1. N+1 Query Problem - Multiple database queries where one would suffice
 * 2. Missing Database Indexes - Slow table scans on large datasets
 * 3. Blocking I/O Operations - Synchronous calls that block threads
 * 4. No Caching Strategy - Repeated database queries for the same data
 * 5. Poor Observability - Limited metrics to diagnose performance issues
 * 6. No Timeout/Retry Logic - Operations can hang indefinitely
 *
 * YOUR MISSION:
 * Transform this slow, unreliable API into a fast, observable, production-ready service.
 *
 * LEARNING GOALS:
 * - Understand what P99 latency means and why it matters
 * - Identify performance bottlenecks using metrics and profiling
 * - Apply database optimization techniques (indexes, query optimization)
 * - Implement caching strategies with Redis
 * - Use async programming to improve throughput
 * - Add comprehensive observability with Micrometer and Prometheus
 * - Implement resilience patterns (timeouts, circuit breakers)
 *
 * @author FourCoreTech
 */
@SpringBootApplication
@EnableAsync  // Enable async processing support (initially misconfigured)
public class LeaderboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderboardServiceApplication.class, args);
        System.out.println("\n" +
                "====================================================================\n" +
                " LEADERBOARD SERVICE - API LATENCY & P99 TUNING LAB\n" +
                "====================================================================\n" +
                " Status: RUNNING (with intentional performance issues)\n" +
                " API Endpoint: http://localhost:8080/leaderboard/top\n" +
                " Metrics: http://localhost:8080/actuator/prometheus\n" +
                " Health: http://localhost:8080/actuator/health\n" +
                "====================================================================\n" +
                " WARNING: This service has INTENTIONAL performance problems!\n" +
                " Your job is to identify and fix them using the lab checklist.\n" +
                "====================================================================\n");
    }
}
