#!/usr/bin/env bash
# Pool size sweep — restarts the Play server at each (play.pool, db.pool.maxSize) combination
# and records RPS for all benchmark endpoints.
# Usage: ./pool-sweep.sh
# Requires the server NOT to be running before starting.

set -euo pipefail

DURATION=15
CONCURRENCY=256
THREADS=4
BASE="http://localhost:9000"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$APP_DIR/../.." && pwd)"

POOL_SIZES=(11 32 64 128 256)   # play.pool values to test (11 = current default on 10-core)

ENDPOINTS=(
  "plaintext|$BASE/plaintext"
  "json|$BASE/json"
  "db|$BASE/db"
  "queries q=1|$BASE/queries?queries=1"
  "queries q=20|$BASE/queries?queries=20"
  "updates q=1|$BASE/updates?queries=1"
  "updates q=20|$BASE/updates?queries=20"
)

run_wrk() {
  local url="$1"
  local raw
  raw=$(wrk -t"$THREADS" -c"$CONCURRENCY" -d"${DURATION}s" --latency "$url" 2>&1)
  echo "$raw" | grep "Requests/sec:" | awk '{printf "%.0f", $2}'
}

wait_for_server() {
  for i in $(seq 1 40); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/plaintext" 2>/dev/null || true)
    [ "$code" = "200" ] && return
    sleep 2
  done
  echo "ERROR: server did not start" >&2; exit 1
}

kill_server() {
  pkill -f 'play.server.Server' 2>/dev/null || true
  sleep 2
}

patch_conf() {
  local pool="$1" dbpool="$2"
  # Write a temp conf that adds play.pool and db.pool.maxSize
  cat > "$APP_DIR/conf/application.conf" <<EOF
application.name=tfb
application.mode=prod
application.secret=8f3a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a
http.port=9000

play.pool=$pool

db=mem
db.pool.maxSize=$dbpool
db.pool.minSize=$dbpool
jpa.ddl=create
EOF
}

restore_conf() {
  cat > "$APP_DIR/conf/application.conf" <<EOF
application.name=tfb
application.mode=prod
application.secret=8f3a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a
http.port=9000

db=mem
jpa.ddl=create

# To benchmark against PostgreSQL, replace db=mem with:
# db.url=jdbc:postgresql://localhost:5432/hello_world
# db.driver=org.postgresql.Driver
# db.user=benchmarkdbuser
# db.pass=benchmarkdbpass
# db.pool.maxSize=256
# jpa.dialect=org.hibernate.dialect.PostgreSQLDialect
# jpa.ddl=none
EOF
}

# ── header ────────────────────────────────────────────────────────────────────

COL_W=12

printf "\n%-22s" "Endpoint"
for p in "${POOL_SIZES[@]}"; do
  printf "%${COL_W}s" "pool=$p"
done
printf "\n"
printf "%22s" ""
for p in "${POOL_SIZES[@]}"; do
  printf "%${COL_W}s" "(RPS)"
done
printf "\n"
printf '%s\n' "$(printf '─%.0s' $(seq 1 $((22 + COL_W * ${#POOL_SIZES[@]}))))"

# ── sweep ─────────────────────────────────────────────────────────────────────

declare -A RESULTS

kill_server  # ensure clean start

for pool in "${POOL_SIZES[@]}"; do
  dbpool=$pool
  patch_conf "$pool" "$dbpool"

  cd "$REPO_DIR" && nohup python3 play run samples-and-tests/tfb/ > /tmp/play-sweep-$pool.log 2>&1 &
  wait_for_server

  # warmup
  wrk -t"$THREADS" -c"$CONCURRENCY" -d8s "$BASE/plaintext" >/dev/null 2>&1
  wrk -t"$THREADS" -c"$CONCURRENCY" -d8s "$BASE/db"        >/dev/null 2>&1

  for ep in "${ENDPOINTS[@]}"; do
    name="${ep%%|*}"
    url="${ep##*|}"
    rps=$(run_wrk "$url")
    RESULTS["${name}__${pool}"]=$rps
  done

  kill_server
done

restore_conf

# ── print table ───────────────────────────────────────────────────────────────

for ep in "${ENDPOINTS[@]}"; do
  name="${ep%%|*}"
  printf "%-22s" "$name"
  for pool in "${POOL_SIZES[@]}"; do
    printf "%${COL_W}s" "${RESULTS[${name}__${pool}]}"
  done
  printf "\n"
done

printf '%s\n' "$(printf '─%.0s' $(seq 1 $((22 + COL_W * ${#POOL_SIZES[@]}))))"
printf "\nNote: db.pool.maxSize = play.pool for each column. ${DURATION}s × ${THREADS}t × ${CONCURRENCY}c\n\n"
