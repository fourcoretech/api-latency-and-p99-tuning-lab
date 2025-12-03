# Quick Start Guide - API Latency & P99 Tuning Lab

## 🚀 **Get Started in 3 Steps**

### **Step 1: Start the Databases**

Choose your preferred method:

**Option A - Docker Compose (Recommended):**
```bash
docker-compose up -d
```

**Option B - Skip Docker, Use H2 In-Memory Database:**
```bash
# No setup needed - H2 runs in memory
# Just run with H2 profile in Step 2
```

---

### **Step 2: Run the Application**

**With PostgreSQL:**
```bash
cd api-latency-and-p99-tuning
./mvnw spring-boot:run
```

**With H2 (no Docker):**
```bash
cd api-latency-and-p99-tuning
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

**Windows users:**
```cmd
cd api-latency-and-p99-tuning
mvnw.cmd spring-boot:run
```

Wait for: `Started LeaderboardServiceApplication in X seconds`

---

### **Step 3: Test It Works**

```bash
# Health check
curl http://localhost:8080/leaderboard/health

# Get top players
curl "http://localhost:8080/leaderboard/top?limit=10"

# Run load test (make executable first on Linux/Mac)
chmod +x load-test.sh
./load-test.sh
```

**Expected Output:**
- Health check returns: `{"status":"UP",...}`
- Top players returns JSON with 10 player entries
- Load test shows **SLOW** P99 latency (1000-3000ms) ← This is intentional!

---

## 📚 **What's Next?**

1. **Review the problems**: Open `README.md` to understand the 7 performance issues
2. **Follow the lab**: Open `checklist-latency.md` for step-by-step fixes
3. **Measure improvement**: Run load tests before and after each fix

---

## 🔧 **Troubleshooting**

**"Port 8080 already in use":**
```bash
# Find and kill the process using port 8080
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac:
lsof -ti:8080 | xargs kill -9
```

**"Database connection refused":**
```bash
# Check PostgreSQL is running
docker ps

# Or use H2 instead:
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

**"mvnw: command not found":**
```bash
# Make it executable (Linux/Mac)
chmod +x mvnw

# Or use system Maven
mvn spring-boot:run
```

---

## 📊 **Key Endpoints**

| Endpoint | Purpose |
|----------|---------|
| `GET /leaderboard/top?limit=100` | Top players globally |
| `GET /leaderboard/top/NA?limit=50` | Top players in North America |
| `GET /leaderboard/health` | Health check |
| `GET /actuator/prometheus` | Metrics endpoint |
| `GET /actuator/health` | Detailed health info |

---

## 🎯 **Lab Goals**

Transform this service from:
- **P99 Latency**: 2500ms → 150ms (16x improvement)
- **DB Queries**: 101 queries → 1 query (99% reduction)
- **Cache Hit Rate**: 0% → 80%+

By fixing:
1. N+1 query problem
2. Missing database indexes
3. No caching
4. Small thread pools
5. Blocking I/O
6. No timeouts
7. Poor observability

---

## 🏆 **Success Checklist**

- ✅ Application starts without errors
- ✅ Health check returns 200 OK
- ✅ API returns top players
- ✅ Load test runs successfully
- ✅ You see SLOW P99 latency (the problem!)
- ✅ Ready to start optimizing

---

**Ready? Open `checklist-latency.md` and start optimizing!** 🚀
