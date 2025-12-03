-- Database Schema for Leaderboard Service
-- INTENTIONALLY MISSING INDEXES for learning purposes

-- Player Profiles Table
-- Stores player metadata: username, avatar, country, etc.
DROP TABLE IF EXISTS player_scores;
DROP TABLE IF EXISTS player_profiles;

CREATE TABLE player_profiles (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    country VARCHAR(2),
    level INTEGER DEFAULT 1,
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Player Scores Table
-- Stores individual scores for the leaderboard
-- PROBLEM: MISSING CRITICAL INDEXES!
CREATE TABLE player_scores (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    score INTEGER NOT NULL,
    region VARCHAR(10) NOT NULL,
    game_mode VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES player_profiles(id)
);

-- INDEXES ARE INTENTIONALLY COMMENTED OUT
-- You'll uncomment these during the optimization phase of the lab

-- Index for sorting by score (used in ORDER BY score DESC)
-- Without this, database does a FULL TABLE SCAN and sorts in memory
-- CREATE INDEX idx_score ON player_scores(score DESC);

-- Composite index for regional leaderboards
-- Without this, filtering by region + sorting is VERY slow
-- CREATE INDEX idx_region_score ON player_scores(region, score DESC);

-- Index for time-based queries
-- CREATE INDEX idx_created_at ON player_scores(created_at DESC);

-- Index for game mode queries
-- CREATE INDEX idx_game_mode ON player_scores(game_mode);

-- WHY ARE THESE INDEXES IMPORTANT?
--
-- 1. idx_score:
--    - Query: SELECT * FROM player_scores ORDER BY score DESC LIMIT 100
--    - Without index: Scans ALL rows, sorts in memory (O(n log n))
--    - With index: Uses index to get sorted results directly (O(log n))
--    - Impact: 10x-100x faster on large datasets
--
-- 2. idx_region_score:
--    - Query: SELECT * FROM player_scores WHERE region='NA' ORDER BY score DESC
--    - Without index: Scans ALL rows, filters, then sorts
--    - With index: Uses index for both filter and sort in one operation
--    - Impact: Critical for regional leaderboards
--
-- 3. idx_created_at:
--    - Query: SELECT * FROM player_scores WHERE created_at > '2024-01-01'
--    - Without index: Scans all rows
--    - With index: Quick range scan
--
-- LEARNING POINT:
-- The lack of these indexes will be VERY visible in your P99 latency metrics!
-- You'll see dramatic improvement when you add them.
