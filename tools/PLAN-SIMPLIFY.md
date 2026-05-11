# Piano: Semplificazione builderHll

## Problemi attuali

1. **Invocazione verbosa**: bisogna specificare il path completo del sorgente e ricordarsi le opzioni
2. **Maven overhead**: ogni build invoca Maven che risolve dipendenze (~5-10s la prima volta)
3. **Nessun feedback rapido**: non c'è un modo per fare solo type-check senza generare il JAR
4. **Non è nel PATH**: bisogna usare `./tools/builderHll`

## Miglioramenti proposti

### Fase 1 — Quick wins (effort: basso)

**1.1 Aggiungere `tools/` al PATH del progetto**

Creare un file `env.sh` da sourceare:
```bash
# source env.sh
export PATH="$PWD/tools:$PATH"
export HLL_PROTO=p5b
```

Dopo: `builderHll app.hll` da qualsiasi punto del progetto.

**1.2 Aggiungere modalità `--check-only`**

Solo type-check + codegen, senza javac/jar. Utile per sviluppo iterativo:
```bash
builderHll app.hll --check-only   # stampa errori HLL, exit 0/1
```

**1.3 Aggiungere modalità `--emit-java`**

Mostra il Java generato su stdout (utile per debug):
```bash
builderHll app.hll --emit-java > Generated.java
```

### Fase 2 — Velocità (effort: medio)

**2.1 Pre-compilare il compilatore HLL**

Invece di invocare Maven ogni volta, fare un `mvn package` una tantum e poi invocare il JAR direttamente:
```bash
# Setup (una volta)
cd prototypes/p5b/src && mvn package -q

# Build (ogni volta, ~1s invece di ~5s)
java -jar prototypes/p5b/src/target/hll-compiler.jar app.hll
```

Il builder può fare auto-detect: se il JAR esiste lo usa, altrimenti fallback a `mvn exec:java`.

**2.2 Aggiungere `--watch` mode**

Ricompila automaticamente quando il file cambia:
```bash
builderHll app.hll --watch   # ricompila ad ogni salvataggio
```

Implementabile con `inotifywait` (inotify-tools).

### Fase 3 — Ergonomia (effort: medio)

**3.1 Comando `hll run`**

Compila ed esegue in un solo comando:
```bash
builderHll run app.hll              # compila + java -jar
builderHll run app.hll -- arg1 arg2 # con argomenti
```

**3.2 Comando `hll init`**

Crea un progetto HLL minimo:
```bash
builderHll init myproject
# → myproject/
#     main.hll
#     .hllconfig   (proto, java version, etc.)
```

**3.3 File `.hllconfig` per progetto**

Evita di ripetere le opzioni:
```ini
proto=p5b
output=build/app.jar
java_home=/usr/lib/jvm/java-21-openjdk-amd64
```

### Fase 4 — Rinominare (effort: basso, impatto alto)

Rinominare `builderHll` → `hll`. Più corto, più idiomatico:
```bash
hll build app.hll          # compila
hll run app.hll            # compila + esegui
hll check app.hll          # solo type-check
hll emit app.hll           # mostra Java generato
```

## Priorità suggerita

| # | Cosa | Impatto | Effort |
|---|------|---------|--------|
| 1 | Rinominare → `hll` con subcomandi | Alto | Basso |
| 2 | `--check-only` e `--emit-java` | Medio | Basso |
| 3 | Pre-compilare il compiler JAR | Alto | Medio |
| 4 | `env.sh` per PATH | Basso | Minimo |
| 5 | `hll run` | Medio | Basso |
| 6 | `--watch` | Medio | Medio |
| 7 | `.hllconfig` + `hll init` | Basso | Medio |

## Primo step concreto

Rinominare `builderHll` → `hll` con subcomandi (`build`, `run`, `check`, `emit`), mantenendo backward-compatibility (se invocato senza subcomando, assume `build`).
