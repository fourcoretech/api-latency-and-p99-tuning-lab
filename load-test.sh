#!/bin/bash

# Load Test Script for Leaderboard Service
# Tests API latency and measures P50, P95, P99 response times

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
ENDPOINT="${ENDPOINT:-/leaderboard/top?limit=100}"
REQUESTS="${REQUESTS:-500}"
CONCURRENCY="${CONCURRENCY:-10}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Leaderboard Service - Load Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo "  API URL:      $API_URL"
echo "  Endpoint:     $ENDPOINT"
echo "  Requests:     $REQUESTS"
echo "  Concurrency:  $CONCURRENCY"
echo ""

# Check if service is running
echo -e "${YELLOW}Checking service health...${NC}"
if ! curl -s -f "${API_URL}/actuator/health" > /dev/null; then
    echo -e "${RED}ERROR: Service is not running at ${API_URL}${NC}"
    echo "Start the service first: ./mvnw spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✓ Service is running${NC}"
echo ""

# Create output directory
OUTPUT_DIR="load-test-results"
mkdir -p "$OUTPUT_DIR"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULT_FILE="$OUTPUT_DIR/test_${TIMESTAMP}.txt"

# Function to run load test using curl
run_curl_test() {
    echo -e "${YELLOW}Running load test with curl...${NC}"
    echo "This will take a moment..."
    echo ""

    TOTAL_TIME=0
    declare -a RESPONSE_TIMES

    for i in $(seq 1 $REQUESTS); do
        # Measure response time in milliseconds
        START=$(date +%s%N)
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${API_URL}${ENDPOINT}")
        END=$(date +%s%N)

        # Calculate time in milliseconds
        TIME_MS=$(( (END - START) / 1000000 ))
        RESPONSE_TIMES[$i]=$TIME_MS
        TOTAL_TIME=$((TOTAL_TIME + TIME_MS))

        # Progress indicator
        if [ $((i % 50)) -eq 0 ]; then
            echo "  Progress: $i/$REQUESTS requests completed..."
        fi

        # Check for errors
        if [ "$HTTP_CODE" != "200" ]; then
            echo -e "${RED}  ERROR: Request $i failed with HTTP $HTTP_CODE${NC}"
        fi
    done

    # Sort response times
    IFS=$'\n' SORTED=($(sort -n <<<"${RESPONSE_TIMES[*]}"))
    unset IFS

    # Calculate statistics
    AVG=$((TOTAL_TIME / REQUESTS))
    MIN=${SORTED[1]}
    MAX=${SORTED[$REQUESTS]}

    # Calculate percentiles
    P50_INDEX=$((REQUESTS * 50 / 100))
    P95_INDEX=$((REQUESTS * 95 / 100))
    P99_INDEX=$((REQUESTS * 99 / 100))

    P50=${SORTED[$P50_INDEX]}
    P95=${SORTED[$P95_INDEX]}
    P99=${SORTED[$P99_INDEX]}

    # Display results
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Load Test Results${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "  Total Requests:     $REQUESTS"
    echo "  Average:            ${AVG}ms"
    echo "  Min:                ${MIN}ms"
    echo "  Max:                ${MAX}ms"
    echo ""
    echo -e "${YELLOW}  Latency Percentiles:${NC}"
    echo "  P50 (median):       ${P50}ms"
    echo "  P95:                ${P95}ms"
    echo -e "${RED}  P99:                ${P99}ms${NC}  ← This is the key metric!"
    echo ""

    # Save results to file
    {
        echo "Load Test Results - $(date)"
        echo "========================================"
        echo "Configuration:"
        echo "  API URL:      $API_URL"
        echo "  Endpoint:     $ENDPOINT"
        echo "  Requests:     $REQUESTS"
        echo "  Concurrency:  $CONCURRENCY"
        echo ""
        echo "Results:"
        echo "  Total Requests:     $REQUESTS"
        echo "  Average:            ${AVG}ms"
        echo "  Min:                ${MIN}ms"
        echo "  Max:                ${MAX}ms"
        echo "  P50:                ${P50}ms"
        echo "  P95:                ${P95}ms"
        echo "  P99:                ${P99}ms"
    } > "$RESULT_FILE"

    echo -e "${GREEN}Results saved to: $RESULT_FILE${NC}"
}

# Function to run load test using Apache Bench (if available)
run_ab_test() {
    if ! command -v ab &> /dev/null; then
        echo -e "${YELLOW}Apache Bench (ab) not found, skipping...${NC}"
        return
    fi

    echo -e "${YELLOW}Running load test with Apache Bench...${NC}"
    AB_OUTPUT="$OUTPUT_DIR/ab_${TIMESTAMP}.txt"

    ab -n "$REQUESTS" -c "$CONCURRENCY" -g "$OUTPUT_DIR/ab_${TIMESTAMP}.tsv" \
       "${API_URL}${ENDPOINT}" | tee "$AB_OUTPUT"

    echo ""
    echo -e "${GREEN}Apache Bench results saved to: $AB_OUTPUT${NC}"
}

# Function to display Prometheus metrics
show_prometheus_metrics() {
    echo ""
    echo -e "${YELLOW}Fetching metrics from Prometheus endpoint...${NC}"
    echo ""

    METRICS=$(curl -s "${API_URL}/actuator/prometheus" | grep "leaderboard_")

    if [ -z "$METRICS" ]; then
        echo -e "${YELLOW}No custom metrics found yet. Run the load test first.${NC}"
    else
        echo "$METRICS" | grep -E "(get_top_players|profile_fetch|api_request)" | head -20
    fi

    echo ""
    echo -e "${BLUE}View all metrics at: ${API_URL}/actuator/prometheus${NC}"
}

# Main execution
echo -e "${YELLOW}Starting load test...${NC}"
echo ""

# Run the test
run_curl_test

# Try Apache Bench if available
echo ""
run_ab_test

# Show metrics
show_prometheus_metrics

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Load Test Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Review the P99 latency (should be VERY HIGH with current problems)"
echo "  2. Follow the lab checklist to optimize the code"
echo "  3. Run this script again to see improvements"
echo "  4. Compare before/after metrics"
echo ""
echo -e "${BLUE}To run different tests:${NC}"
echo "  REQUESTS=1000 CONCURRENCY=20 ./load-test.sh"
echo "  ENDPOINT=/leaderboard/top/NA?limit=50 ./load-test.sh"
echo ""
