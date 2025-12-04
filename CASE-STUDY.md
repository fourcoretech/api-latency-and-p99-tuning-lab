# Case Study – API Latency & P99 Tuning Lab

## Problem

A leaderboard API with:

- N+1 query pattern (101 queries for top 100 players)
- Missing indexes on `score` and `(region, score)`
- Small DB + web server thread pools
- No caching
- Random artificial latency spikes

Baseline performance (500 requests, concurrency 10):

- Avg: 1289ms
- P50: 928ms
- P95: 1765ms
- P99: 6097ms

## Approach

1. **Database**
   - Added `idx_score` and `idx_region_score` indexes.
   - Replaced N+1 queries with a single DTO projection join.

2. **Threading & Connections**
   - Increased Hikari connection pool from 5 → 20.
   - Increased Tomcat thread pool from 20 → 200.
   - Fixed async executor (2 → 20 core threads, 50 max).

3. **Caching**
   - Enabled Redis cache for global and regional leaderboards with `@Cacheable`.
   - Tuned TTL to 30 seconds to balance freshness and performance.

4. **Latency Spikes & Timeouts**
   - Removed simulated random latency.
   - Verified Tomcat timeouts and metrics via Prometheus.

## Results

Final performance (500 requests, concurrency 10):

- Avg: 108ms
- P50: 101ms
- P95: 146ms
- P99: 179ms

Improvements:

- P99 reduced from **6097ms → 179ms** (~34x faster).
- Queries per request reduced from **101 → 1**.
- Cache hit rate ~80% under load.

## What This Demonstrates

- Ability to diagnose high P99 latency using metrics and logs.
- Practical query optimization and index design.
- Thread pool and connection pool tuning for throughput.
- Using Redis caching strategically (with tradeoffs).
- Communicating performance work as a clear before/after story.

## How I Use This in Interviews

When I get questions like:

- **"How would you diagnose high API latency?"**  
  I walk through this lab: baseline P99 at 6097ms, how I used metrics + SQL logging to find N+1, missing indexes, and thread pool limits.

- **"Tell me about a time you improved performance."**  
  I use this as a concrete story: 34x P99 improvement, 99% fewer DB queries, and the exact changes made (indexes, DTO projections, Redis, pool tuning).

- **"How do you decide what to cache?"**  
  I reference the leaderboard use case: read-heavy, tolerant of slight staleness, small payloads, and how I picked a 30s TTL.

## If This Were a Real Production System

If I owned this in production, next steps would be:

- Add a Grafana dashboard for P50/P95/P99, error rate, and cache hit ratio.
- Run load tests with much larger datasets (100k+ rows) and introduce pagination.
- Add rate limiting and circuit breakers around any external dependencies.
- Profile JVM (GC, heap, CPU) under load to ensure no hidden bottlenecks.
