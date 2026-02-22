#!/usr/bin/env bash
# TFB benchmark script — runs wrk against all five endpoints and prints a summary table.
# Usage: ./benchmark.sh [duration_seconds] [concurrency]
# Defaults: 30s, 256 connections, 4 threads

set -euo pipefail

DURATION=${1:-30}
CONCURRENCY=${2:-256}
THREADS=4
BASE="http://localhost:9000"

# ── helpers ──────────────────────────────────────────────────────────────────

check_deps() {
  for cmd in wrk curl; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "ERROR: $cmd not found. Install wrk (brew install wrk)."; exit 1; }
  done
}

wait_for_server() {
  echo "Waiting for server at $BASE ..."
  for i in $(seq 1 30); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/plaintext" 2>/dev/null || true)
    [ "$code" = "200" ] && echo "Server ready." && return
    sleep 2
  done
  echo "ERROR: server did not become ready after 60s. Is 'play run samples-and-tests/tfb/' running?"
  exit 1
}

# Run wrk and extract RPS, avg latency, p50, p99, errors
run_wrk() {
  local url="$1"
  local raw
  raw=$(wrk -t"$THREADS" -c"$CONCURRENCY" -d"${DURATION}s" --latency "$url" 2>&1)

  local rps lat_avg p50 p99 errors
  rps=$(echo     "$raw" | grep "Requests/sec:" | awk '{printf "%.0f", $2}')
  lat_avg=$(echo "$raw" | grep "Latency " | grep -v "Distribution" | awk '{print $2}')
  p50=$(echo     "$raw" | grep -E "^[[:space:]]+50%" | awk '{print $2}')
  p99=$(echo     "$raw" | grep -E "^[[:space:]]+99%" | awk '{print $2}')
  errors=$(echo  "$raw" | grep -E "^  (Socket errors|Non-2xx)" | awk '{sum+=$NF} END {print (sum>0)?sum:0}')
  errors=${errors:-0}

  echo "$rps|$lat_avg|$p50|$p99|$errors"
}

print_row() {
  printf "| %-26s | %8s | %9s | %6s | %6s | %8s |\n" "$1" "$2" "$3" "$4" "$5" "$6"
}

print_divider() {
  printf "|%s|%s|%s|%s|%s|%s|\n" \
    "----------------------------" "----------" "-----------" "--------" "--------" "----------"
}

# ── main ─────────────────────────────────────────────────────────────────────

check_deps

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRAMEWORK_DIR="$(cd "$SCRIPT_DIR/../../framework" && pwd)"

echo "Building framework JAR ..."
(cd "$FRAMEWORK_DIR" && ant jar -q) || { echo "ERROR: ant jar failed."; exit 1; }
echo "Build done."

wait_for_server

echo ""
echo "Warming up (10s each: plaintext, json, db) ..."
wrk -t"$THREADS" -c"$CONCURRENCY" -d10s "$BASE/plaintext" >/dev/null 2>&1
wrk -t"$THREADS" -c"$CONCURRENCY" -d10s "$BASE/json"      >/dev/null 2>&1
wrk -t"$THREADS" -c"$CONCURRENCY" -d10s "$BASE/db"        >/dev/null 2>&1
echo "Warmup done."
echo ""
echo "Running benchmarks: ${DURATION}s × ${THREADS} threads × ${CONCURRENCY} connections"
echo ""

# Collect results
declare -A RPS LAT_AVG P50 P99 ERRORS

run_endpoint() {
  local name="$1" url="$2"
  local result
  result=$(run_wrk "$url")
  IFS='|' read -r rps avg p50 p99 err <<< "$result"
  RPS[$name]=$rps
  LAT_AVG[$name]=$avg
  P50[$name]=$p50
  P99[$name]=$p99
  ERRORS[$name]=$err
  printf "  %-26s  %s req/s\n" "$name" "$rps"
}

run_endpoint "plaintext"          "$BASE/plaintext"
run_endpoint "json"               "$BASE/json"
run_endpoint "db"                 "$BASE/db"
run_endpoint "queries q=1"        "$BASE/queries?queries=1"
run_endpoint "queries q=20"       "$BASE/queries?queries=20"
run_endpoint "queries q=500"      "$BASE/queries?queries=500"
run_endpoint "updates q=1"        "$BASE/updates?queries=1"
run_endpoint "updates q=20"       "$BASE/updates?queries=20"

# ── Summary table ─────────────────────────────────────────────────────────────

echo ""
echo "=================================================================="
echo " TFB Benchmark Results"
echo " $(date '+%Y-%m-%d %H:%M:%S')  |  JVM: $(java -version 2>&1 | head -1)"
echo " ${DURATION}s run  |  ${THREADS} threads  |  ${CONCURRENCY} connections"
echo "=================================================================="
echo ""
printf "| %-26s | %8s | %9s | %6s | %6s | %8s |\n" \
  "Endpoint" "RPS" "Avg lat" "p50" "p99" "Errors"
print_divider
for name in "plaintext" "json" "db" "queries q=1" "queries q=20" "queries q=500" "updates q=1" "updates q=20"; do
  print_row "$name" "${RPS[$name]}" "${LAT_AVG[$name]}" "${P50[$name]}" "${P99[$name]}" "${ERRORS[$name]}"
done
print_divider
echo ""
echo "Note: errors = socket errors + non-2xx responses reported by wrk."
echo "      Variable-length JSON responses do NOT inflate this count (unlike ab)."
echo ""
