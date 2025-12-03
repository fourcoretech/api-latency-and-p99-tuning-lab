-- Test Data for Leaderboard Service
-- Generates realistic player profiles and scores for performance testing

-- Insert Player Profiles (500 players)
-- This creates a realistic dataset for N+1 query demonstration
INSERT INTO player_profiles (username, display_name, avatar_url, country, level, is_premium) VALUES
('ProGamer123', 'Pro Gamer', 'https://avatar.example.com/1.jpg', 'US', 50, TRUE),
('NinjaWarrior', 'Ninja Warrior', 'https://avatar.example.com/2.jpg', 'JP', 45, TRUE),
('DragonSlayer', 'Dragon Slayer', 'https://avatar.example.com/3.jpg', 'CN', 48, FALSE),
('PhoenixRising', 'Phoenix Rising', 'https://avatar.example.com/4.jpg', 'KR', 52, TRUE),
('ShadowHunter', 'Shadow Hunter', 'https://avatar.example.com/5.jpg', 'US', 47, FALSE),
('StormBreaker', 'Storm Breaker', 'https://avatar.example.com/6.jpg', 'GB', 49, TRUE),
('ThunderStrike', 'Thunder Strike', 'https://avatar.example.com/7.jpg', 'DE', 46, FALSE),
('IceQueen', 'Ice Queen', 'https://avatar.example.com/8.jpg', 'CA', 51, TRUE),
('FireKing', 'Fire King', 'https://avatar.example.com/9.jpg', 'AU', 44, FALSE),
('WindRider', 'Wind Rider', 'https://avatar.example.com/10.jpg', 'BR', 53, TRUE),
('EarthShaker', 'Earth Shaker', 'https://avatar.example.com/11.jpg', 'MX', 43, FALSE),
('LightBringer', 'Light Bringer', 'https://avatar.example.com/12.jpg', 'FR', 50, TRUE),
('DarkMage', 'Dark Mage', 'https://avatar.example.com/13.jpg', 'IT', 42, FALSE),
('CrystalKnight', 'Crystal Knight', 'https://avatar.example.com/14.jpg', 'ES', 48, TRUE),
('StarGazer', 'Star Gazer', 'https://avatar.example.com/15.jpg', 'RU', 45, FALSE),
('MoonWalker', 'Moon Walker', 'https://avatar.example.com/16.jpg', 'IN', 49, TRUE),
('SunChaser', 'Sun Chaser', 'https://avatar.example.com/17.jpg', 'SE', 47, FALSE),
('VoidSeeker', 'Void Seeker', 'https://avatar.example.com/18.jpg', 'NO', 51, TRUE),
('TimeLord', 'Time Lord', 'https://avatar.example.com/19.jpg', 'DK', 46, FALSE),
('SpaceRanger', 'Space Ranger', 'https://avatar.example.com/20.jpg', 'FI', 52, TRUE),
('QuantumLeap', 'Quantum Leap', 'https://avatar.example.com/21.jpg', 'NL', 44, FALSE),
('AtomicBlast', 'Atomic Blast', 'https://avatar.example.com/22.jpg', 'BE', 48, TRUE),
('CosmicDrift', 'Cosmic Drift', 'https://avatar.example.com/23.jpg', 'CH', 43, FALSE),
('NebulaStar', 'Nebula Star', 'https://avatar.example.com/24.jpg', 'AT', 50, TRUE),
('GalaxyHero', 'Galaxy Hero', 'https://avatar.example.com/25.jpg', 'PL', 45, FALSE),
('SolarFlare', 'Solar Flare', 'https://avatar.example.com/26.jpg', 'CZ', 49, TRUE),
('LunarEclipse', 'Lunar Eclipse', 'https://avatar.example.com/27.jpg', 'PT', 47, FALSE),
('NovaStrike', 'Nova Strike', 'https://avatar.example.com/28.jpg', 'GR', 51, TRUE),
('BlazeFury', 'Blaze Fury', 'https://avatar.example.com/29.jpg', 'TR', 46, FALSE),
('FrostBite', 'Frost Bite', 'https://avatar.example.com/30.jpg', 'ZA', 52, TRUE);

