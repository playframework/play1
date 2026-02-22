#!/usr/bin/env bash
# TFB benchmark script — runs wrk against all five endpoints and prints a summary table.
# Usage: ./benchmark.sh [--profile] [duration_seconds] [concurrency]
# Defaults: 30s, 256 connections, 4 threads
#
# --profile  Attach async-profiler (asprof) to the running Play server during the
#            benchmark run and write a collapsed-stack CPU profile to /tmp/play-profile.txt

set -euo pipefail

PROFILE=false
POSITIONAL=()
for arg in "$@"; do
  case "$arg" in
    --profile) PROFILE=true ;;
    *) POSITIONAL+=("$arg") ;;
  esac
done

DURATION=${POSITIONAL[0]:-30}
CONCURRENCY=${POSITIONAL[1]:-256}
THREADS=4
BASE="http://localhost:9000"
PROFILE_OUT="/tmp/play-profile.txt"

# ── helpers ──────────────────────────────────────────────────────────────────

check_deps() {
  for cmd in wrk curl; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "ERROR: $cmd not found. Install wrk (brew install wrk)."; exit 1; }
  done
  if $PROFILE; then
    command -v asprof >/dev/null 2>&1 || { echo "ERROR: asprof not found. Download async-profiler and put asprof on PATH."; exit 1; }
  fi
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

find_server_pid() {
  local pid
  pid=$(pgrep -f 'play.server.Server' 2>/dev/null | head -1 || true)
  if [ -z "$pid" ]; then
    echo "ERROR: could not find Play server PID (no process matching play.server.Server)." >&2
    exit 1
  fi
  echo "$pid"
}

# Ensure libasyncProfiler.dylib is in the running JVM's lib dir so dynamic attach works.
install_dylib_for_pid() {
  local pid="$1"
  local asprof_dir
  asprof_dir="$(dirname "$(command -v asprof)")"
  local dylib="$asprof_dir/libasyncProfiler.dylib"
  if [ ! -f "$dylib" ]; then
    echo "ERROR: libasyncProfiler.dylib not found next to asprof ($asprof_dir). Cannot profile." >&2
    exit 1
  fi
  # Resolve the JVM's java.home for this PID
  local java_bin
  java_bin=$(ps -p "$pid" -o command= | awk '{print $1}')
  local java_home
  java_home=$("$java_bin" -XshowSettings:all -version 2>&1 | awk -F'= ' '/java\.home/{print $2; exit}')
  local dest="$java_home/lib/libasyncProfiler.dylib"
  if [ ! -f "$dest" ]; then
    echo "Installing libasyncProfiler.dylib → $dest"
    cp "$dylib" "$dest"
  fi
}

profiler_start() {
  local pid="$1"
  install_dylib_for_pid "$pid"
  echo "Starting async-profiler on PID $pid ..."
  asprof start -e cpu "$pid" 2>&1 || { echo "ERROR: asprof start failed."; exit 1; }
  echo "Profiler running."
}

profiler_stop() {
  local pid="$1"
  echo ""
  echo "Stopping profiler and writing collapsed stacks to $PROFILE_OUT ..."
  asprof stop -o collapsed -f "$PROFILE_OUT" "$pid"
  echo "Profile written: $PROFILE_OUT"
  echo "  $(wc -l < "$PROFILE_OUT") stack traces captured."
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

SERVER_PID=""
if $PROFILE; then
  SERVER_PID=$(find_server_pid)
  profiler_start "$SERVER_PID"
fi

echo ""
echo "Running benchmarks: ${DURATION}s × ${THREADS} threads × ${CONCURRENCY} connections"
if $PROFILE; then echo "  (profiling PID $SERVER_PID)"; fi
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

if $PROFILE; then
  profiler_stop "$SERVER_PID"
fi

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
