#!/bin/bash
# Score calculator for HLL prototypes
# Usage: ./score.sh <prototype_dir>

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

PROTO=$(realpath "$1")
if [ -z "$1" ]; then
    echo "Usage: ./score.sh <prototype_dir>"
    echo "Example: ./score.sh ../../prototypes/p2a"
    exit 1
fi

PROTO_NAME=$(basename "$PROTO")
SRC="$PROTO/src"
BENCHMARK_DIR="$PROTO/benchmark"
INVALID_DIR="$PROTO/tests/invalid"
SCRIPT_DIR=$(dirname "$(realpath "$0")")
JAVA_BENCH_DIR="$SCRIPT_DIR/benchmark"

echo "=== Scoring $PROTO_NAME ==="
echo ""

# --- Correctness ---
valid_ok=0; valid_total=0
for f in "$BENCHMARK_DIR"/*.hll; do
    [ -f "$f" ] || continue
    valid_total=$((valid_total+1))
    result=$(cd "$SRC" && mvn exec:java -Dexec.mainClass=dev.hll.Main -Dexec.args="$(realpath $f) --check-only" -q 2>&1)
    if echo "$result" | grep -q "OK"; then valid_ok=$((valid_ok+1)); fi
done

invalid_ok=0; invalid_total=0
# Use only the prototype's own tests (each prototype has its own + inherited tests in its own syntax)
for f in "$INVALID_DIR"/*.hll; do
    [ -f "$f" ] || continue
    invalid_total=$((invalid_total+1))
    result=$(cd "$SRC" && mvn exec:java -Dexec.mainClass=dev.hll.Main -Dexec.args="$(realpath $f) --check-only --strict" -q 2>&1)
    if echo "$result" | grep -q "^ERROR:\|Parse error\|Compilation failed"; then invalid_ok=$((invalid_ok+1)); fi
done

if [ $valid_total -eq 0 ]; then valid_pct=0; else valid_pct=$((valid_ok * 100 / valid_total)); fi
if [ $invalid_total -eq 0 ]; then invalid_pct=0; else invalid_pct=$((invalid_ok * 100 / invalid_total)); fi
correctness=$(( (valid_pct * 50 + invalid_pct * 50) / 100 ))

echo "Correctness: $correctness"
echo "  Valid: $valid_ok/$valid_total ($valid_pct%)"
echo "  Invalid rejected: $invalid_ok/$invalid_total ($invalid_pct%)"

# --- Conciseness ---
# Find the main benchmark .hll and corresponding Java
hll_loc=0; java_loc=0
for f in "$BENCHMARK_DIR"/*.hll; do
    [ -f "$f" ] || continue
    loc=$(grep -cv '^\s*$\|^\s*//' "$f" 2>/dev/null || echo 0)
    if [ $loc -gt $hll_loc ]; then hll_loc=$loc; fi
done

# Find matching Java benchmark: first check prototype's own dir, then shared
for f in "$BENCHMARK_DIR"/*.java; do
    [ -f "$f" ] || continue
    loc=$(grep -cv '^\s*$\|^\s*//' "$f" 2>/dev/null || echo 0)
    if [ $loc -gt $java_loc ]; then java_loc=$loc; fi
done
# If no Java in prototype dir, use shared benchmark matching the level
if [ $java_loc -eq 0 ]; then
    # Determine level from prototype name (p2x→L2, p3x→L3, p4x→L4)
    level=$(echo "$PROTO_NAME" | grep -oP 'p\K[0-9]')
    java_file="$JAVA_BENCH_DIR/BenchmarkL${level}.java"
    if [ -f "$java_file" ]; then
        java_loc=$(grep -cv '^\s*$\|^\s*//' "$java_file" 2>/dev/null || echo 0)
    else
        # Fallback: largest Java file in shared dir
        for f in "$JAVA_BENCH_DIR"/*.java; do
            [ -f "$f" ] || continue
            loc=$(grep -cv '^\s*$\|^\s*//' "$f" 2>/dev/null || echo 0)
            if [ $loc -gt $java_loc ]; then java_loc=$loc; fi
        done
    fi
fi

if [ $java_loc -eq 0 ]; then conciseness=0
else conciseness=$((100 - (hll_loc * 100 / java_loc))); fi
if [ $conciseness -lt 0 ]; then conciseness=0; fi

