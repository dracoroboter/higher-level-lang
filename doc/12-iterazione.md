# Iterazione in HLL: analisi dei modelli

## Problema

HLL non ha un costrutto per iterare su collezioni. Serve per:
- Iterare su liste (findByEmail ritorna una lista)
- Trasformare dati (map)
- Filtrare (filter)
- Accumulare (reduce/fold)
- Subscriber pattern (for each message in queue)

## Due modelli fondamentali

### Modello Imperativo (`for x in collection`)

```hll
for order in orders {
    if order.amount > 100 {
        println(order.id)
    }
}
```

**Chi lo usa:** Python, Rust, Go, Kotlin, Swift, Java (enhanced for)

**Pro:**
- Familiare a tutti i programmatori
- Facile da debuggare (step-by-step, breakpoint nel body)
- Permette `break`, `continue`, early return
- Effetti collaterali espliciti
- Performance prevedibile

**Contro:**
- Mescola "cosa" con "come" (logica + iterazione insieme)
- Più verboso per trasformazioni (filter+map = 5+ righe)
- Mutabilità implicita (accumulatori)
- Più facile introdurre bug (off-by-one, stato mutabile)

---

### Modello Dichiarativo (`collection.filter().map()`)

```hll
let bigOrders = orders
    .filter(|o| o.amount > 100)
    .map(|o| o.id)
```

**Chi lo usa:** Haskell, Elixir, Rust (iterators), Java 8+ (Streams), Kotlin, Scala

**Pro:**
- Esprime l'intento senza il meccanismo
- Composizionale (catena di trasformazioni)
- Nessuna mutabilità
- Parallelizzabile automaticamente
- Meno bug (no off-by-one)

**Contro:**
- Richiede lambda/closure
- Debugging più difficile (pipeline opaca)
- `break`/early return complessi (serve `takeWhile`, `find`)
- Meno familiare per programmatori imperativi
- Errori in una lambda: come si gestisce `fails` dentro un `map`?

---

## Il problema irrisolto: dichiarativo + debuggabile + break

Nessun linguaggio ha risolto completamente questo:

| Linguaggio | Dichiarativo? | Break? | Debuggabile? |
|---|---|---|---|
| Python comprehension | ✅ | ❌ | ⚠️ |
| C# LINQ | ✅ | ❌ (solo Take) | ✅ (Visual Studio) |
| Kotlin | ✅ | ❌ (bug KT-1436 dal 2015) | ⚠️ |
| Rust | ✅ pipeline + ✅ for | ✅ (nel for) | ✅ |
| Java Streams | ✅ | ❌ | ❌ |
| Haskell | ✅ | ❌ (takeWhile) | ❌ |

**Rust** è il più vicino: componi la pipeline dichiarativa, poi la consumi con un `for` imperativo dove break/continue funzionano. Ma richiede due costrutti separati.

---

## Proposte per HLL

### Proposta A: `for-in` con clausole dichiarative (ibrido)

```hll
for order in orders
    where order.amount > 100
    where order.status != "cancelled"
    take 10
{
    // body imperativo: break, continue funzionano
    println(order.id)
}
```

Con trasformazione (yield → produce una nuova lista):
```hll
let ids = for order in orders
    where order.amount > 100
    yield order.id
```

Con accumulo:
```hll
let total = for order in orders
    where order.status == "confirmed"
    into sum(order.amount)
```

**Pro:** un solo costrutto, no lambda per casi comuni, debuggabile, break/continue nel body
**Contro:** `yield` ha semantica complessa, `into` è nuovo, non copre tutti i casi

### Proposta B: `from/where/select` (stile LINQ/SQL)

```hll
let ids = from order in orders
    where order.amount > 100
    select order.id

let total = from order in orders
    where order.status == "confirmed"
    reduce sum(order.amount)
```

**Pro:** leggibile come SQL, familiare a chi conosce LINQ, debuggabile
**Contro:** nuove keyword (from, select, reduce), meno familiare per non-C#

### Proposta C: Pipeline con lambda (stile Rust/Java/Kotlin)

```hll
let ids = orders
    .filter(|o| o.amount > 100)
    .map(|o| o.id)
    .collect()
```

