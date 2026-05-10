# Concorrenza in HLL: analisi dei modelli

## Obiettivo

Aggiungere concorrenza a HLL bloccando **a compile-time** 4 antipattern:
- Race Condition / Data Race
- Deadlock
- Busy Waiting / Spin Lock
- Priority Inversion

## Modelli candidati

### 1. Ownership + Send/Sync (Rust)

**Come funziona:** il type system traccia chi "possiede" un dato. Un dato mutabile può avere un solo proprietario. Per condividerlo tra thread, deve essere `Send` (trasferibile) o `Sync` (condivisibile immutabilmente). Il borrow checker verifica a compile-time.

**Antipattern bloccati:**
- ✅ Race Condition — impossibile avere due riferimenti mutabili allo stesso dato
- ⚠️ Deadlock — non bloccato (Rust ha deadlock con Mutex)
- ✅ Busy Waiting — non idiomatico (si usano channel/condvar)
- ❌ Priority Inversion — non gestito

**Pro per HLL:**
- HLL ha già type-state → l'ownership è un'estensione naturale
- Zero-cost: nessun overhead runtime
- Garanzie fortissime a compile-time

**Contro per HLL:**
- Complessità enorme del borrow checker
- Curva di apprendimento ripida (il problema #1 di Rust)
- HLL transpila a Java (che ha GC) → l'ownership non ha senso per la gestione memoria

**Verdetto:** troppo complesso per il target di HLL. L'ownership per la memoria non serve (Java ha GC). Ma l'idea di "un solo proprietario per dato mutabile" è utile.

---

### 2. Actor Model (Erlang/Elixir, Akka)

**Come funziona:** ogni unità di concorrenza è un "attore" con stato privato e una mailbox. Gli attori comunicano solo via messaggi immutabili. Nessuna memoria condivisa.

**Antipattern bloccati:**
- ✅ Race Condition — nessuna memoria condivisa, stato privato
- ✅ Deadlock — no lock (solo messaggi asincroni)
- ✅ Busy Waiting — il runtime gestisce lo scheduling
- ⚠️ Priority Inversion — dipende dal runtime/scheduler

**Pro per HLL:**
- Si integra perfettamente con il module system: un `service` = un attore
- `provide` = implementazione dell'attore
- `needs` = canali di comunicazione tra attori
- Nessuna nuova complessità nel type system
- Il `fails` gestisce già gli errori — si estende a "l'attore è crashato"

**Contro per HLL:**
- Overhead di message passing (copia dei messaggi)
- Mailbox overflow se un attore è lento
- Non adatto a computazione CPU-bound fine-grained

**Verdetto:** ⭐ **candidato migliore**. Si integra con le strutture esistenti di HLL.

---

### 3. CSP — Communicating Sequential Processes (Go)

**Come funziona:** goroutine (thread leggeri) comunicano via channel tipizzati. I channel sono first-class values.

**Antipattern bloccati:**
- ⚠️ Race Condition — mitigato (channel) ma Go permette ancora shared memory
- ⚠️ Deadlock — possibile con channel bloccanti
- ✅ Busy Waiting — non idiomatico (select + channel)
- ❌ Priority Inversion — non gestito

**Pro per HLL:**
- Sintassi semplice (`spawn`, `send`, `receive`)
- Channel tipizzati si integrano col type system
- Familiare ai programmatori Go

**Contro per HLL:**
- Go permette shared memory → le garanzie non sono a compile-time
- Goroutine leak (goroutine bloccata su channel mai letto)
- Per bloccare race condition serve **vietare** shared mutable state (non solo scoraggiarlo)

**Verdetto:** buono ma meno sicuro dell'actor model. Se HLL vieta shared mutable state (già lo fa!) allora CSP diventa sicuro.

---

### 4. STM — Software Transactional Memory (Haskell, Clojure)

**Come funziona:** le modifiche a stato condiviso avvengono in transazioni. Se due transazioni confliggono, una viene ripetuta automaticamente.

**Antipattern bloccati:**
- ✅ Race Condition — transazioni atomiche
- ✅ Deadlock — no lock, solo retry
- ✅ Busy Waiting — il runtime gestisce retry
- ❌ Priority Inversion — non gestito

**Pro per HLL:**
- Composizionale (due transazioni → una transazione più grande)
- Nessun deadlock per costruzione
- Elegante per stato condiviso

**Contro per HLL:**
- Overhead runtime significativo (bookkeeping transazioni)
- Retry storm sotto contention
- Difficile da transpilare a Java idiomatico
- Mai diventato mainstream nonostante decenni di ricerca

**Verdetto:** elegante ma impratico per un linguaggio che transpila a Java.

---

### 5. Structured Concurrency (Java 21+, Kotlin)

**Come funziona:** i task concorrenti sono raggruppati in uno "scope". Lo scope garantisce che tutti i task figli terminino prima che il padre continua. Cancellazione automatica se un figlio fallisce.

**Antipattern bloccati:**
- ⚠️ Race Condition — non direttamente (serve ancora disciplina)
- ⚠️ Deadlock — mitigato (scope limita il lifetime)
- ✅ Busy Waiting — non idiomatico
- ❌ Priority Inversion — non gestito

**Pro per HLL:**
- Si transpila direttamente a Java 21 `StructuredTaskScope`
- Familiare ai programmatori Java
- Lifetime dei task verificabile a compile-time

**Contro per HLL:**
- Non blocca race condition da solo (serve combinare con immutabilità)
- Meno garanzie dell'actor model

**Verdetto:** buon complemento ma non sufficiente da solo.

---

## Proposta per HLL: Actor Model + Type-State

La combinazione naturale per HLL:

```hll
// Un service è un attore
export service Counter {
    function increment() -> Int
    function get() -> Int
}

// spawn crea un'istanza concorrente
function main() {
    let counter = spawn Counter   // crea un attore
    counter.increment()            // messaggio asincrono
    counter.increment()
    let value = await counter.get()  // attende risposta
    println(value)
}
```

**Perché funziona con le strutture esistenti:**

| Struttura HLL | Ruolo nella concorrenza |
|---|---|
| `service` | Definisce l'interfaccia dell'attore (messaggi accettati) |
| `provide` | Implementa lo stato privato dell'attore |
| `needs` | Dipendenze tra attori (grafo di comunicazione) |
| `state` | Protocollo dell'attore (es. `Idle → Running → Done`) |
| `fails` | Gestione errori dell'attore (crash → supervisor) |
| `mock` | Test dell'attore in isolamento |
| DAG check | Previene cicli di comunicazione (→ no deadlock) |

**Keyword nuove necessarie:** `spawn`, `await` (o `send`/`receive`)

**Antipattern bloccati:**
- ✅ Race Condition — stato privato nell'attore, messaggi immutabili
- ✅ Deadlock — DAG dei moduli previene cicli di comunicazione
- ✅ Busy Waiting — il runtime gestisce scheduling
- ⚠️ Priority Inversion — mitigabile con priorità sui service

**Transpilazione a Java:**
- `spawn` → `new Thread()` o `ExecutorService.submit()` o Java 21 virtual threads
- Messaggi → `BlockingQueue` o `CompletableFuture`
- Stato privato → campo dell'oggetto (thread-confined)

---

## Bibliografia

- Hewitt, C. "A Universal Modular ACTOR Formalism for Artificial Intelligence" (1973) — paper originale actor model
- Armstrong, J. "Making reliable distributed systems in the presence of software errors" (2003) — tesi PhD, Erlang/OTP
- Hoare, C.A.R. "Communicating Sequential Processes" (1978) — paper originale CSP
- Harris, T. et al. "Composable Memory Transactions" (2005) — STM in Haskell
- Padovani, L. "Deadlock-Free Typestate-Oriented Programming" (2018) — typestate + concorrenza
- JEP 525: "Structured Concurrency" (Java 21+) — structured concurrency in Java
- Matsakis, N. "Fearless Concurrency with Rust" (2015) — ownership per concorrenza

## Decisione: Actor Model

**Scelto:** Actor Model (stile Erlang/Elixir, non Akka untyped).

**Ragioni:**

1. **Provato in produzione a scala massiva.** WhatsApp (2M connessioni/server, Erlang), Discord (milioni di utenti, Elixir), sistemi telecom Ericsson (99.9999999% uptime). Nessun altro modello ha questo track record.

2. **Blocca race condition per costruzione.** Stato privato + messaggi immutabili = nessuna memoria condivisa = nessuna race condition. Non è una disciplina del programmatore, è una garanzia strutturale.

3. **Si integra perfettamente con le strutture esistenti di HLL.** Non serve inventare nulla di nuovo:
   - `service` = interfaccia dell'attore (messaggi accettati)
   - `provide` = implementazione con stato privato
   - `needs` = grafo di comunicazione tra attori
   - `state` = protocollo dell'attore (typestate)
   - `fails` = gestione errori (crash → supervisor)
   - DAG check = previene cicli di comunicazione (→ no deadlock)

4. **Transpilabile a Java.** Akka ha dimostrato che il modello funziona su JVM. La transpilazione può usare virtual threads (Java 21) o `ExecutorService` + `BlockingQueue`.

5. **Fault tolerance nativa.** Il pattern supervisor (Erlang/OTP) si integra con `fails` — un attore che crasha viene riavviato dal supervisore, non propaga l'errore a tutto il sistema.

**Scartati:**
- **Rust ownership:** troppo complesso, inutile con GC Java, curva di apprendimento inaccettabile
- **CSP (Go):** non blocca race condition a compile-time (Go permette shared memory)
- **STM (Haskell):** retry storm sotto carico, no I/O in transazioni, non distribuibile, mai mainstream
- **Structured Concurrency (Java 21):** buon complemento ma non sufficiente da solo per bloccare race condition

## Prossimi passi

1. Definire la sintassi esatta (`spawn`, `await`, supervisor)
2. Decidere se i messaggi sono sincroni (come Go channel) o asincroni (come Erlang mailbox)
3. Implementare nella grammatica di p4a (o creare p5a)
4. Scrivere test che dimostrano il blocco di Race Condition e Deadlock
