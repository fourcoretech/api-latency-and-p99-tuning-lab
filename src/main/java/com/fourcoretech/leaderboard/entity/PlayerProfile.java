package com.fourcoretech.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PlayerProfile Entity - Represents a player's profile information
 *
 * This entity stores player metadata like username, avatar, country, etc.
 * It's separate from PlayerScore to demonstrate N+1 query problems.
 *
 * PROBLEM: The way this is fetched causes N+1 queries
 * - Service fetches top 100 scores in ONE query
 * - Then fetches EACH player profile in SEPARATE queries (100 more queries!)
 * - Total: 101 database round-trips instead of 1
 *
 * REAL-WORLD IMPACT:
 * - Each DB round-trip adds ~1-5ms of latency
 * - 100 extra queries = 100-500ms of added latency
 * - This is a CLASSIC cause of high P99 latency
 * - Database connection pool gets exhausted
 *
 * WHAT YOU'LL FIX:
 * - Use JOIN FETCH or DTO projection to get all data in one query
 * - Or use caching to avoid repeated profile fetches
 */
@Entity
@Table(name = "player_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player username - displayed in leaderboard
     * This is fetched SEPARATELY for each score (N+1 problem!)
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Display name - may differ from username
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * Avatar URL - profile picture
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Player's country code (ISO 3166-1 alpha-2)
     * Examples: US, CA, GB, DE, JP
     */
    @Column(length = 2)
    private String country;

    /**
     * Player level or rank
     */
    @Column
    private Integer level;

    /**
     * Premium/VIP status
     */
    @Column(name = "is_premium")
    private Boolean isPremium;

    /**
     * Account creation timestamp
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Last login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isPremium == null) {
            isPremium = false;
        }
    }
}