echo ""
echo "Conciseness: $conciseness"
echo "  HLL: $hll_loc LOC, Java: $java_loc LOC"

# --- Cyclomatic complexity ---
hll_cc=0; java_cc=0
for f in "$BENCHMARK_DIR"/*.hll; do
    [ -f "$f" ] || continue
    cc=$(grep -c "match\|if\|while\|for" "$f" 2>/dev/null || echo 0)
    hll_cc=$((hll_cc + cc))
done
# Use same Java file as conciseness comparison
java_cc_file=""
for f in "$BENCHMARK_DIR"/*.java; do
    [ -f "$f" ] || continue
    java_cc_file="$f"
done
if [ -z "$java_cc_file" ]; then
    level=$(echo "$PROTO_NAME" | grep -oP 'p\K[0-9]')
    java_cc_file="$JAVA_BENCH_DIR/BenchmarkL${level}.java"
    [ -f "$java_cc_file" ] || java_cc_file=""
fi
if [ -n "$java_cc_file" ] && [ -f "$java_cc_file" ]; then
    java_cc=$(grep -c "if\|while\|for\|switch\|\?" "$java_cc_file" 2>/dev/null || echo 0)
fi
echo ""
echo "Cyclomatic complexity: HLL=$hll_cc, Java=$java_cc"

# --- Antipatterns blocked ---
# Count blocked-by-design (from BLOCKED_BY_DESIGN.md if exists)
blocked_by_design=0
if [ -f "$PROTO/BLOCKED_BY_DESIGN.md" ]; then
    blocked_by_design=$(grep -c "^| [0-9]" "$PROTO/BLOCKED_BY_DESIGN.md" 2>/dev/null || echo 0)
fi
total_blocked=$((invalid_ok + blocked_by_design))
antipattern=$((total_blocked * 100 / 46))
echo ""
echo "Antipatterns: $antipattern (${total_blocked}/46 = ${invalid_ok} tested + ${blocked_by_design} by design)"

# --- Patterns included ---
pattern=$((valid_ok * 100 / 47))
echo ""
echo "Patterns: $pattern (${valid_ok}/47)"

# --- Final score ---
# Weights (configurable)
W_CORRECTNESS=${W_CORRECTNESS:-30}
W_CONCISENESS=${W_CONCISENESS:-25}
W_ANTIPATTERNS=${W_ANTIPATTERNS:-25}
W_PATTERNS=${W_PATTERNS:-20}

# Conciseness includes cyclomatic complexity bonus
cc_bonus=0
if [ $java_cc -gt 0 ] && [ $hll_cc -lt $java_cc ]; then
    cc_bonus=$(( (java_cc - hll_cc) * 100 / java_cc ))
    conciseness_with_cc=$(( (conciseness + cc_bonus) / 2 ))
else
    conciseness_with_cc=$conciseness
fi

score=$(( correctness * W_CORRECTNESS / 100 + conciseness_with_cc * W_CONCISENESS / 100 + antipattern * W_ANTIPATTERNS / 100 + pattern * W_PATTERNS / 100 ))
echo ""
echo "================================"
echo "SCORE: $score / 100"
echo "================================"
echo "  Correctness ($W_CORRECTNESS%): $correctness"
echo "  Conciseness ($W_CONCISENESS%): $conciseness_with_cc (LOC=$conciseness, CC bonus=$cc_bonus)"
echo "  Antipatterns ($W_ANTIPATTERNS%): $antipattern"
echo "  Patterns ($W_PATTERNS%):     $pattern"
echo ""
echo "--- Raw values ---"
echo "  valid_ok=$valid_ok valid_total=$valid_total"
echo "  invalid_ok=$invalid_ok invalid_total=$invalid_total"
echo "  hll_loc=$hll_loc java_loc=$java_loc"
echo "  hll_cc=$hll_cc java_cc=$java_cc"
echo ""
echo "--- Weights (override with env vars) ---"
echo "  W_CORRECTNESS=$W_CORRECTNESS W_CONCISENESS=$W_CONCISENESS W_ANTIPATTERNS=$W_ANTIPATTERNS W_PATTERNS=$W_PATTERNS"
