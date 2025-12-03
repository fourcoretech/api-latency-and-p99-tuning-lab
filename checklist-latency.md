# API Latency & P99 Tuning - Lab Checklist

## 🎯 **Your Mission**

Transform a slow API into a fast, production-ready service through systematic optimization.

---

## 📋 **Phase 0: Setup and Baseline** (15 minutes)

### **Step 1: Get the Service Running**

Choose your setup method:

**Option A: Docker Compose (Recommended)**
```bash
# Start PostgreSQL and Redis
cd api-latency-and-p99-tuning
docker-compose up -d

# Verify databases are running
docker ps

# Build and run the application
./mvnw clean spring-boot:run

# Wait for "Started LeaderboardServiceApplication" in logs
```

**Option B: H2 In-Memory (Simplest)**
```bash
cd api-latency-and-p99-tuning
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2

# Access H2 console: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:leaderboard
```

**✅ Checkpoint**: Visit http://localhost:8080/leaderboard/health
- You should see: `{"status":"UP",...}`

---

### **Step 2: Explore the API**

```bash
# Get top 100 players globally
curl "http://localhost:8080/leaderboard/top?limit=100"

# Get top 50 players in North America
curl "http://localhost:8080/leaderboard/top/NA?limit=50"

# Check metrics endpoint
curl http://localhost:8080/actuator/prometheus | grep leaderboard
```

