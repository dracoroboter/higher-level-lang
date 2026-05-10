#!/bin/bash
# Benchmark App Score — measures how well a prototype reproduces the Java reference behavior
# Usage: ./score_app.sh <prototype_dir>
#
# Scores:
#   - Compiles: does the HLL translation compile?
#   - Tests: how many HLL tests pass?
#   - Output: does the generated Java produce the same output as reference?

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

PROTO=$(realpath "$1")
PROTO_NAME=$(basename "$PROTO")
BENCHMARKAPP="$PROTO/benchmarkapp"
REFERENCE_DIR=$(realpath "$(dirname "$0")/../../benchmarkapp")
SRC="$PROTO/src"

if [ ! -d "$BENCHMARKAPP" ]; then
    echo "No benchmarkapp/ in $PROTO_NAME — skipping"
    exit 0
fi

echo "=== Benchmark App Score: $PROTO_NAME ==="
echo ""

# --- 1. Compilation ---
compiles=0
hll_file=$(find "$BENCHMARKAPP" -name "*.hll" | head -1)
if [ -z "$hll_file" ]; then
    echo "Compiles: NO (no .hll file found)"
else
    result=$(cd "$SRC" && mvn exec:java -Dexec.mainClass=dev.hll.Main -Dexec.args="$(realpath $hll_file) --check-only" -q 2>&1)
    if echo "$result" | grep -q "^OK:"; then
        compiles=1
        echo "Compiles: YES ✅"
    else
        echo "Compiles: NO ❌"
        echo "  $(echo "$result" | grep "^ERROR:" | head -3)"
    fi
fi

# --- 2. Tests ---
tests_pass=0
tests_total=0
if [ $compiles -eq 1 ]; then
    result=$(cd "$SRC" && mvn exec:java -Dexec.mainClass=dev.hll.Main -Dexec.args="$(realpath $hll_file) --test" -q 2>&1)
    tests_pass=$(echo "$result" | grep -c "^✅")
    tests_fail=$(echo "$result" | grep -c "^❌")
    tests_total=$((tests_pass + tests_fail))
    echo "Tests: $tests_pass/$tests_total"
else
    echo "Tests: N/A (doesn't compile)"
fi

# --- 3. Output comparison (if codegen works) ---
output_match="N/A"
if [ $compiles -eq 1 ]; then
    # Try to generate Java and run it
    generated=$(cd "$SRC" && mvn exec:java -Dexec.mainClass=dev.hll.Main -Dexec.args="$(realpath $hll_file)" -q 2>&1)
    if echo "$generated" | grep -q "^public class\|^import"; then
        echo "$generated" > /tmp/HllGenerated.java
        if javac /tmp/HllGenerated.java -d /tmp/hll_bench 2>/dev/null; then
            hll_output=$(java -cp /tmp/hll_bench HllGenerated 2>&1)
            ref_output=$(cd "$REFERENCE_DIR" && java OrderSystem 2>&1)
            if [ "$hll_output" = "$ref_output" ]; then
                output_match="IDENTICAL ✅"
            else
                output_match="DIFFERS ❌"
            fi
        else
            output_match="CODEGEN_COMPILE_FAIL"
        fi
        rm -rf /tmp/HllGenerated.java /tmp/hll_bench
    else
        output_match="NO_CODEGEN"
    fi
fi
echo "Output: $output_match"

# --- Summary ---
echo ""
echo "================================"
if [ $tests_total -eq 0 ]; then
    app_score=0
else
    app_score=$((tests_pass * 100 / tests_total))
fi
echo "APP SCORE: $app_score% ($tests_pass/$tests_total tests, compiles=$compiles, output=$output_match)"
echo "================================"
