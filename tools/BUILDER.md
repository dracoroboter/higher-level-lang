# hll — HLL Compiler Toolchain

Compila sorgenti `.hll` in JAR eseguibili tramite una pipeline a 4 step.

## Pipeline

```
.hll → [HLL compiler] → .java → [javac] → .class → [jar] → .jar
```

## Comandi

```bash
source env.sh              # aggiunge tools/ al PATH (una volta per sessione)

hll build app.hll          # compila → app.jar
hll build app.hll -o x.jar # output custom
hll run app.hll            # compila + esegui
hll run app.hll -- a b     # con argomenti al programma
hll check app.hll          # solo type-check, nessun output
hll emit app.hll           # stampa il Java generato su stdout
```

Senza subcomando assume `build` (backward-compat con `builderHll`).

## Opzioni

| Flag | Default | Descrizione |
|------|---------|-------------|
| `-o, --output` | `<nome>.jar` | Path del JAR di output |
| `--proto` | `p5b` (o `$HLL_PROTO`) | Prototipo compilatore da usare |
| `-h, --help` | — | Mostra l'help |

## Pre-compiled JAR (fast path)

Lo script cerca automaticamente `prototypes/<proto>/src/target/hll-compiler.jar`.
Se presente, lo usa direttamente (~0.6s). Altrimenti fa fallback a `mvn exec:java` (~5-10s).

### Come generare il JAR precompilato

```bash
cd prototypes/p5b/src
mvn package -q
```

Produce `target/hll-compiler.jar` (fat JAR con dipendenze incluse, ~557K).

### Quando rigenerarlo

- Dopo modifiche al compilatore HLL (grammar, type-checker, codegen)
- Dopo `mvn clean`

Il `target/` è in `.gitignore`, quindi ogni sviluppatore deve fare `mvn package` una volta dopo il clone.

## Variabili d'ambiente

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `JAVA_HOME` | `/usr/lib/jvm/java-21-openjdk-amd64` | JDK da usare |
| `HLL_PROTO` | `p5b` | Prototipo di default |

## Comportamento in caso di errore

| Step | Errore | Comportamento |
|------|--------|---------------|
| HLL compilation | Errori di tipo/sintassi | Mostra errori, exit 1 |
| javac | Java non valido | Mostra errori, **preserva sorgente** in `/tmp/hll-build-*` |
| jar | Packaging fallito | exit 1 |

## File

| Path | Descrizione |
|------|-------------|
| `tools/hll` | Script principale (nuovo) |
| `tools/builderHll` | Script legacy (mantenuto per backward-compat) |
| `env.sh` | Source per aggiungere `tools/` al PATH |
| `prototypes/<proto>/src/target/hll-compiler.jar` | Compiler precompilato |