**What to observe**:
- Response includes `rank`, `username`, `score`, `region`, etc.
- Response time is SLOW (you'll see in next step)

---

### **Step 3: Run Baseline Load Test**

```bash
# Make script executable (Linux/Mac)
chmod +x load-test.sh

# Run baseline test
./load-test.sh

# Save the output for comparison later!
```

**📊 Record Your Baseline Metrics**:

| Metric | Baseline Value | Target Value | Final Value |
|--------|----------------|--------------|-------------|
| P50 (median) | _____ms | <100ms | _____ms |
| P95 | _____ms | <150ms | _____ms |
| **P99** | _____ms | <200ms | _____ms |
| Avg Response Time | _____ms | <80ms | _____ms |

**✅ Checkpoint**: Your P99 should be 1000-3000ms (VERY SLOW - that's the problem!)

---

### **Step 4: Understand the Problems**

Open these files and read the comments:
1. `src/main/java/com/fourcoretech/leaderboard/service/LeaderboardService.java`
2. `src/main/java/com/fourcoretech/leaderboard/repository/PlayerScoreRepository.java`
3. `src/main/resources/schema.sql`
4. `src/main/resources/application.yml`

**🔍 Questions to Answer**:
- How many database queries are made for `/leaderboard/top?limit=100`?
- Answer: _____ (Hint: It's more than 1!)
- What indexes are missing from the database?
- Answer: _____________________________

---

## 📋 **Phase 1: Fix Database Problems** (30 minutes)

### **Problem**: N+1 queries + missing indexes = VERY SLOW

---

### **Step 5: See the N+1 Problem in Action**

Enable SQL logging to see all queries:

**Edit `src/main/resources/application.yml`:**
```yaml
spring:
  jpa:
    show-sql: true    # Change from false to true
```

**Restart the service and make a request:**
```bash
# Ctrl+C to stop service
./mvnw spring-boot:run

# In another terminal:
curl "http://localhost:8080/leaderboard/top?limit=10"
```

**Look at your console logs**. You should see:
1. **One** SELECT query for scores
2. **Ten** SELECT queries for player profiles (one per player!)

**This is the N+1 problem!** (1 + N queries instead of 1 query)

**✅ Checkpoint**: Count the SELECT statements in logs. With limit=10, you should see 11 queries.

---

### **Step 6: Add Missing Database Indexes**

**Why**: Without indexes, database scans ENTIRE table to find and sort data.

**Edit `src/main/resources/schema.sql`:**

Find the commented-out index lines and **UNCOMMENT them**:

```sql
-- BEFORE (commented out):
-- CREATE INDEX idx_score ON player_scores(score DESC);
-- CREATE INDEX idx_region_score ON player_scores(region, score DESC);

-- AFTER (uncommented):
CREATE INDEX idx_score ON player_scores(score DESC);
CREATE INDEX idx_region_score ON player_scores(region, score DESC);
```

**Apply the changes:**

**Option 1 - Restart with fresh database:**
```bash
# Stop service (Ctrl+C)
# Drop and recreate database
docker-compose down
docker-compose up -d
./mvnw spring-boot:run
```

**Option 2 - Add indexes manually (if using existing DB):**
```bash
# Connect to PostgreSQL
docker exec -it leaderboard-postgres psql -U postgres -d leaderboard

# Run commands:
CREATE INDEX idx_score ON player_scores(score DESC);
CREATE INDEX idx_region_score ON player_scores(region, score DESC);

# Exit psql
\q
```

**Test the improvement:**
```bash
# Run a few test requests
curl "http://localhost:8080/leaderboard/top?limit=100"

# Observe logs - queries should be faster
```

**✅ Checkpoint**: Queries should be noticeably faster in logs (look for execution time).

---

### **Step 7: Fix the N+1 Query Problem**

**There are 2 approaches. We'll use the better one: DTO Projection**

The `LeaderboardEntryDTO` class already has the constructor needed for query projection! Look at the file and you'll see it has TWO constructors:
1. One created by `@AllArgsConstructor` (with all 11 fields including rank)
2. One for DTO projection (10 fields, no rank) - this is the one the query will use

**Edit `src/main/java/com/fourcoretech/leaderboard/repository/PlayerScoreRepository.java`:**

Find the commented-out optimized methods near the bottom of the file (around line 75-107) and **UNCOMMENT them**:
6
```java
// BEFORE (commented out):
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

// AFTER (uncommented - remove the /* and */ lines):
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
```

**What's happening here?**
- The query uses `new LeaderboardEntryDTO(...)` to create DTOs directly in the database query
- This calls the 10-parameter constructor in LeaderboardEntryDTO (without rank)
- The rank field will be set separately in the service layer (see next step)

**Update the Service to use the optimized queries:**

**Edit `src/main/java/com/fourcoretech/leaderboard/service/LeaderboardService.java`:**

**Replace the entire `getTopPlayers` method** with:

```java
public List<LeaderboardEntryDTO> getTopPlayers(int limit) {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        log.info("Fetching top {} players (OPTIMIZED VERSION)", limit);

        // NEW: Use optimized query - ONE query instead of N+1!
        List<LeaderboardEntryDTO> leaderboard = scoreRepository.findTopScoresOptimized(
                PageRequest.of(0, limit));

        // Add rank numbers
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        log.info("Built leaderboard with {} entries (using only 1 DB query!)", leaderboard.size());
        return leaderboard;

    } finally {
        sample.stop(Timer.builder("leaderboard.get_top_players")
                .description("Total time to fetch top players")
                .tag("limit", String.valueOf(limit))
                .register(meterRegistry));
    }
}
```

**Replace the entire `getTopPlayersByRegion` method** with:

```java
public List<LeaderboardEntryDTO> getTopPlayersByRegion(String region, int limit) {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        log.info("Fetching top {} players for region {} (OPTIMIZED)", limit, region);

        // NEW: Use optimized query with JOIN
        List<LeaderboardEntryDTO> leaderboard = scoreRepository.findTopScoresByRegionOptimized(
                region, PageRequest.of(0, limit));

        // Add rank numbers
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        log.info("Built regional leaderboard with {} entries (1 query!)", leaderboard.size());
        return leaderboard;

    } finally {
        sample.stop(Timer.builder("leaderboard.get_top_players_by_region")
                .description("Total time to fetch top players by region")
                .tag("region", region)
                .tag("limit", String.valueOf(limit))
                .register(meterRegistry));
    }
}
```

**Restart and test:**
```bash
# Stop service (Ctrl+C)
./mvnw spring-boot:run

# Test in another terminal
curl "http://localhost:8080/leaderboard/top?limit=100"
```

**Look at the logs now**. You should see:
- **Only 1 SELECT query** (with a JOIN)
- **No more N+1 queries!**
- Much faster response time

**✅ Checkpoint**:
- Logs show only ONE query
- Response time improved significantly
- Run load test again: `./load-test.sh`
- P99 should be 500-1000ms (better, but still not great)

---

## 📋 **Phase 2: Increase Thread Pool and Connection Pool** (15 minutes)

### **Problem**: Too few threads and connections = bottleneck

---

### **Step 8: Increase Database Connection Pool**

**Edit `src/main/resources/application.yml`:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # CHANGED from 5
      minimum-idle: 10            # CHANGED from 2
      connection-timeout: 30000
```

---

### **Step 9: Increase Web Server Thread Pool**

**Still in `src/main/resources/application.yml`:**

```yaml
server:
  tomcat:
    threads:
      max: 200                    # CHANGED from 20
      min-spare: 10               # CHANGED from 2
    max-connections: 10000        # CHANGED from 100
    accept-count: 100             # CHANGED from 10
```

---

### **Step 10: Fix Async Thread Pool Configuration**

**Edit `src/main/java/com/fourcoretech/leaderboard/config/AsyncConfig.java`:**

Find the `getAsyncExecutor()` method and update it:

```java
@Override
public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // FIXED: Proper thread pool sizing
    executor.setCorePoolSize(20);           // CHANGED from 2
    executor.setMaxPoolSize(50);            // CHANGED from 2
    executor.setQueueCapacity(100);         // CHANGED from 500
    executor.setThreadNamePrefix("leaderboard-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    // Note: setAwaitTerminationSeconds is optional - the above setting is sufficient

    executor.initialize();
    return executor;
}
```

**What changed?**
- Core pool size: 2 → 20 threads (can handle 20 concurrent async tasks)
- Max pool size: 2 → 50 threads (can scale up to 50 under load)
- Queue capacity: 500 → 100 (smaller queue means faster failure instead of hiding issues)

**Restart and test:**
```bash
./mvnw spring-boot:run

# Run load test
./load-test.sh
```

**✅ Checkpoint**: P99 should be 400-700ms (getting better!)

---

## 📋 **Phase 3: Add Redis Caching** (20 minutes)

### **Problem**: Every request hits database even for same data

---

### **Step 11: Start Redis (if not already running)**

```bash
# If using Docker Compose
docker-compose up -d

# Verify Redis is running
docker ps | grep redis

# Or start Redis manually
redis-server
```

---

### **Step 12: Enable Caching in Configuration**

**Edit `src/main/resources/application.yml`:**

```yaml
spring:
  cache:
    type: redis          # CHANGED from 'none'
    redis:
      time-to-live: 30000   # Cache for 30 seconds
```

---

### **Step 13: Make DTO Serializable for Redis**

**CRITICAL**: Before we can cache objects in Redis, they must be serializable!

**Edit `src/main/java/com/fourcoretech/leaderboard/dto/LeaderboardEntryDTO.java`:**

Add the import at the top:
```java
import java.io.Serializable;
```

Make the class implement `Serializable`:
```java
// BEFORE:
public class LeaderboardEntryDTO {

// AFTER:
public class LeaderboardEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // rest of class...
```

**Why is this needed?**
- Redis stores objects as byte arrays
- Spring's default serializer requires objects to implement `Serializable`
- Without this, you'll get `NotSerializableException` when trying to cache

---

### **Step 14: Add @Cacheable Annotation to Service**

**Edit `src/main/java/com/fourcoretech/leaderboard/service/LeaderboardService.java`:**

Add the import at the top:
```java
import org.springframework.cache.annotation.Cacheable;
```

Update the `getTopPlayers` method signature:
```java
@Cacheable(value = "leaderboard:top", key = "#limit")
public List<LeaderboardEntryDTO> getTopPlayers(int limit) {
    // existing code...
}
```

Update the `getTopPlayersByRegion` method signature:
```java
@Cacheable(value = "leaderboard:region", key = "#region + ':' + #limit")
public List<LeaderboardEntryDTO> getTopPlayersByRegion(String region, int limit) {
    // existing code...
}
```

---

**Edit `src/main/java/com/fourcoretech/leaderboard/LeaderboardServiceApplication.java`:**

Add annotation to class:
```java
@SpringBootApplication
@EnableAsync
@EnableCaching    // ADD THIS
public class LeaderboardServiceApplication {
```

Add import:
```java
import org.springframework.cache.annotation.EnableCaching;
```

**Restart and test:**
```bash
./mvnw spring-boot:run

# First request (cache miss - slow)
time curl "http://localhost:8080/leaderboard/top?limit=100"

# Second request (cache hit - FAST!)
time curl "http://localhost:8080/leaderboard/top?limit=100"

# Third request (cache hit - FAST!)
time curl "http://localhost:8080/leaderboard/top?limit=100"
```

**Watch the logs**:
- First request: "Fetching top 100 players" + database query
- Second request: No log entry (served from cache!)

**✅ Checkpoint**:
- Second request should be 10-50ms (cached)
- Run load test: `./load-test.sh`
- P99 should be 200-400ms (much better!)

---

---

## 📋 **Phase 4: Remove Simulated Latency** (5 minutes)

### **Problem**: Random delays simulate slow external services

For this lab, we'll remove the simulated delays to see our true performance. In a real app, you'd fix the actual slow operations.

---

### **Step 16: Comment Out Latency Simulation**

**Edit `src/main/java/com/fourcoretech/leaderboard/service/LeaderboardService.java`:**

Find and **comment out** or delete the calls to simulation methods:

```java
public List<LeaderboardEntryDTO> getTopPlayers(int limit) {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        log.info("Fetching top {} players (OPTIMIZED VERSION)", limit);

        // COMMENTED OUT: simulateRandomLatencySpike("top_players");

        List<LeaderboardEntryDTO> leaderboard = scoreRepository.findTopScoresOptimized(
                PageRequest.of(0, limit));

        // ... rest of method
```

Do the same for `getTopPlayersByRegion` method.

**Or, disable via configuration:**

**Edit `src/main/resources/application.yml`:**

```yaml
leaderboard:
  simulation:
    latency-spike-probability: 0.0    # CHANGED from 0.2 (0% chance)
```

**Restart and test:**
```bash
./mvnw spring-boot:run
./load-test.sh
```

**✅ Checkpoint**: P99 should be 100-200ms (EXCELLENT!)

---

## 📋 **Phase 5: Verify Timeout Protection** (5 minutes)

### **Good News**: Server-level timeouts are already configured!

The `application.yml` already has timeout protection at the Tomcat server level. This means:
- Requests are automatically terminated if they take too long
- Thread pools won't get exhausted by slow requests
- No additional code changes needed!

---

### **Step 17: Verify Server Timeout Configuration**

**Review `src/main/resources/application.yml`:**

Look for the server configuration section:

```yaml
server:
  tomcat:
    threads:
      max: 200                    # We increased this in Step 9
      min-spare: 10
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20000     # 20 second connection timeout
```

**What this does:**
- Connections timeout after 20 seconds
- Thread pool is sized correctly (200 threads)
- Max connections: 10,000 (handles high concurrency)

**This is already protecting us!** Combined with our optimizations (caching, query fixes), requests should complete in <200ms, well under the timeout.

---

### **Step 18: Test Under Load**

Let's verify the service handles load gracefully:

```bash
# Run standard load test
./load-test.sh

# Try higher concurrency
REQUESTS=2000 CONCURRENCY=50 ./load-test.sh

# Check for any timeout errors
curl http://localhost:8080/actuator/metrics/http.server.requests | grep -i timeout
```

**✅ Checkpoint**:
- No timeout errors even under high load
- P99 latency stays under 200ms
- All requests succeed

---

## 📋 **Phase 6: Final Verification** (10 minutes)

### **Step 19: Run Final Load Test**

```bash
# Run comprehensive load test
./load-test.sh

# Try higher load
REQUESTS=1000 CONCURRENCY=20 ./load-test.sh

# Test regional endpoint
ENDPOINT=/leaderboard/top/NA?limit=100 ./load-test.sh
```

**📊 Record Final Metrics:**

| Metric | Baseline | Final | Improvement |
|--------|----------|-------|-------------|
| **P50** | _____ms | _____ms | __x faster |
| **P95** | _____ms | _____ms | __x faster |
| **P99** | _____ms | _____ms | __x faster |
| **DB Queries** | 101 | 1 | 99% less |
| **Cache Hit Rate** | 0% | ~80% | N/A |

---

### **Step 20: Verify Metrics in Prometheus**

```bash
# Check custom metrics
curl http://localhost:8080/actuator/prometheus | grep leaderboard

# Look for:
# - leaderboard_get_top_players_seconds (should show improved times)
# - leaderboard_latency_spike_total (should be 0 or very low)
# - cache hit/miss metrics
```

---

### **Step 21: Review What You Fixed**

**Checklist of fixes:**
- ✅ Added database indexes (idx_score, idx_region_score)
- ✅ Eliminated N+1 queries with DTO projection
- ✅ Increased connection pool size (5 → 20)
- ✅ Increased thread pool size (20 → 200)
- ✅ Added Redis caching with @Cacheable
- ✅ Removed/reduced simulated latency
- ✅ Added timeout protection with Resilience4j

**Expected improvement**: P99 latency reduced by **10-20x**!

---

## 🎓 **Reflection Questions**

Answer these to solidify your learning:

### **1. What caused the high P99 latency in the original code?**

Write your answer:
```
Primary causes:
-
-
-
```

### **2. Which optimization had the biggest impact?**

Write your answer:
```
The most impactful fix was _____________ because:


```

### **3. What's the tradeoff of caching?**

Write your answer:
```
Pros:
-
-

Cons:
-
-
```

### **4. How would you explain the N+1 query problem to a junior developer?**

Write your answer:
```



```

### **5. In a production system, how would you decide what to cache?**

Write your answer:
```
I would cache data when:
1.
2.
3.

I would NOT cache when:
1.
2.
```

---

## 🏆 **Stretch Challenges**

### **Challenge 1: Set up Grafana Dashboard**

1. Add Grafana to docker-compose.yml
2. Configure Prometheus data source
3. Create dashboard showing:
   - P50/P95/P99 latency over time
   - Request rate
   - Error rate
   - Cache hit ratio
   - Database connection pool usage

### **Challenge 2: Implement Cache Warming**

Add code to pre-warm the cache on application startup:
- Fetch top 100 players for all regions
- Load into cache before accepting traffic
- Measure improvement in first request

### **Challenge 3: Add Circuit Breaker with Resilience4j**

Implement circuit breaker pattern using Resilience4j:

1. Add Resilience4j configuration to `application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      leaderboard:
        sliding-window-size: 100
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 10
```

2. Wrap service calls with circuit breaker:
```java
@Autowired
private CircuitBreakerRegistry circuitBreakerRegistry;

public List<LeaderboardEntryDTO> getTopPlayers(int limit) {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("leaderboard");

    return circuitBreaker.executeSupplier(() -> {
        // existing service logic
    });
}
```

3. Test circuit breaker behavior:
   - Simulate failures by stopping Redis
   - Verify circuit opens after 50% failure rate
   - Check circuit closes after successful calls

### **Challenge 4: Add Rate Limiting**

Use Resilience4j RateLimiter:
- Limit to 100 requests per second per user
- Return 429 Too Many Requests when exceeded
- Add header with retry-after time

### **Challenge 5: Optimize for Large Datasets**

Test with 100,000+ scores:
- Measure query performance
- Add pagination support
- Implement cursor-based pagination
- Compare performance of offset vs cursor

### **Challenge 6: Add Real-time Updates**

Implement WebSocket endpoint:
- Push leaderboard updates to connected clients
- Invalidate cache when new high score
- Use Redis pub/sub for coordination

---

## 📈 **Success Criteria**

You've successfully completed this lab when:

- ✅ P99 latency is under 200ms (down from 2000ms+)
- ✅ Database queries reduced from 101 to 1 for top 100
- ✅ Cache hit rate is above 70%
- ✅ You can explain each problem and fix to another person
- ✅ You understand when to apply each optimization

---

## 🎉 **Congratulations!**

You've successfully optimized an API and reduced P99 latency by 10-20x! These are real-world skills that companies value highly.

**Interview-Ready Skills You Now Have:**
- Diagnosing N+1 query problems
- Adding database indexes strategically
- Implementing caching with Redis
- Understanding P50/P95/P99 metrics
- Applying resilience patterns
- Optimizing thread pools and connection pools

**Update your resume with:**
> "Optimized Spring Boot APIs for performance, reducing P99 latency by 90% through database indexing, N+1 query elimination, Redis caching, and async processing"

**Next Steps:**
1. Review the reflection questions
2. Try the stretch challenges
3. Apply these techniques to your own projects
4. Share your learnings with your team

**Great work!** 🚀
