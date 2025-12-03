package com.fourcoretech.leaderboard.repository;

import com.fourcoretech.leaderboard.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PlayerProfile entity
 *
 * PROBLEM: This repository is called in a LOOP by the service
 * - For each PlayerScore, we call findById() separately
 * - This creates N database round-trips
 * - Classic N+1 query anti-pattern
 *
 * REAL-WORLD IMPACT:
 * - Each findById() call takes ~2-5ms
 * - For 100 players: 200-500ms just for profile lookups!
 * - Database connection pool exhaustion under high load
 * - Unnecessary network latency for each query
 *
 * WHAT YOU'LL FIX:
 * - Use findAllById() to fetch multiple profiles at once
 * - Or better: use JOIN in the score query to get everything together
 * - Add caching for frequently accessed profiles
 */
@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    /**
     * Find player by ID - CALLED IN A LOOP (N+1 problem!)
     *
     * The service does this:
     * for (PlayerScore score : scores) {
     *     PlayerProfile profile = repository.findById(score.getPlayerId());
     *     // ... build DTO
     * }
     *
     * This is TERRIBLE for performance!
     */
    Optional<PlayerProfile> findById(Long id);

    /**
     * Find player by username
     */
    Optional<PlayerProfile> findByUsername(String username);

    /**
     * Find multiple players by IDs - BETTER but still not optimal
     *
     * This reduces N queries to 1 query, but it's still an extra round-trip.
     * Better to JOIN in the original score query.
     */
    List<PlayerProfile> findAllById(Iterable<Long> ids);

    /**
     * Find players by country
     */
    List<PlayerProfile> findByCountry(String country);

    /**
     * Find premium players
     */
    List<PlayerProfile> findByIsPremium(Boolean isPremium);
}
