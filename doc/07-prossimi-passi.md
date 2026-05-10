# Prossimi passi: gestione eccezioni e framework di confronto

## Stato attuale

- **hll-p1 "null train"**: funzionante, dimostra Option + tipi nominali + Demeter
- **Prossimo tema**: gestione delle eccezioni e tipi di errore

## Framework di confronto tra prototipi

### Metriche automatiche

| Metrica | Cosa misura | Come si calcola |
|---|---|---|
| **Correttezza** | I test passano? | `tests/valid/` compilano, `tests/invalid/` sono rifiutati |
| **Snellezza** | Quanto codice serve? | LOC del benchmark in HLL vs Java equivalente |
| **Boilerplate** | Quante annotazioni/tipi extra? | Conteggio annotazioni di tipo obbligatorie |
| **Antipattern bloccati** | Quanti errori cattura a compile-time? | Numero di `tests/invalid/` rifiutati |
| **Pattern inclusi** | Quanti pattern sono nativi? | Checklist di pattern esprimibili senza boilerplate |
| **Leggibilità** | Un estraneo capisce il codice? | LOC + profondità nesting + keyword count |

### Benchmark condiviso

Tutti i prototipi devono compilare lo **stesso programma benchmark** — così il confronto è oggettivo.

Il benchmark per il tema "eccezioni" sarà:
- Un servizio che legge un file di configurazione (può fallire: file non trovato, formato errato)
- Parsa il contenuto (può fallire: parse error)
- Si connette a un DB (può fallire: connection refused, timeout)
- Esegue una query (può fallire: query error)
- Restituisce un risultato o un errore strutturato

Questo benchmark ha **4 punti di fallimento** con errori diversi — perfetto per testare la gestione eccezioni.

---

## Tema: gestione eccezioni

### Il problema

In Java:
```java
try {
    Config cfg = readConfig(path);        // IOException
    Connection conn = connect(cfg);        // SQLException
    Result r = conn.query("SELECT ...");   // SQLException
    return r.getString("name");            // può essere null!
} catch (Exception e) {
    // catch generico — antipattern
    log.error("something failed", e);
    return null;  // altro antipattern
}
```

Antipattern presenti: catch generico, return null, errori non tipizzati, nessuna distinzione tra errori recuperabili e fatali.

### Ipotesi in competizione

#### hll-p2a "result chain" — Result senza scappatoie (stile Rust)

**Ipotesi:** ogni funzione che può fallire dichiara i suoi errori nel tipo di ritorno. Nessun `try/catch`, nessun `throw`. Solo `Result<T, E>` e propagazione con `?`.

```hll
fn load_user(path: FilePath, id: UserId) -> Result<UserName, AppError> {
    let cfg = read_config(path)?           // propaga ConfigError
    let conn = connect(cfg)?               // propaga DBError
    let row = conn.query("SELECT...")?     // propaga QueryError
    Ok(row.get("name"))
}
```

**Pro:** esplicito, nessun errore nascosto, composizione chiara
**Contro:** verboso se molti errori diversi, richiede union types per AppError

#### hll-p2b "effect" — Effetti dichiarati (stile algebraic effects)

**Ipotesi:** le funzioni dichiarano quali *effetti* possono avere (fallire, loggare, fare IO). Il chiamante decide come gestirli.

```hll
fn load_user(path: FilePath, id: UserId) -> UserName
    effects { IOError, DBError, ParseError } {
    let cfg = read_config(path)
    let conn = connect(cfg)
    conn.query("SELECT...").get("name")
}

// Il chiamante gestisce gli effetti:
let name = handle load_user(path, id) {
    IOError(e) => default_name
    DBError(e) => retry(3) { load_user(path, id) }
    ParseError(e) => abort("bad config")
}
```

**Pro:** separazione tra "cosa può fallire" e "come gestisco il fallimento", più flessibile
**Contro:** concetto meno familiare, più complesso da implementare

#### hll-p2c "checked simple" — Eccezioni checked semplificate

**Ipotesi:** come Java checked exceptions ma senza i problemi. Niente `throws Exception` generico, niente catch vuoto, errori tipizzati e obbligatori.

```hll
fn read_config(path: FilePath) -> Config fails IOError {
    ...
}

fn load_user(path: FilePath) -> UserName fails IOError, DBError {
    let cfg = read_config(path)    // IOError propagato automaticamente (dichiarato)
    let conn = connect(cfg)        // DBError propagato
    conn.query("SELECT...").get("name")
}

// Il chiamante DEVE gestire:
let name = load_user(path, id)
    | IOError(e) => "default"
    | DBError(e) => "offline"
```

**Pro:** familiare per programmatori Java, errori visibili nella firma
**Contro:** rischio di diventare verboso come Java checked exceptions

---

## Piano di implementazione

### Fase 1: Definire il benchmark condiviso

Un singolo file `benchmark/error_handling.hll` scritto in 3 varianti (una per prototipo).
Più un equivalente Java `benchmark/ErrorHandling.java` per confronto.

### Fase 2: Implementare i 3 prototipi

| Prototipo | Nickname | Ipotesi | Meccanismo |
|---|---|---|---|
| hll-p2a | "result chain" | Errori nel tipo di ritorno | `Result<T, E>` + `?` |
| hll-p2b | "effect" | Effetti dichiarati | `effects { }` + `handle` |
| hll-p2c | "checked simple" | Eccezioni checked migliorate | `fails E` + `| E =>` |

Ogni prototipo:
- Estende hll-p1 (mantiene Option, tipi nominali, Demeter)
- Aggiunge il suo meccanismo di gestione errori
- Compila lo stesso benchmark
- Ha la stessa test suite di programmi errati

