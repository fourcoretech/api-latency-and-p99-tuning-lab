package com.fourcoretech.leaderboard.repository;

import com.fourcoretech.leaderboard.entity.PlayerScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PlayerScore entity
 *
 * PROBLEM: These queries do NOT use JOIN FETCH
 * - They return PlayerScore objects with only playerId
 * - The service then fetches each PlayerProfile SEPARATELY (N+1 problem!)
 *
 * WHAT YOU'LL SEE:
 * - One query to get top 100 scores
 * - 100 individual queries to get each player profile
 * - Total: 101 database round-trips
 *
 * OPTIMIZATION (you'll add):
 * - Add JOIN FETCH queries to load player profiles in one query
 * - Use DTO projections to select only needed columns
 * - Add query hints for performance tuning
 */
@Repository
public interface PlayerScoreRepository extends JpaRepository<PlayerScore, Long> {

    /**
     * Find top scores globally - CAUSES N+1 QUERY PROBLEM
     *
     * This query ONLY fetches PlayerScore records. When we need player
     * information (username, avatar, etc.), we'll make SEPARATE queries
     * for each player - that's the N+1 problem!
     *
     * Example with top 100:
     * - 1 query to get 100 scores
     * - 100 queries to get player profiles
     * - Total: 101 queries!
     *
     * @param pageable Page request with size and sort
     * @return List of top player scores
     */
    @Query("SELECT ps FROM PlayerScore ps ORDER BY ps.score DESC")
    List<PlayerScore> findTopScores(Pageable pageable);

    /**
     * Find top scores by region - ALSO CAUSES N+1 QUERY PROBLEM
     *
     * ADDITIONAL PROBLEM: Missing index on (region, score)
     * - Database must scan entire table to filter by region
     * - Then sort the results (expensive without index)
     * - Combined with N+1 queries = VERY SLOW
     *
     * @param region Region code (e.g., "NA", "EU", "ASIA")
     * @param pageable Page request with size and sort
     * @return List of top player scores for the region
     */
    @Query("SELECT ps FROM PlayerScore ps WHERE ps.region = :region ORDER BY ps.score DESC")
    List<PlayerScore> findTopScoresByRegion(@Param("region") String region, Pageable pageable);

    /**
     * Count total scores - used for pagination metadata
     */
    long count();

    /**
     * Count scores by region
     */
    long countByRegion(String region);

    // OPTIMIZED VERSION (commented out - you'll add this in Step 7 of the checklist):
    /*
    **
     * Optimized query using DTO projection - fetches everything in ONE query
     * This eliminates the N+1 problem completely!
     *
     * Instead of:
     * - 1 query for scores + 100 queries for player profiles (101 total)
     *
     * We get:
     * - 1 query with JOIN that fetches everything (1 total!)
     *
     * @param pageable Page request with limit
     * @return List of leaderboard entries with all player data
     */
    /*
    @Query("SELECT new com.fourcoretech.leaderboard.dto.LeaderboardEntryDTO(" +
           "ps.playerId, p.username, p.displayName, p.avatarUrl, ps.score, " +
           "ps.region, p.country, p.level, p.isPremium, ps.gameMode) " +
           "FROM PlayerScore ps " +
           "JOIN PlayerProfile p ON ps.playerId = p.id " +
           "ORDER BY ps.score DESC")
    List<LeaderboardEntryDTO> findTopScoresOptimized(Pageable pageable);

    @Query("SELECT new com.fourcoretech.leaderboard.dto.LeaderboardEntryDTO(" +
           "ps.playerId, p.username, p.displayName, p.avatarUrl, ps.score, " +
           "ps.region, p.country, p.level, p.isPremium, ps.gameMode) " +
           "FROM PlayerScore ps " +
           "JOIN PlayerProfile p ON ps.playerId = p.id " +
           "WHERE ps.region = :region " +
           "ORDER BY ps.score DESC")
    List<LeaderboardEntryDTO> findTopScoresByRegionOptimized(@Param("region") String region, Pageable pageable);
    */
}
