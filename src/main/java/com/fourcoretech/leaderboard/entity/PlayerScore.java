package com.fourcoretech.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PlayerScore Entity - Represents a player's score in the leaderboard
 *
 * PROBLEM: This table is MISSING CRITICAL INDEXES!
 * - No index on 'score' column (used for sorting in leaderboard queries)
 * - No composite index on (region, score) for regional leaderboards
 * - Results in FULL TABLE SCANS on large datasets
 *
 * REAL-WORLD IMPACT:
 * - Query time grows linearly with table size (O(n) instead of O(log n))
 * - With 10,000 rows, queries take 50-200ms
 * - With 100,000 rows, queries take 500-2000ms
 * - P99 latency spikes as database works harder
 *
 * WHAT YOU'LL ADD:
 * - Index on score column for efficient sorting
 * - Composite index on (region, score) for regional queries
 * - Index on created_at for time-based queries
 */
@Entity
@Table(name = "player_scores")
// NOTE: Indexes are intentionally MISSING - you'll add them in the optimization phase
// @Index(name = "idx_score", columnList = "score DESC")
// @Index(name = "idx_region_score", columnList = "region, score DESC")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player ID - References the PlayerProfile
     * NOTE: This creates an N+1 query problem when we fetch profiles separately
     */
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    /**
     * Score value - NEEDS AN INDEX for efficient sorting
     * This is queried with ORDER BY score DESC in every leaderboard request
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Region - NEEDS A COMPOSITE INDEX with score
     * Often filtered by region: WHERE region = 'NA' ORDER BY score DESC
     */
    @Column(length = 10, nullable = false)
    private String region;

    /**
     * Timestamp when score was recorded
     * Useful for time-based analysis and filtering recent scores
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Game mode or context for the score
     * Examples: "ranked", "casual", "tournament"
     */
    @Column(name = "game_mode", length = 50)
    private String gameMode;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