**Pro:** standard industria, potente, composizionale
**Contro:** richiede lambda (feature non ancora in HLL), debugging difficile, no break

### Proposta D: Ibrido (A + C)

Per i casi semplici (80%): `for-in` con clausole dichiarative (no lambda):
```hll
let ids = for order in orders
    where order.amount > 100
    yield order.id
```

Per i casi complessi (20%): pipeline con lambda:
```hll
let result = orders
    .filter(|o| o.amount > 100)
    .map(|o| transform(o))
    .reduce(|acc, x| acc + x)
```

**Pro:** copre tutti i casi, il 80% non richiede lambda, debuggabile per i casi comuni
**Contro:** due costrutti da imparare, complessità del linguaggio

---

## Dipendenze

Qualsiasi proposta richiede:
- **`List<T>`** come tipo builtin (o almeno un protocollo Iterable)
- Per C e D: **lambda/closure** (`|params| expr`)

## Precedenti e fonti

- Rust iterators: zero-cost abstraction, pipeline + for-in (The Rust Book, cap. 13)
- C# LINQ: query syntax integrata nel linguaggio (Meijer, E. "LINQ: Reconciling Object, Relations and XML" 2006)
- Python comprehension: Guido van Rossum, PEP 202 (2000)
- Kotlin sequences: lazy evaluation senza lambda overhead
- Java Streams: Goetz, B. "State of the Lambda" (2013)
- Khatchadourian, R. et al. "An Empirical Study on the Use and Misuse of Java 8 Streams" (FASE 2020, Springer) — studio su antipattern nelle pipeline

## Problemi noti delle pipeline dichiarative (Java Streams)

Dalla letteratura e dalla pratica industriale emergono problemi specifici quando le pipeline dichiarative contengono operazioni complesse:

### 1. Side effects nelle lambda (I/O, DB calls)

```java
// PROBLEMATICO: chiamata DB dentro un map
orders.stream()
    .map(o -> repository.findDetails(o.id))  // N chiamate DB!
    .filter(d -> d.isValid())
    .collect(toList());
```

**Problema:** ogni elemento della pipeline fa una chiamata di rete/DB. Con 1000 elementi = 1000 query. Impossibile da ottimizzare (il runtime non sa che potrebbe fare un batch). Con `parallelStream()` peggiora: N thread che martellano il DB.

### 2. parallelStream() — quando NON usarlo

**Fonti:** Oracle docs, Goetz "Java Concurrency in Practice", studio empirico FASE 2020

Regole empiriche (dalla letteratura):
- **Non usare** se le operazioni hanno side effects (I/O, stato mutabile)
- **Non usare** se la collezione è piccola (<10.000 elementi) — overhead > beneficio
- **Non usare** se l'ordine conta
- **Non usare** con sorgenti non splittabili (LinkedList, Stream.iterate)
- **Usare** solo per computazione CPU-bound pura su grandi collezioni (>100.000 elementi)

Il paper FASE 2020 (Khatchadourian et al.) ha trovato che il **39% degli usi di parallelStream in progetti open source è problematico** — side effects, collezioni troppo piccole, o operazioni non associative.

### 3. Debugging impossibile

Una pipeline come:
```java
result = orders.stream()
    .filter(o -> isValid(o))
    .map(o -> transform(o))
    .flatMap(o -> expand(o))
    .filter(o -> check(o))
    .collect(toList());
```

Se `result` è sbagliato, dove è il bug? Non puoi mettere un breakpoint "tra" filter e map. Devi spezzare la pipeline in variabili intermedie, perdendo il vantaggio dichiarativo.

### 4. Gestione errori

```java
// Cosa succede se transform() lancia un'eccezione?
orders.stream()
    .map(o -> transform(o))  // checked exception non permessa in lambda!
    .collect(toList());
```

Java non permette checked exceptions nelle lambda. Devi wrappare in RuntimeException o usare workaround brutti. Questo è un problema strutturale del modello.

### Implicazioni per HLL