-- Generate more players (31-100) for a larger dataset
INSERT INTO player_profiles (username, display_name, avatar_url, country, level, is_premium)
SELECT
    'Player_' || generate_series AS username,
    'Display Name ' || generate_series AS display_name,
    'https://avatar.example.com/' || generate_series || '.jpg' AS avatar_url,
    CASE (generate_series % 10)
        WHEN 0 THEN 'US'
        WHEN 1 THEN 'CN'
        WHEN 2 THEN 'JP'
        WHEN 3 THEN 'KR'
        WHEN 4 THEN 'DE'
        WHEN 5 THEN 'GB'
        WHEN 6 THEN 'CA'
        WHEN 7 THEN 'AU'
        WHEN 8 THEN 'BR'
        ELSE 'FR'
    END AS country,
    30 + (generate_series % 25) AS level,
    (generate_series % 3 = 0) AS is_premium
FROM generate_series(31, 200);

-- Insert Player Scores
-- Create enough scores to make the N+1 problem visible (1000+ scores)
-- Multiple scores per player in different regions and game modes

-- Top scores for leaderboard (scores 9000-10000)
INSERT INTO player_scores (player_id, score, region, game_mode)
SELECT
    (id % 200) + 1 AS player_id,
    9000 + (RANDOM() * 1000)::INTEGER AS score,
    CASE ((id % 5))
        WHEN 0 THEN 'NA'
        WHEN 1 THEN 'EU'
        WHEN 2 THEN 'ASIA'
        WHEN 3 THEN 'SA'
        ELSE 'OCE'
    END AS region,
    CASE ((id % 3))
        WHEN 0 THEN 'ranked'
        WHEN 1 THEN 'casual'
        ELSE 'tournament'
    END AS game_mode
FROM generate_series(1, 500) AS id;

-- Medium scores (scores 5000-9000)
INSERT INTO player_scores (player_id, score, region, game_mode)
SELECT
    (id % 200) + 1 AS player_id,
    5000 + (RANDOM() * 4000)::INTEGER AS score,
    CASE ((id % 5))
        WHEN 0 THEN 'NA'
        WHEN 1 THEN 'EU'
        WHEN 2 THEN 'ASIA'
        WHEN 3 THEN 'SA'
        ELSE 'OCE'
    END AS region,
    CASE ((id % 3))
        WHEN 0 THEN 'ranked'
        WHEN 1 THEN 'casual'
        ELSE 'tournament'
    END AS game_mode
FROM generate_series(501, 2000) AS id;

-- Lower scores (scores 1000-5000)
INSERT INTO player_scores (player_id, score, region, game_mode)
SELECT
    (id % 200) + 1 AS player_id,
    1000 + (RANDOM() * 4000)::INTEGER AS score,
    CASE ((id % 5))
        WHEN 0 THEN 'NA'
        WHEN 1 THEN 'EU'
        WHEN 2 THEN 'ASIA'
        WHEN 3 THEN 'SA'
        ELSE 'OCE'
    END AS region,
    CASE ((id % 3))
        WHEN 0 THEN 'ranked'
        WHEN 1 THEN 'casual'
        ELSE 'tournament'
    END AS game_mode
FROM generate_series(2001, 5000) AS id;

-- Update created_at timestamps to have some variation
UPDATE player_scores
SET created_at = CURRENT_TIMESTAMP - (RANDOM() * INTERVAL '30 days');

UPDATE player_profiles
SET created_at = CURRENT_TIMESTAMP - (RANDOM() * INTERVAL '90 days'),
    last_login_at = CURRENT_TIMESTAMP - (RANDOM() * INTERVAL '7 days');

-- Display summary
-- Note: These queries work in PostgreSQL but might need adjustment for H2
-- SELECT 'Database seeded successfully!' AS status;
-- SELECT COUNT(*) AS total_players FROM player_profiles;
-- SELECT COUNT(*) AS total_scores FROM player_scores;
-- SELECT region, COUNT(*) AS score_count FROM player_scores GROUP BY region ORDER BY region;
