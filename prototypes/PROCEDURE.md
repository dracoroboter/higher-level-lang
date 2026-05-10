# Procedura: creare, testare e valutare un prototipo

## 1. Creare un nuovo prototipo

```bash
# 1. Creare la directory
mkdir -p prototypes/pXy/{grammar,src,tests/valid,tests/invalid,benchmark,output}

# 2. Scrivere INTENT.md (PRIMA del codice)
#    - Deriva da: quali padri
#    - Ipotesi generale: cosa cambia
#    - Problemi risolti: tabella antipattern → meccanismo
#    - Trade-off accettati

# 3. Scrivere LANGUAGE.md
#    - Keyword nuove
#    - Esempi di sintassi

# 4. Copiare il codice del padre come base (se pragmatico)
cp -r prototypes/pXparent/src/* prototypes/pXy/src/

# 5. Modificare grammatica, AST, type checker, codegen

# 6. Scrivere i test (PRIMA di implementare — TDD)
#    - Un test invalid per ogni antipattern dichiarato nell'INTENT
#    - Un test valid per ogni feature dichiarata

# 7. Implementare fino a che i test passano
```

## 2. Testare un prototipo

```bash
cd prototypes/pXy/src
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH

# Rigenerare parser (se grammatica modificata)
java -jar target/antlr4-4.13.1-complete.jar -visitor -no-listener \
    -package dev.hll.parser \
    -o target/generated-sources/antlr4/dev/hll/parser \
    -Xexact-output-dir src/main/antlr4/Hll.g4

# Compilare
rm -rf target/classes && mvn compile -q

# Test valid (devono tutti dare "OK")
for f in ../tests/valid/*.hll; do
    echo -n "$(basename $f): "
    mvn exec:java -Dexec.mainClass=dev.hll.Main \
        -Dexec.args="$f --check-only" -q 2>&1 | grep -o "OK\|ERROR"
done

# Test invalid (devono tutti dare "ERROR" — non NPE/crash!)
for f in ../tests/invalid/*.hll; do
    result=$(mvn exec:java -Dexec.mainClass=dev.hll.Main \
        -Dexec.args="$f --check-only" -q 2>&1)
    if echo "$result" | grep -q "^ERROR:"; then
        echo "✅ $(basename $f): $(echo "$result" | grep "^ERROR:" | head -1)"
    elif echo "$result" | grep -q "NullPointer\|Exception"; then
        echo "💀 $(basename $f): CRASH (bug nel compilatore)"
    elif echo "$result" | grep -q "WARNING"; then
        echo "⚠️ $(basename $f): warning only"
    else
        echo "❌ $(basename $f): accepted (should be rejected)"
    fi
done

# Benchmark
for f in ../benchmark/*.hll; do
    echo -n "$(basename $f): "
    mvn exec:java -Dexec.mainClass=dev.hll.Main \
        -Dexec.args="$f --check-only" -q 2>&1 | grep -o "OK\|ERROR"
done
```

## 2b. Verificare codegen (output identico a Java)

Se il prototipo ha codegen funzionante:

```bash
cd prototypes/pXy/src

# Generare Java dal benchmark
mvn exec:java -Dexec.mainClass=dev.hll.Main \
    -Dexec.args="../benchmark/benchmark_exec.hll" -q > /tmp/Generated.java

# Compilare ed eseguire il Java generato
javac /tmp/Generated.java -d /tmp
java -cp /tmp Generated > /tmp/hll_output.txt

# Confrontare con output di riferimento
diff /tmp/hll_output.txt ../benchmark/expected_output.txt
# Se diff è vuoto → codegen corretto
```

**Regola:** il codegen è valido solo se l'output del Java generato è identico all'output di riferimento. Differenze di whitespace sono accettabili (usare `diff -b`).

## 3. Calcolare lo score

```
SCORE = (correttezza × 0.3) + (snellezza × 0.25) + (antipattern × 0.25) + (pattern × 0.2)
```

### Correttezza
```
valid_ok = numero test valid che danno "OK"
valid_total = numero totale test valid
invalid_rejected = numero test invalid che danno "ERROR:" (non crash!)
invalid_total = numero totale test invalid

correttezza = ((valid_ok/valid_total) × 50) + ((invalid_rejected/invalid_total) × 50)
```

### Snellezza
```
loc_hll = righe non vuote/commento del benchmark HLL
loc_java = righe non vuote/commento del benchmark Java equivalente

snellezza = max(0, 100 - (loc_hll / loc_java × 100))
```

### Antipattern bloccati
```
antipattern = test_invalid_rejected / 46 × 100

(denominatore fisso = 46 antipattern dal database)
(non testato = 0 punti, crash = -1 punto)
```

### Pattern inclusi
```
pattern = test_valid_naturali / 47 × 100

(denominatore fisso = 47 pattern dal database)
```

## 4. Verificare corrispondenza INTENT ↔ Implementazione

Per ogni riga nella tabella "Problemi risolti" dell'INTENT:
1. Esiste un test invalid che lo verifica? Se no → scriverlo
2. Il test viene rifiutato con un messaggio che indica il motivo giusto? Se no → bug
3. Il test non crasha (NPE)? Se crasha → bug grave

**Regola:** un antipattern dichiarato nell'INTENT ma non verificato da un test = 0 punti.
Un antipattern dichiarato e verificato ma che crasha il compilatore = -1 punto.

## 5. Aggiornare LINEAGE.md e RESULTS.md

Dopo ogni sessione di test, aggiornare:
- `prototypes/LINEAGE.md` — stato e score
- `tools/compare/RESULTS.md` — dettaglio calcoli