Questi problemi suggeriscono che la proposta A (for-in con clausole) è più sicura:
- Le clausole `where` sono pure (no side effects — il compilatore può verificarlo)
- Il body imperativo è dove vanno gli effetti (I/O, DB calls) — espliciti e debuggabili
- Nessuna tentazione di mettere chiamate DB dentro un `filter`
- Il compilatore potrebbe **rifiutare** side effects nelle clausole dichiarative (come Haskell separa IO da puro)

Possibile regola per HLL: **le espressioni nelle clausole `where`/`yield` devono essere pure** (no `fails`, no I/O, no mutazione). Gli effetti vanno nel body del `for`.

### Come riprodurre l'effetto di chiamate iterative a risorse esterne

Rendere impossibile l'I/O nell'iterazione non è accettabile — il caso d'uso "per ogni ordine, chiama il servizio esterno" è reale e frequente. La soluzione non è vietare, ma **separare e rendere esplicito**:

```hll
// CASO 1: filtro puro + effetti nel body (preferito)
for order in orders
    where order.amount > 100    // puro: nessuna chiamata esterna
{
    let details = fetchDetails(order.id)  // effetto esplicito nel body
        | IOError(e) => continue          // gestione errore chiara
    println(details.summary)
}

// CASO 2: pre-fetch batch (ottimizzazione esplicita)
let ids = for order in orders where order.amount > 100 yield order.id
let details = batchFetch(ids)  // una sola chiamata, non N
for detail in details {
    println(detail.summary)
}

// CASO 3: pipeline con effetti (se lambda disponibili)
// Il compilatore AVVISA (non vieta) che c'è I/O nella pipeline
let results = orders
    .filter(|o| o.amount > 100)
    .map(|o| fetchDetails(o.id))   // WARNING: I/O in pipeline, consider batch
    .collect()
```

**Principio:** non vietare, ma rendere visibile. Il compilatore:
- **Nelle clausole `where`/`yield`:** errore se c'è I/O (queste devono essere pure)
- **Nel body del `for`:** I/O permesso (è il posto giusto)
- **Nelle pipeline `.map()`:** warning se rileva una funzione con `fails` o I/O (suggerisce batch)

Questo guida il programmatore verso il pattern corretto senza impedirgli di lavorare.

## Decisione

**Rimandata.** Da implementare nel livello 5 (p5a e p5b come prototipi concorrenti). La proposta D (ibrido) è la più promettente. Valutare se il `for-in` con clausole (proposta A/B) è sufficiente per la benchmarkapp senza lambda.

## Analisi: vantaggi lambda riprodotti dall'ibrido?

| Vantaggio lambda | Riprodotto dall'ibrido? | Come |
|---|---|---|
| Composizione multi-step | ⚠️ Parziale | Concatenare più `for` o funzioni |
| Lazy / short-circuit | ✅ | `take N` |
| Riusabilità pipeline | ✅ | Funzione che wrappa il `for` |
| Parallelismo automatico | ❌ | Serve keyword aggiuntiva o actor model |
| Operazioni terminali diverse | ⚠️ Parziale | `into` estensibile (sum, count, max, groupBy) |

### Composizione multi-step

Lambda: `orders.filter(active).map(total).reduce(sum)` — una riga, N trasformazioni.

Ibrido:
```hll
let totals = for order in orders where order.active yield order.total
let sum = for t in totals into sum(t)
```
Due righe. Per 3+ trasformazioni la verbosità cresce linearmente ma resta leggibile.

### Parallelismo

Lambda (Java): `orders.parallelStream().filter(...).collect(...)` — una keyword.

Ibrido: non ha equivalente diretto. Possibile estensione: `for order in orders parallel where ...` ma richiede integrazione con l'actor model (spawn worker per chunk). Alternativa: il compilatore parallelizza automaticamente se le clausole sono pure (verificato a compile-time).

### Conclusione

L'ibrido copre ~80% dei casi senza lambda. Il 20% rimanente si risolve con:
- Funzioni helper (composizione multi-step)
- Actor model (parallelismo)
- Estensione di `into` (operazioni terminali)

Lambda resta utile per il 20% ma non è bloccante per il prototipo iniziale. p5a implementa lambda per confronto, p5b dimostra che l'ibrido è sufficiente per la maggior parte dei casi.

## Studio della letteratura sugli iteratori

