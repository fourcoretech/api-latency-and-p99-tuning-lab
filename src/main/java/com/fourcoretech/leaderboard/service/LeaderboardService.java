package com.fourcoretech.leaderboard.service;

import com.fourcoretech.leaderboard.dto.LeaderboardEntryDTO;
import com.fourcoretech.leaderboard.entity.PlayerProfile;
import com.fourcoretech.leaderboard.entity.PlayerScore;
import com.fourcoretech.leaderboard.repository.PlayerProfileRepository;
import com.fourcoretech.leaderboard.repository.PlayerScoreRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LeaderboardService - Contains INTENTIONAL performance problems
 *
 * This service has MULTIPLE performance anti-patterns that you'll identify and fix:
 *
 * PROBLEM 1: N+1 QUERY PATTERN
 * - Fetches scores in one query
 * - Then fetches EACH player profile in separate queries
 * - 100 scores = 101 database queries!
 *
 * PROBLEM 2: BLOCKING I/O WITH RANDOM DELAYS
 * - Simulates slow external API calls or services
 * - Uses Thread.sleep() to block threads
 * - Causes thread pool exhaustion under load
 *
 * PROBLEM 3: NO CACHING
 * - Every request hits the database
 * - Popular leaderboards could be cached for seconds/minutes
 * - Wastes database resources
 *
 * PROBLEM 4: SYNCHRONOUS PROCESSING
 * - Everything happens sequentially
 * - Could parallelize profile fetches or use async
 * - Low throughput, high latency
 *
 * PROBLEM 5: POOR OBSERVABILITY
 * - Limited metrics and logging
 * - Hard to diagnose where time is spent
 * - No P95/P99 tracking on operations
 *
 * YOUR MISSION:
 * Fix these problems one by one and measure the improvement in P99 latency!
 */
@Service
@Slf4j
public class LeaderboardService {

    private final PlayerScoreRepository scoreRepository;
    private final PlayerProfileRepository profileRepository;
    private final MeterRegistry meterRegistry;
    private final Random random = new Random();

    public LeaderboardService(
            PlayerScoreRepository scoreRepository,
            PlayerProfileRepository profileRepository,
            MeterRegistry meterRegistry) {
        this.scoreRepository = scoreRepository;
        this.profileRepository = profileRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get top players globally - FULL OF PERFORMANCE PROBLEMS!
     *
     * @param limit Number of top players to return
     * @return List of leaderboard entries
     */
    public List<LeaderboardEntryDTO> getTopPlayers(int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("Fetching top {} players (SLOW VERSION with problems)", limit);

            // PROBLEM 1: Fetch scores WITHOUT player information (sets up N+1)
            // This query only gets PlayerScore records, not the related PlayerProfile data
            List<PlayerScore> topScores = scoreRepository.findTopScores(PageRequest.of(0, limit));
            log.debug("Fetched {} scores from database", topScores.size());

            // PROBLEM 2: Random latency spike simulation (simulates slow external service)
            // In real apps, this could be: slow API call, DNS lookup, network issue, etc.
            simulateRandomLatencySpike("top_players");

            // PROBLEM 3 & 4: N+1 Query Problem - fetch each profile SEPARATELY in a loop!
            // This is one of the WORST performance anti-patterns in database programming
            List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
            int rank = 1;

            for (PlayerScore score : topScores) {
                // THIS IS THE N+1 PROBLEM: One query PER player!
                // With 100 players, this makes 100 separate database queries!
                Timer.Sample profileSample = Timer.start(meterRegistry);

                PlayerProfile profile = profileRepository.findById(score.getPlayerId())
                        .orElse(null);

                profileSample.stop(Timer.builder("leaderboard.profile.fetch")
                        .description("Time to fetch individual player profile")
                        .register(meterRegistry));

                if (profile != null) {
                    // PROBLEM 5: More blocking delays (simulates processing time)
                    simulateProcessingDelay();

                    LeaderboardEntryDTO entry = buildLeaderboardEntry(rank++, score, profile);
                    leaderboard.add(entry);
                }
            }

            log.info("Built leaderboard with {} entries (made {} DB queries!)",
                     leaderboard.size(), topScores.size() + 1);

            return leaderboard;

        } finally {
            // Record the total time - watch this P99 skyrocket!
            sample.stop(Timer.builder("leaderboard.get_top_players")
                    .description("Total time to fetch top players")
                    .tag("limit", String.valueOf(limit))
                    .register(meterRegistry));
        }
    }

