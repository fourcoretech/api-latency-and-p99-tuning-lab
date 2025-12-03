package com.fourcoretech.leaderboard.controller;

import com.fourcoretech.leaderboard.dto.LeaderboardEntryDTO;
import com.fourcoretech.leaderboard.service.LeaderboardService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Leaderboard endpoints
 *
 * PROBLEM: This controller has NO timeout or error handling
 * - Requests can hang indefinitely if service is slow
 * - No circuit breaker to protect from cascading failures
 * - No rate limiting to prevent abuse
 * - Limited error details for debugging
 *
 * WHAT YOU'LL ADD:
 * - Request timeouts using @Timeout annotation
 * - Circuit breaker with Resilience4j
 * - Better error handling and responses
 * - Rate limiting (optional stretch goal)
 */
@RestController
@RequestMapping("/leaderboard")
@Slf4j
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final MeterRegistry meterRegistry;

    public LeaderboardController(LeaderboardService leaderboardService, MeterRegistry meterRegistry) {
        this.leaderboardService = leaderboardService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get global top players
     *
     * Example: GET /leaderboard/top?limit=100
     *
     * PROBLEM: No timeout! If the service is slow, this request just hangs.
     * Under high load, all web threads could be blocked waiting for slow DB queries.
     *
     * @param limit Number of top players to return (default: 100, max: 1000)
     * @return List of top leaderboard entries
     */
    @GetMapping("/top")
    public ResponseEntity<Map<String, Object>> getTopPlayers(
            @RequestParam(defaultValue = "100") int limit) {

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("GET /leaderboard/top?limit={}", limit);

            // Validate limit
            if (limit < 1 || limit > 1000) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Limit must be between 1 and 1000"));
            }

            // Call service - THIS CAN BE VERY SLOW!
            List<LeaderboardEntryDTO> leaderboard = leaderboardService.getTopPlayers(limit);
            long totalScores = leaderboardService.getTotalScores();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("data", leaderboard);
            response.put("metadata", Map.of(
                    "count", leaderboard.size(),
                    "totalScores", totalScores,
                    "limit", limit
            ));

            meterRegistry.counter("leaderboard.api.success",
                    "endpoint", "top").increment();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching top players", e);

            meterRegistry.counter("leaderboard.api.error",
                    "endpoint", "top",
                    "error", e.getClass().getSimpleName()).increment();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch leaderboard: " + e.getMessage()));

        } finally {
            sample.stop(Timer.builder("leaderboard.api.request")
                    .description("API request duration")
                    .tag("endpoint", "top")
                    .register(meterRegistry));
        }
    }

    /**
     * Get top players by region
     *
     * Example: GET /leaderboard/top/NA?limit=50
     *
     * PROBLEM: Same as above - no timeout, no circuit breaker
     * ADDITIONAL PROBLEM: Missing database index on region makes this EXTRA slow
     *
     * @param region Region code (NA, EU, ASIA, SA, OCE)
     * @param limit Number of top players to return
     * @return List of top leaderboard entries for the region
     */
    @GetMapping("/top/{region}")
    public ResponseEntity<Map<String, Object>> getTopPlayersByRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "100") int limit) {

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("GET /leaderboard/top/{}?limit={}", region, limit);

            // Validate inputs
            if (limit < 1 || limit > 1000) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Limit must be between 1 and 1000"));
            }

            // Simple region validation
            if (!isValidRegion(region)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid region. Valid regions: NA, EU, ASIA, SA, OCE"));
            }

            // Call service - VERY SLOW without index on region!
            List<LeaderboardEntryDTO> leaderboard = leaderboardService.getTopPlayersByRegion(region, limit);
            long totalScores = leaderboardService.getTotalScoresByRegion(region);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("data", leaderboard);
            response.put("metadata", Map.of(
                    "region", region,
                    "count", leaderboard.size(),
                    "totalScores", totalScores,
                    "limit", limit
            ));

            meterRegistry.counter("leaderboard.api.success",
                    "endpoint", "top_by_region",
                    "region", region).increment();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching top players for region {}", region, e);

            meterRegistry.counter("leaderboard.api.error",
                    "endpoint", "top_by_region",
                    "region", region,
                    "error", e.getClass().getSimpleName()).increment();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch leaderboard: " + e.getMessage()));

        } finally {
            sample.stop(Timer.builder("leaderboard.api.request")
                    .description("API request duration")
                    .tag("endpoint", "top_by_region")
                    .tag("region", region)
                    .register(meterRegistry));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "leaderboard",
                "message", "Service is running (with intentional performance issues!)"
        ));
    }

    /**
     * Simple region validation
     */
    private boolean isValidRegion(String region) {
        return region != null && (
                region.equals("NA") ||
                region.equals("EU") ||
                region.equals("ASIA") ||
                region.equals("SA") ||
                region.equals("OCE")
        );
    }
}
