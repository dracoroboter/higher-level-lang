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

# Find matching Java benchmark
for f in "$JAVA_BENCH_DIR"/*.java; do
    [ -f "$f" ] || continue
    loc=$(grep -cv '^\s*$\|^\s*//' "$f" 2>/dev/null || echo 0)
    if [ $loc -gt $java_loc ]; then java_loc=$loc; fi
done

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
for f in "$JAVA_BENCH_DIR"/*.java; do
    [ -f "$f" ] || continue
    cc=$(grep -c "if\|while\|for\|switch\|?" "$f" 2>/dev/null || echo 0)
    java_cc=$((java_cc + cc))
done
echo ""
echo "Cyclomatic complexity: HLL=$hll_cc, Java=$java_cc"

# --- Antipatterns blocked ---
antipattern=$((invalid_ok * 100 / 46))
echo ""
echo "Antipatterns: $antipattern (${invalid_ok}/46)"

# --- Patterns included ---
pattern=$((valid_ok * 100 / 47))
echo ""
echo "Patterns: $pattern (${valid_ok}/47)"

# --- Final score ---
# Conciseness includes cyclomatic complexity bonus
if [ $java_cc -gt 0 ] && [ $hll_cc -lt $java_cc ]; then
    cc_bonus=$(( (java_cc - hll_cc) * 100 / java_cc ))
    conciseness_with_cc=$(( (conciseness + cc_bonus) / 2 ))
else
    conciseness_with_cc=$conciseness
fi

score=$(( correctness * 30 / 100 + conciseness_with_cc * 25 / 100 + antipattern * 25 / 100 + pattern * 25 / 100 ))
echo ""
echo "================================"
echo "SCORE: $score / 100"
echo "================================"
echo "  Correctness (30%): $correctness"
echo "  Conciseness (25%): $conciseness_with_cc (LOC=$conciseness, CC bonus=$cc_bonus)"
echo "  Antipatterns (25%): $antipattern"
echo "  Patterns (25%):     $pattern"