    /**
     * Get top players by region - SAME PROBLEMS + missing index on region!
     *
     * ADDITIONAL PROBLEM: Database has no index on (region, score)
     * - Must scan entire table to filter by region
     * - Then sort the results
     * - Very slow on large datasets
     */
    public List<LeaderboardEntryDTO> getTopPlayersByRegion(String region, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("Fetching top {} players for region {} (SLOW VERSION)", limit, region);

            // PROBLEM: This query will do a FULL TABLE SCAN (no index on region!)
            List<PlayerScore> topScores = scoreRepository.findTopScoresByRegion(
                    region, PageRequest.of(0, limit));

            simulateRandomLatencySpike("top_players_by_region");

            // Same N+1 problem as above
            List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
            int rank = 1;

            for (PlayerScore score : topScores) {
                PlayerProfile profile = profileRepository.findById(score.getPlayerId())
                        .orElse(null);

                if (profile != null) {
                    simulateProcessingDelay();
                    LeaderboardEntryDTO entry = buildLeaderboardEntry(rank++, score, profile);
                    leaderboard.add(entry);
                }
            }

            log.info("Built regional leaderboard with {} entries", leaderboard.size());
            return leaderboard;

        } finally {
            sample.stop(Timer.builder("leaderboard.get_top_players_by_region")
                    .description("Total time to fetch top players by region")
                    .tag("region", region)
                    .tag("limit", String.valueOf(limit))
                    .register(meterRegistry));
        }
    }

    /**
     * Simulate random latency spikes - represents unpredictable slowness
     *
     * REAL-WORLD EXAMPLES:
     * - Garbage collection pause
     * - Network hiccup
     * - Database lock wait
     * - Slow DNS resolution
     * - Cache miss on cold data
     * - CPU throttling
     *
     * This is what causes HIGH P99 latency even when P50 looks good!
     */
    private void simulateRandomLatencySpike(String operation) {
        // 20% chance of a latency spike
        if (random.nextInt(100) < 20) {
            int delayMs = 100 + random.nextInt(400); // 100-500ms spike
            log.warn("LATENCY SPIKE in {}: {}ms delay!", operation, delayMs);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            meterRegistry.counter("leaderboard.latency_spike",
                    "operation", operation,
                    "delay_ms", String.valueOf(delayMs)).increment();
        }
    }

    /**
     * Simulate small processing delays - represents computation or I/O
     *
     * Even small delays (5-10ms) add up when done in a loop!
     * 100 players × 5ms = 500ms minimum latency
     */
    private void simulateProcessingDelay() {
        try {
            // Small delay per record (represents JSON parsing, validation, etc.)
            Thread.sleep(2 + random.nextInt(5)); // 2-7ms per record
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Build a LeaderboardEntryDTO from score and profile
     */
    private LeaderboardEntryDTO buildLeaderboardEntry(int rank, PlayerScore score, PlayerProfile profile) {
        LeaderboardEntryDTO entry = new LeaderboardEntryDTO();
        entry.setRank(rank);
        entry.setPlayerId(score.getPlayerId());
        entry.setScore(score.getScore());
        entry.setRegion(score.getRegion());
        entry.setGameMode(score.getGameMode());

        // Player profile information
        entry.setUsername(profile.getUsername());
        entry.setDisplayName(profile.getDisplayName());
        entry.setAvatarUrl(profile.getAvatarUrl());
        entry.setCountry(profile.getCountry());
        entry.setLevel(profile.getLevel());
        entry.setIsPremium(profile.getIsPremium());

        return entry;
    }

    /**
     * Get total number of scores
     */
    public long getTotalScores() {
        return scoreRepository.count();
    }

    /**
     * Get total scores for a region
     */
    public long getTotalScoresByRegion(String region) {
        return scoreRepository.countByRegion(region);
    }
}
