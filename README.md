# API Latency and P99 Tuning Lab

## 🎯 **Welcome to Your Performance Optimization Journey!**

In this hands-on lab, you'll transform a **slow, unreliable API** into a **fast, production-ready service**. You'll learn to diagnose and fix real-world performance problems that cause high latency and poor user experience.

## 📚 **What You'll Learn**

By the end of this lab, you'll be able to confidently explain and fix:

- **N+1 Query Problem** - The classic database anti-pattern
- **Missing Database Indexes** - Why queries slow down as data grows
- **Blocking I/O Issues** - How synchronous calls kill throughput
- **Caching Strategies** - Using Redis to reduce database load
- **Observability** - Tracking P50/P95/P99 latency with Micrometer
- **Resilience Patterns** - Timeouts, circuit breakers, and retries

**Why This Matters**: In job interviews and on the job, you'll be asked "How would you improve API performance?" or "Our P99 latency is terrible - what would you check?" This lab gives you hands-on experience with the exact problems you'll encounter in production systems.

## 🚀 **Quick Start**

### **Prerequisites**

You'll need:
- **Java 17+** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 12+** or use H2 (in-memory) for easier setup
- **Redis** (optional - for caching phase)
- **Docker & Docker Compose** (optional - for easy database setup)

### **Option 1: Quick Setup with Docker Compose (Recommended)**

```bash
# Start databases (PostgreSQL + Redis)
docker-compose up -d

# Build and run the application
./mvnw spring-boot:run

# Verify service is running
curl http://localhost:8080/leaderboard/health
```

### **Option 2: Setup with H2 In-Memory Database (No Docker needed)**

```bash
# Run with H2 profile (simplest option)
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2

# Access H2 console at: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:leaderboard
# Username: sa
# Password: (leave blank)
```

### **Option 3: Manual PostgreSQL Setup**

```bash
# Create database
createdb leaderboard

# Update src/main/resources/application.yml with your credentials

# Run application
./mvnw spring-boot:run
```

## 🧪 **Run Your First Load Test**

Once the service is running, test its baseline performance:

```bash
# Make the script executable (Linux/Mac)
chmod +x load-test.sh

# Run load test
./load-test.sh

# Windows users: Use Git Bash or WSL, or run individual curl commands
```

**What to expect**:
- **P50 (median) latency**: 300-500ms
- **P99 latency**: 1000-3000ms+ ⚠️ **VERY SLOW!**

This is **intentionally bad** - your job is to fix it!

## 📊 **Understanding the Metrics**

### **What is P99 Latency?**

- **P50 (median)**: Half of requests are faster than this time
- **P95**: 95% of requests are faster than this time
- **P99**: 99% of requests are faster than this time ← **This is what we care about!**

**Why P99 matters**: If your P99 is 3 seconds, that means **1 in 100 users waits 3+ seconds**. With millions of users, that's thousands of people having a terrible experience!

### **View Real-Time Metrics**

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep leaderboard

# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics
```

## 🧨 **Intentional Problems in This Lab**

This service has **SEVEN major performance problems**:

### **1. N+1 Query Problem** 🔴 **Critical**
- Fetches top 100 scores in **1 query**
- Then fetches **each player profile separately** (100 more queries!)
- **Total**: 101 database round-trips instead of 1
- **Impact**: Each query adds 2-5ms → 200-500ms wasted!

### **2. Missing Database Indexes** 🔴 **Critical**
- No index on `score` column (used for sorting)
- No composite index on `(region, score)` for regional queries
- **Impact**: Full table scans → 50-200ms per query
- Gets worse as data grows!

### **3. Blocking I/O with Random Delays** 🟡 **High Impact**
- `Thread.sleep()` simulates slow external services
- 20% of requests get 100-500ms delay
- **Impact**: Thread pool exhaustion, high P99 latency

### **4. No Caching** 🟡 **High Impact**
- Every request hits the database
- Popular leaderboards fetched repeatedly
- **Impact**: Unnecessary database load and latency

### **5. Poor Async Configuration** 🟡 **High Impact**
- Only **2 threads** for async operations
- Huge queue (500) masks the problem
- **Impact**: Tasks pile up, latency spikes

### **6. Small Connection Pool** 🟠 **Medium Impact**
- Only **5 database connections**
- With N+1 queries, connections get exhausted
- **Impact**: Connection wait time adds to latency

### **7. No Timeout/Circuit Breaker** 🟠 **Medium Impact**
- Requests can hang indefinitely
- No protection from cascading failures
- **Impact**: Service can become completely unresponsive

## 📝 **Your Mission: Follow the Checklist**

Open **`checklist-latency.md`** for step-by-step instructions to fix each problem.

The checklist guides you through:
1. **Baseline Measurement** - Capture current performance
2. **Database Optimization** - Add indexes, fix N+1 queries
3. **Caching Implementation** - Add Redis caching
4. **Async Processing** - Convert to non-blocking I/O
5. **Observability** - Enhanced metrics and monitoring
6. **Resilience** - Add timeouts and circuit breakers
7. **Final Verification** - Measure improvements

## 📈 **Expected Results (After Optimization)**

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **P50 Latency** | 928ms | ~100ms | **9x faster** |
| **P95 Latency** | 1765ms | ~145ms | **12x faster** |
| **P99 Latency** | 6097ms | ~180ms | **34x faster** |
| **Database Queries** | 101 | 1 | **99% reduction** |
| **Cache Hit Rate** | 0% | 80%+ | N/A |
| **Throughput** | 50 req/s | 500+ req/s | **10x improvement** |

**Note**: Your exact numbers may vary depending on your hardware, operating system (Windows/Linux/Mac), and whether you're using PostgreSQL or H2. What matters is the dramatic improvement in all metrics!

## 🎓 **Interview-Ready Knowledge**

After completing this lab, you can confidently discuss:

### **Common Interview Questions You Can Now Answer**:

1. **"How would you diagnose high API latency?"**
   - Check P50/P95/P99 metrics
   - Enable SQL logging to spot N+1 queries
   - Review database slow query logs
   - Check thread pool utilization
   - Look for missing indexes with EXPLAIN ANALYZE

2. **"What's the N+1 query problem and how do you fix it?"**
   - Explain with real example from this lab
   - Show JOIN FETCH or DTO projection solutions
   - Discuss when it's acceptable vs critical

3. **"How do you decide what to cache?"**
   - High read, low write ratio
   - Data doesn't change frequently
   - Acceptable staleness (seconds/minutes)
   - Show Redis implementation from this lab

4. **"Why is P99 latency important?"**
   - Affects real users, not just averages
   - P50 looks good but P99 reveals tail latency
   - Causes: GC pauses, network hiccups, lock contention, etc.

5. **"What database indexes would you add for a leaderboard?"**
   - Composite index on (region, score DESC)
   - Index on score for global leaderboard
   - Explain index selection strategy

## 🏗️ **Project Structure**

```
api-latency-and-p99-tuning/
├── src/main/java/com/fourcoretech/leaderboard/
│   ├── LeaderboardServiceApplication.java    # Main application
│   ├── config/
│   │   └── AsyncConfig.java                  # Async thread pool config
│   ├── controller/
│   │   └── LeaderboardController.java        # REST endpoints
│   ├── service/
│   │   └── LeaderboardService.java           # Business logic (WITH PROBLEMS!)
│   ├── repository/
│   │   ├── PlayerScoreRepository.java        # Score data access
│   │   └── PlayerProfileRepository.java      # Profile data access
│   ├── entity/
│   │   ├── PlayerScore.java                  # Score entity
│   │   └── PlayerProfile.java                # Profile entity
│   └── dto/
│       └── LeaderboardEntryDTO.java          # API response DTO
├── src/main/resources/
│   ├── application.yml                       # Configuration (WITH PROBLEMS!)
│   ├── schema.sql                            # Database schema (MISSING INDEXES!)
│   └── data.sql                              # Test data (5000+ records)
├── README.md                                 # This file
├── checklist-latency.md                      # Step-by-step lab guide
├── load-test.sh                              # Load testing script
├── docker-compose.yml                        # PostgreSQL + Redis setup
└── pom.xml                                   # Maven dependencies
```

## 🔧 **API Endpoints**

### **Get Global Top Players**
```bash
GET /leaderboard/top?limit=100

# Example
curl "http://localhost:8080/leaderboard/top?limit=50"
```

### **Get Top Players by Region**
```bash
GET /leaderboard/top/{region}?limit=100

