package com.fourcoretech.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object for Leaderboard Entry
 *
 * This DTO combines data from PlayerScore and PlayerProfile entities.
 * It represents what we send to the API client.
 *
 * CURRENT PROBLEM: We build this DTO using N+1 queries
 * - Fetch all scores in one query
 * - For each score, fetch the player profile (N separate queries)
 *
 * BETTER APPROACH (you'll implement):
 * - Use a JOIN query or DTO projection to fetch everything at once
 * - Or use caching to avoid repeated profile lookups
 *
 * IMPORTANT: Implements Serializable for Redis caching
 * - Redis needs to serialize objects to store them in cache
 * - Without Serializable, you'll get NotSerializableException
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Rank position in leaderboard (1 = first place)
     */
    private Integer rank;

    /**
     * Player ID
     */
    private Long playerId;

    /**
     * Player username
     */
    private String username;

    /**
     * Player display name (if different from username)
     */
    private String displayName;

    /**
     * Player avatar URL
     */
    private String avatarUrl;

    /**
     * Player score
     */
    private Integer score;

    /**
     * Region where score was achieved
     */
    private String region;

    /**
     * Player's country
     */
    private String country;

    /**
     * Player level
     */
    private Integer level;

    /**
     * Premium status indicator
     */
    private Boolean isPremium;

    /**
     * Game mode
     */
    private String gameMode;

    /**
     * Constructor for JPQL DTO Projection (used in Step 7)
     *
     * This constructor is called by the @Query projection in PlayerScoreRepository.
     * It creates a DTO from the JOIN query result WITHOUT the rank field.
     * The rank is set separately in the service layer.
     *
     * IMPORTANT: Java allows multiple constructors!
     * - @AllArgsConstructor creates one with ALL fields (including rank)
     * - This constructor is for the database query (WITHOUT rank)
     *
     * @param playerId Player ID
     * @param username Player username
     * @param displayName Player display name
     * @param avatarUrl Player avatar URL
     * @param score Player score
     * @param region Region code
     * @param country Country code
     * @param level Player level
     * @param isPremium Premium status
     * @param gameMode Game mode
     */
    public LeaderboardEntryDTO(Long playerId, String username, String displayName,
                               String avatarUrl, Integer score, String region,
                               String country, Integer level, Boolean isPremium, String gameMode) {
        this.playerId = playerId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.score = score;
        this.region = region;
        this.country = country;
        this.level = level;
        this.isPremium = isPremium;
        this.gameMode = gameMode;
        // Note: rank is NOT set here - it will be set by the service layer
    }
}