### Paper e fonti principali

1. **Gamma et al. "Design Patterns" (1994)** — definizione originale. Distingue:
   - **External iterator**: il client controlla l'avanzamento (`next()`, `hasNext()`)
   - **Internal iterator**: la collezione controlla il traversal (callback/visitor)

2. **Moors, Piessens, Odersky "Iterators Reconsidered" (2002)** — critica fondamentale:
   - Gli iteratori esterni **violano l'incapsulamento**: espongono lo stato interno della collezione
   - Problemi di **invalidazione**: modificare la collezione invalida l'iteratore
   - Problemi di **aliasing**: due iteratori sulla stessa collezione possono interferire
   - Proposta: iteratori interni (higher-order) sono più sicuri ma meno flessibili

3. **Järvi, Freeman, Crowl "Internal Iteration Externalized" (1999)** — dimostra che:
   - Gli iteratori interni sono **più sicuri** (no invalidation, no aliasing)
   - Ma sono **meno flessibili** (no early exit, no interleaving di due collezioni)
   - Proposta: con closure/coroutine si può avere la sicurezza dell'interno con la flessibilità dell'esterno

4. **P0467R2 "Iterator Concerns for Parallel Algorithms" (C++ Committee, 2017)** — problemi specifici:
   - Iteratori non thread-safe per default
   - Invalidazione in contesto parallelo è undefined behavior
   - Proposta: range-based iteration con ownership semantics

5. **Khatchadourian et al. "An Empirical Study on the Use and Misuse of Java 8 Streams" (FASE 2020)**:
   - 39% degli usi di `parallelStream` è problematico
   - Side effects nelle lambda sono il bug più comune
   - Refactoring da loop a stream introduce bug nel 14% dei casi

### Tassonomia dei problemi

| Problema | External iter | Internal iter | for-in | Pipeline lazy | HLL p5b |
|---|---|---|---|---|---|
| Invalidazione (modifica durante iterazione) | ❌ Bug | ✅ Safe | ⚠️ Dipende | ✅ Safe (immutabile) | ✅ Safe (clausole pure) |
| Off-by-one | ❌ Possibile | ✅ Safe | ✅ Safe | ✅ Safe | ✅ Safe |
| Resource leak (iteratore non consumato) | ❌ Possibile | ✅ Safe | ✅ Safe | ❌ Possibile (lazy) | ✅ Safe (eager) |
| Early exit (break) | ✅ Facile | ❌ Difficile | ✅ Facile | ❌ Difficile | ✅ take N + break nel body |
| Interleaving (zip due collezioni) | ✅ Facile | ❌ Impossibile | ❌ Difficile | ✅ zip() | ❌ Non supportato |
| Side effects nascosti | ❌ Possibile | ❌ Possibile | ❌ Possibile | ❌ Possibile | ✅ where/yield puri |
| N+1 query | ❌ Possibile | ❌ Possibile | ❌ Possibile | ❌ Possibile | ⚠️ Solo nel body (visibile) |
| Parallelismo | ❌ Unsafe | ⚠️ Dipende | ❌ Sequenziale | ✅ parallelStream | 🔮 Futuro |

### Conclusioni dalla letteratura

1. **Nessun modello è perfetto** — ogni approccio ha trade-off
2. **La sicurezza viene dalla purezza** — se le operazioni di filtro/trasformazione sono pure, la maggior parte dei bug scompare
3. **L'early exit è il punto debole degli iteratori interni** — risolto in HLL con `take` e `break` nel body
4. **L'interleaving (zip) è il punto debole del for-in** — richiede un costrutto aggiuntivo o lambda
5. **Il parallelismo sicuro richiede garanzie di purezza** — HLL le ha nelle clausole `where`/`yield`

### Implicazioni per HLL

Il modello p5b (for-in con clausole pure) è il più sicuro della tabella:
- Blocca invalidazione, off-by-one, resource leak, side effects nascosti
- Supporta early exit (take + break)
- Punto debole: interleaving (zip) non supportato senza lambda

Questo è un risultato originale: nessun linguaggio mainstream combina clausole pure verificate a compile-time con iterazione imperativa nel body.