### Fase 3: Confronto automatico

```
tools/compare/
├── benchmark/
│   ├── error_handling_p2a.hll
│   ├── error_handling_p2b.hll
│   ├── error_handling_p2c.hll
│   └── ErrorHandling.java        ← equivalente Java per confronto LOC
├── compare.sh                     ← esegue test + calcola metriche
└── results.md                     ← output del confronto
```

### Fase 4: Migliorare l'efficienza del ciclo di test

Attualmente il ciclo è lento (Maven + exec:java per ogni file). Miglioramenti:
1. **Build una volta, test molti:** creare un jar eseguibile, poi `java -jar hll.jar file.hll`
2. **Script di test batch:** un singolo script che esegue tutti i test e produce un report
3. **Watch mode (futuro):** ricompila automaticamente quando un .hll cambia

---

## Ereditarietà tra prototipi

I prototipi formano un **grafo di derivazione funzionale** — non di codice. Ogni prototipo figlio deve **risolvere gli stessi problemi** del padre (e di più), ma può farlo con implementazione completamente diversa.

```
hll-p1 "null train" (risolve: NPE, tipi sbagliati, Demeter)
 ├── hll-p2a "result chain" (risolve: tutto p1 + eccezioni ignorate)
 ├── hll-p2b "effect" (risolve: tutto p1 + eccezioni ignorate)
 └── hll-p2c "checked simple" (risolve: tutto p1 + eccezioni ignorate)
```

**Cosa si eredita:** la **capacità di risolvere certi problemi**, non il codice.

**Cosa NON si eredita necessariamente:** l'implementazione. Un prototipo figlio può partire dal codice del padre (probabilmente più semplice e veloce) oppure reimplementare da zero se l'ipotesi generale lo richiede. La scelta è pragmatica, non dogmatica.

**Ipotesi dichiarate:** ogni prototipo dichiara esplicitamente:
1. Quali problemi risolve (lista di antipattern bloccati)
2. Quali ipotesi generali implementative adotta (es. "errori nel tipo di ritorno" vs "effetti dichiarati")
3. Quali trade-off accetta (es. "più verboso ma più esplicito")

**Confronto:** il benchmark verifica che tutti i prototipi allo stesso livello risolvano gli stessi problemi. La differenza è *come* li risolvono — e il confronto misura ergonomia, snellezza, leggibilità.

**Convergenza:** il vincitore di ogni livello definisce le ipotesi per il livello successivo. Se "result chain" vince, il livello 3 parte dall'ipotesi "errori nel tipo di ritorno" come dato acquisito.

---

## Fase futura: validazione con codice Java reale

**Obiettivo:** tradurre un frammento di codice Java reale (da un progetto vero) in HLL per verificare:
1. La traduzione è **possibile** (il linguaggio è espressivo abbastanza)
2. La traduzione **mantiene la funzionalità** (stesso comportamento)
3. Dove il linguaggio **si blocca** (limiti concreti)

**Cosa ci aspettiamo di scoprire:**
- Le dipendenze da librerie esterne saranno il blocco principale (Spring, Hibernate, etc.)
- Alcuni pattern Java non avranno equivalente diretto
- L'interop Java (import wrappato) avrà limiti pratici

**Scopo:** non è dimostrare che HLL può sostituire Java — è scoprire i **limiti** del linguaggio e la **potenziale applicabilità pratica**. Sapere dove si rompe è più utile che sapere dove funziona.

**Quando farlo:** quando almeno un prototipo di livello 3+ è stabile e il benchmark sintetico non rivela più informazioni nuove.

---

## Supporto imperativo (aggiunto 2026-05-10)

p2a supporta ora costrutti imperativi:
- `let mut x = value` — variabili mutabili
- `x = new_value` — riassegnamento
- `while condition { body }` — loop
- `return expr` — return esplicito

Testato con funzioni matematiche pure: fibonacci, factorial, is_prime, gcd. Il parser e type checker accettano il codice. Il codegen non traduce ancora `while` correttamente (placeholder).

Questo dimostra che il linguaggio è **multi-paradigma**: supporta sia lo stile funzionale (match, Option, Result) che lo stile imperativo (loop, mutabilità locale).

---

## Decisione richiesta

Prima di implementare, serve scegliere:
1. **Implementare tutti e 3?** (più lavoro, confronto migliore)
2. **Implementarne 2 e scartare il più debole sulla carta?** (più veloce)
3. **Quale ordine?** (suggerisco: p2a prima perché più semplice da implementare, poi p2c, poi p2b)

## Antipattern bloccati per prototipo (previsione)

| Antipattern | p2a | p2b | p2c |
|---|---|---|---|
| Exception swallowing (catch vuoto) | ✅ | ✅ | ✅ |
| Unchecked exceptions | ✅ | ✅ | ✅ |
| throws Exception generico | ✅ | ✅ | ✅ |
| Return null su errore | ✅ | ✅ | ✅ |
| Errori non gestiti | ✅ | ✅ | ✅ |
| Boilerplate try/catch | ✅ | ✅ | ⚠️ meno |
| Retry/fallback come pattern | ❌ | ✅ nativo | ❌ |
| Separazione recuperabile/fatale | ⚠️ | ✅ | ⚠️ |

## Attività future (backlog)

- **Documentazione multilingua:** uniformare e mantenere la documentazione in italiano e inglese. Attualmente i doc di ricerca sono in italiano, i file tecnici (LANGUAGE.md, INTENT.md) in inglese. Servono entrambe le versioni per entrambi i pubblici.