# Example
curl "http://localhost:8080/leaderboard/top/NA?limit=50"

# Valid regions: NA, EU, ASIA, SA, OCE
```

### **Health Check**
```bash
GET /leaderboard/health

curl http://localhost:8080/leaderboard/health
```

### **Metrics**
```bash
GET /actuator/prometheus
GET /actuator/health
GET /actuator/metrics

curl http://localhost:8080/actuator/prometheus
```

## 🎯 **Stretch Challenges (Senior Level)**

Once you've completed the basic optimizations:

### **1. Add Grafana Dashboard**
- Set up Grafana with Docker
- Create dashboard showing P50/P95/P99
- Add panels for DB query count, cache hit rate
- Set up alerts for P99 > 500ms

### **2. Implement Rate Limiting**
- Use Resilience4j RateLimiter
- Protect endpoint from abuse
- Return 429 Too Many Requests

### **3. Add Database Read Replicas**
- Configure read/write split
- Route leaderboard queries to read replica
- Measure impact on primary database

### **4. Optimize Cold Start**
- Pre-warm cache on startup
- Eager load indexes
- Measure startup time improvement

### **5. Add Time-Based Caching**
- Cache top 100 for 30 seconds
- Invalidate cache when new high score
- Implement cache-aside pattern

## 🤔 **Reflection Questions**

Think about these as you work through the lab:

1. **What caused the tail latency (high P99)?**
   - Which problems had the biggest impact?
   - Why does P99 spike more than P50?

2. **Which optimization gave the most improvement?**
   - Database indexes?
   - Fixing N+1 queries?
   - Caching?

3. **What new tradeoffs emerged?**
   - Cache staleness vs consistency
   - Memory usage with caching
   - Complexity vs performance

4. **How would this scale to millions of users?**
   - What would break first?
   - What else would you need?

## 🛠️ **Troubleshooting**

### **"Application won't start"**
- Check Java version: `java -version` (need 17+)
- Check PostgreSQL is running: `docker ps` or `pg_isready`
- Check port 8080 is free: `lsof -i :8080`

### **"Database connection refused"**
- Verify PostgreSQL is running
- Check credentials in `application.yml`
- Try H2 profile: `-Dspring-boot.run.profiles=h2`

### **"Load test script won't run"**
- Make it executable: `chmod +x load-test.sh`
- Use Git Bash on Windows
- Or run curl commands manually

### **"No data returned from API"**
- Check `data.sql` was executed
- Verify database has records: Connect to DB and run `SELECT COUNT(*) FROM player_scores;`
- Check logs for errors

## 📚 **Additional Resources**

- [Spring Boot Performance Tuning](https://spring.io/guides/gs/spring-boot/)
- [Micrometer Metrics](https://micrometer.io/docs)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [PostgreSQL Performance Tips](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Redis Caching Patterns](https://redis.io/docs/manual/patterns/)

## 💡 **Tips for Success**

1. **Read the code comments** - They explain what's wrong and why
2. **Measure before and after** - Always capture baseline metrics
3. **Fix one problem at a time** - Don't try to fix everything at once
4. **Understand the "why"** - Don't just copy code, understand the problem
5. **Test under load** - Performance problems only appear under load

## 🎉 **Next Steps**

1. Start with the **[checklist-latency.md](checklist-latency.md)** for step-by-step instructions
2. Run the baseline load test
3. Fix problems one by one
4. Measure improvements
5. Complete reflection questions
6. Try stretch challenges

## 📞 **Need Help?**

- Check the detailed comments in the code
- Review `checklist-latency.md` for hints
- Search error messages in the logs
- Remember: The problems are intentional - you're supposed to find and fix them!

---

## 🏆 **Completion Certificate**

Once you've completed this lab and can explain the fixes to another person, you have valuable **production-ready skills** that companies are looking for. Update your resume with:

> "Optimized Spring Boot APIs for performance, reducing P99 latency by 97% (from 6000ms to 180ms) through database indexing, N+1 query elimination, Redis caching, and thread pool optimization"

**Achievement unlocked**: You've successfully transformed a slow, unreliable API into a production-ready service with sub-200ms P99 latency!

**Good luck, and happy optimizing!** 🚀
