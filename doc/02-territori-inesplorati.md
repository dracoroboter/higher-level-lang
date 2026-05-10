# Territori di ricerca: antipattern bloccabili dal linguaggio

## Riepilogo

| # | Nome locale | Antipattern che blocca | Novità |
|---|---|---|---|
| 1 | Open senza Close | Temporal Coupling | Type-state nativo |
| 2 | Tipi nominali obbligatori | Stringly-typed, injection | No String raw nelle interfacce |
| 3 | Singleton vs Global State | Mutable global state | Service con limiti di accoppiamento |
| 4 | Nessun null | NPE | Option come unica via, unificato con type-state |
| 5 | Errori obbligatori | Exception swallowing, unchecked errors | Result senza scappatoie |
| 6 | Log inerente | Logging assente/inconsistente | Observability come costrutto del linguaggio |

---

## 1. Open senza Close (Temporal Coupling)

### Il nome

Chiamiamo questo antipattern **"Open senza Close"** perché la sua manifestazione più intuitiva è esattamente quella: apri una risorsa e dimentichi di chiuderla, o la usi prima di aprirla. Il nome accademico è *Temporal Coupling* — il codice funziona solo se le operazioni avvengono in un ordine preciso, ma niente nel linguaggio lo garantisce.

### Manifestazioni

```java
// 1. Uso prima dell'apertura
Connection conn = new Connection();
conn.send("hello");  // errore runtime: non connesso

// 2. Dimenticanza della chiusura
File f = open("data.txt");
f.read();
// ... f non viene mai chiuso → leak di risorse

// 3. Operazione nello stato sbagliato
Transaction tx = db.begin();
tx.commit();
tx.addRecord(record);  // errore: transazione già chiusa

// 4. Doppia chiusura
stream.close();
stream.close();  // comportamento indefinito
```

### Cosa fanno i linguaggi oggi

| Approccio | Linguaggio | Limiti |
|-----------|-----------|-------|
| `try-with-resources` / `using` | Java, C# | Solo per il caso open/close, non generalizzabile |
| RAII (distruttore automatico) | C++, Rust | Risolve il leak ma non impedisce l'uso nello stato sbagliato |
| Type-state pattern manuale | Rust | Possibile ma verboso: devi creare un tipo per ogni stato |
| Linter/analisi statica | Tutti | Warning, non errore — ignorabile |

Nessuno offre una soluzione **dichiarativa e verificata a compile-time** per protocolli arbitrari.

### La proposta: `state` come costrutto nativo

```
state Connection {
    states: Disconnected, Connected, Closed

    Disconnected {
        connect() -> Connected
    }

    Connected {
        send(data: Bytes) -> Connected
        disconnect() -> Closed
    }

    Closed {
        // nessun metodo disponibile: tipo "terminale"
    }
}
```

**Cosa garantisce il compilatore:**
- `send()` è chiamabile **solo** su una `Connection` in stato `Connected`
- Dopo `disconnect()`, il tipo diventa `Connection<Closed>` — nessun metodo è disponibile
- Se il codice ha un path dove `Connection` non raggiunge `Closed`, warning di resource leak
- Doppia chiusura impossibile: dopo `disconnect()` il tipo non ha più `disconnect()`

### Come funzionerebbe in pratica

```
let conn = Connection.new()       // tipo: Connection<Disconnected>
conn.connect()                     // tipo diventa: Connection<Connected>
conn.send("hello")                 // ok: send esiste in Connected
conn.disconnect()                  // tipo diventa: Connection<Closed>
conn.send("world")                 // ERRORE COMPILE-TIME: send non esiste in Closed
```

Il compilatore traccia lo stato attraverso il flusso di controllo:

```
let conn = Connection.new()
if condition {
    conn.connect()
    conn.send("data")
}
conn.disconnect()  // ERRORE: in un branch conn è ancora Disconnected
```

### Confronto con soluzioni esistenti

| | try-with-resources | RAII | State nativo |
|---|---|---|---|
| Previene leak | ✅ | ✅ | ✅ |
| Previene uso nello stato sbagliato | ❌ | ❌ | ✅ |
| Previene doppia chiusura | ❌ | ✅ | ✅ |
| Generalizzabile a protocolli arbitrari | ❌ | ❌ | ✅ |
| Costo runtime | Zero | Zero | Zero |
| Verbosità per il programmatore | Bassa | Media | Bassa (dichiarativo) |

### Precedenti accademici

- **Plaid** (CMU, 2009-2014): linguaggio di ricerca con typestate. Mai uscito dall'accademia, sintassi complessa.
- **Session Types** (teoria dei tipi): formalizzano protocolli di comunicazione.
- **Linear Types** (Rust, Clean): garantiscono che un valore sia usato esattamente una volta. Prerequisito per type-state ma non sufficienti da soli.

### Domande aperte

1. Come gestire i branch? Se in un `if` lo stato cambia solo in un ramo, il compilatore deve unificare.
2. Come gestire i loop? Lo stato deve essere invariante nel loop?
3. Composizione: se un oggetto contiene due risorse con stato, come si compongono i protocolli?
4. Ergonomia: il programmatore deve annotare lo stato ovunque, o il compilatore lo inferisce?

---

## 2. Tipi nominali obbligatori (Stringly-typed)

### L'antipattern

Usare `String` per tutto: email, URL, codice fiscale, query SQL. Il type system non distingue e non protegge.

```java
void sendEmail(String to, String subject, String body) { ... }
sendEmail(subject, body, to);  // compila, ma è sbagliato
```

### Cosa fanno i linguaggi oggi

Haskell e Rust hanno i newtype (wrapper a costo zero), ma sono **opzionali**. Nessuno ti obbliga a usarli. In pratica la gente continua a passare `String` ovunque.

### La proposta: no String raw nelle interfacce pubbliche

Il linguaggio non ha un tipo `String` generico usabile nelle firme pubbliche. Devi dichiarare tipi nominali:

```
type Email = String where matches(/^.+@.+\..+$/)
type URL = String where starts_with("http")
type Subject = String where max_length(200)

fn sendEmail(to: Email, subject: Subject, body: Body) { ... }
```

Costo runtime: zero (è lo stesso String sotto). Il compilatore distingue i tipi e impedisce di scambiarli.

### Cosa elimina

- **SQL injection by design** — se la query accetta `Query` e non `String`, non puoi passarci input utente raw
- **Parametri scambiati** — `sendEmail(Email, Subject, Body)` non è confondibile
- **Validazione al confine** — la conversione `String → Email` è l'unico punto dove validi, poi il tipo garantisce la correttezza ovunque
- **Documentazione implicita** — la firma dice *cosa* è ogni parametro, non solo il tipo di dato

### Domande aperte

1. Come gestire le conversioni? `"hello@example.com" as Email` con validazione a runtime?
2. Le funzioni interne/private possono usare String raw? (Probabilmente sì, per pragmatismo)
3. Come interagire con librerie esterne che usano String?
4. I vincoli `where` sono opzionali o obbligatori nella dichiarazione del tipo?

---

## 3. Singleton vs Global State

### Il problema

Singleton e global state mutable sono la stessa cosa a livello di implementazione — la differenza è nell'*intento* e nell'*uso*. La domanda è: si può distinguere formalmente?

### Quando Singleton è legittimo vs quando è global state mascherato

| Singleton legittimo | Global state (antipattern) |
|---|---|
| Stateless o read-only (config, logger) | Stato mutabile condiviso |
| Accesso controllato (interfaccia stretta) | Accesso diretto da ovunque |
| Iniettabile/sostituibile (testabile) | Hardcoded, non mockabile |
| Una sola responsabilità | Accumula responsabilità |

### Criteri formali per distinguerli

1. **Mutabilità**: se il servizio ha stato mutabile, richiedi sincronizzazione esplicita e limita chi può scriverci
2. **Accoppiamento**: se più di N moduli dipendono dallo stesso servizio, warning — stai creando un hub
3. **Iniettabilità obbligatoria**: nessun accesso globale diretto. Anche un singleton deve essere iniettato — così è sostituibile nei test
4. **Immutabilità di default**: un `service` è immutabile a meno che non dichiari esplicitamente `mutable service`

### Possibile costrutto

```
service Logger {                    // immutabile, ok ovunque
    fn log(msg: LogMessage) { ... }
}

mutable service ConnectionPool {   // mutabile: vincoli extra
    max_consumers: 3               // max 3 moduli possono iniettarlo
    fn acquire() -> Connection
    fn release(conn: Connection)
}
```

**Il compilatore:**
- Rifiuta accesso diretto (`ConnectionPool.acquire()` — errore)
- Richiede injection (`inject pool: ConnectionPool` nella firma del modulo)
- Conta i consumatori e rifiuta se superano `max_consumers`
- Un `mutable service` senza `max_consumers` è errore

### Il punto critico

La distinzione formale non è perfetta. Un `service` immutabile che logga su file ha side effects. Ma il linguaggio cattura l'80% dei casi problematici con:
- Immutabile di default → forza la decisione esplicita
- Injection obbligatoria → elimina il "globale nascosto"
- Limite di accoppiamento → impedisce che diventi un hub

Il restante 20% ricade nel territorio del God Object (limiti strutturali).

---

## 4. Nessun null (NPE)

### L'antipattern

Il "billion dollar mistake" di Tony Hoare. Ogni riferimento può essere null, e il linguaggio non ti obbliga a controllare.

### Cosa fanno i linguaggi oggi

| Approccio | Linguaggio | Limiti |
|---|---|---|
| Nullable types (`Type?`) | Kotlin, Swift, Dart | Buono, ma `!!` è una scappatoia |
| `Option<T>` | Rust, Haskell, OCaml | Solido, ma `unwrap()` esiste |
| Null di default | Java, C#, Python, JS | Nessuna protezione |

### La proposta: nessun null, nessuna scappatoia

- Il linguaggio **non ha null**. Non esiste il concetto.
- I valori assenti sono `Option<T>` — un tipo con due stati: `Some(value)` o `None`.
- **Non esiste `unwrap()`**. L'unico modo di accedere al valore è il pattern matching:

```
let result: Option<User> = findUser(id)

match result {
    Some(user) => greet(user)
    None => showError("not found")
}
```

### Unificazione con type-state

`Option<T>` è in fondo un caso speciale di type-state:

```
state Option<T> {
    states: Some, None

    Some {
        value() -> T
        map(fn) -> Option<U>
    }

    None {
        default(T) -> T
    }
}
```

Questo suggerisce che il costrutto `state` è più fondamentale di `Option` — e `Option` ne è solo un'istanza predefinita.

### Domande aperte

1. Senza `unwrap()`, come gestire i casi in cui *sai* che il valore c'è? (Es. dopo un check precedente) → Il type-state potrebbe risolvere: dopo il check, il tipo è già `Some`.
2. Performance: il pattern matching obbligatorio aggiunge overhead? → No se il compilatore ottimizza (come Rust).

---

## 5. Errori obbligatori (Exception swallowing)

### L'antipattern

Eccezioni ignorate, `catch` vuoti, errori non gestiti che esplodono a runtime in produzione.

```java
try {
    riskyOperation();
} catch (Exception e) {
    // TODO: handle this later
}
```

### Cosa fanno i linguaggi oggi

| Approccio | Problema |
|---|---|
| Java checked exceptions | Verboso, la gente mette `throws Exception` ovunque |
| Unchecked exceptions (Python, JS) | Nessuna garanzia, esplode a runtime |
| Rust `Result<T, E>` | Buono ma `unwrap()` è una scappatoia |
| Go `if err != nil` | Verboso, facile dimenticare il check |

### La proposta: Result senza scappatoie

Ogni funzione che può fallire dichiara i suoi errori. Il compilatore **rifiuta di ignorarli**. Niente `unwrap()`, niente `catch` vuoto, niente `throws Exception` generico.

```
fn readFile(path: FilePath) -> Content fails IOError {
    ...
}

// Il chiamante DEVE gestire ogni caso:
let content = readFile(path)
    | IOError.NotFound => default_content
    | IOError.Permission => abort("no access")

// Oppure propaga esplicitamente:
fn loadConfig(path: FilePath) -> Config fails IOError {
    let raw = readFile(path)?   // propaga, ma il tipo lo dichiara
    parse(raw)
}
```

### Cosa elimina

- **Catch vuoti** — impossibili, ogni errore deve avere un handler
- **Exception generiche** — devi dichiarare *quali* errori, non `throws Exception`
- **Errori silenziosi** — il compilatore rifiuta un `Result` non gestito
- **Panic nascosti** — niente `unwrap()`, niente crash implicito

### Relazione con il type-state

Anche qui c'è un collegamento: una funzione che `fails` restituisce un valore in uno di due stati (`Success | Failure`), e il compilatore forza la gestione di entrambi i rami.

### Domande aperte

1. Come gestire errori "impossibili" (es. dopo una validazione)? Serve un meccanismo di `assert` che il compilatore può verificare?
2. Come comporre errori di funzioni diverse? Union types automatica? (`fails IOError | ParseError`)
3. Come gestire errori fatali (out of memory)? Probabilmente un livello separato non catturabile.

---

## 6. Log inerente e querabile

### L'antipattern

Logging assente, inconsistente, non strutturato. In produzione qualcosa va storto e non c'è modo di capire cosa è successo. Oppure c'è troppo log ma è testo libero non filtrabile.

### Cosa fanno i linguaggi oggi

Nessun linguaggio ha logging come costrutto. È sempre una libreria (Log4j, slog, Winston, logging). Conseguenze:
- Il programmatore decide cosa loggare (e spesso decide male)
- Il formato è inconsistente
- Aggiungere logging è lavoro manuale e inquina il codice
- Il log non è queryabile senza tool esterni (ELK, Datadog)

### La proposta: observability come costrutto del linguaggio

Il linguaggio produce automaticamente un **log strutturato e queryabile** senza codice esplicito del programmatore.

#### Livello 1: Trace automatico

Ogni chiamata di funzione marcata `@observable` viene tracciata:

```
@observable
fn processOrder(order: Order) -> Receipt {
    let validated = validate(order)
    let receipt = charge(validated)
    receipt
}
```

Il runtime registra automaticamente:
```json
{
    "fn": "processOrder",
    "input": {"order": {"id": 42, "amount": 99.90}},
    "output": {"receipt": {"id": "R-001"}},
    "duration_ms": 45,
    "called": ["validate", "charge"],
    "state_changes": []
}
```

#### Livello 2: Log semantico

Il programmatore annota *cosa* è importante, il linguaggio lo cattura strutturato:

```
fn processOrder(order: Order) -> Receipt {
    emit OrderReceived { order_id: order.id, amount: order.total }
    let receipt = charge(order)
    emit OrderCompleted { order_id: order.id, receipt_id: receipt.id }
    receipt
}
```

`emit` non è un print — è un evento tipizzato che il runtime raccoglie.

#### Livello 3: Query nativa

Il log non è testo ma dati tipizzati, interrogabili:

```
query events
    where type == OrderCompleted
    and duration > 100ms
    last 1h
```

#### Cosa elimina

- **Logging dimenticato** — il trace automatico c'è sempre
- **Log non strutturato** — tutto è tipizzato
- **Inconsistenza** — il formato è deciso dal linguaggio, non dal programmatore
- **Boilerplate** — niente `logger.info("processing order " + order.id)`

#### Costo e ottimizzazione

Il compilatore può:
- Eliminare il trace se nessuno ascolta (zero-cost abstraction)
- Compilare le query in filtri efficienti
- Decidere a compile-time cosa è observable e cosa no

### Confine linguaggio/runtime

Questo è il territorio più ambiguo: è un costrutto del linguaggio o una feature del runtime? Probabilmente entrambi:
- Il **linguaggio** definisce `@observable`, `emit`, `query`
- Il **runtime** decide dove vanno i dati (memoria, file, rete)
- Il **compilatore** ottimizza via ciò che non serve

### Domande aperte

1. Quanto è il costo del trace automatico? Anche con zero-cost abstraction, la serializzazione degli input ha un costo.
2. Privacy: se tutto è loggato, come gestire dati sensibili? Serve un `@sensitive` che esclude campi dal trace?
3. Il query language è parte del linguaggio o un tool separato?
4. Come gestire il volume? Sampling automatico? Retention policy nel linguaggio?

---

## Osservazioni trasversali

### Pattern OOP-specifici vs universali

Non tutti i pattern sono universali. Molti esistono solo come workaround a limiti dell'OOP:

**Pattern OOP-specifici (scompaiono con un linguaggio espressivo):**
- Visitor → pattern matching
- Strategy → first-class functions
- Template Method → higher-order functions + trait
- Iterator → generatori/lazy sequences
- Command → funzioni come valori
- Decorator → composizione di funzioni
- Factory Method → costruttori nominati + sum types
- Observer → binding reattivi nativi

**Pattern universali (esistono in qualsiasi paradigma):**
- State Machine, Circuit Breaker, Retry, Saga, Producer-Consumer, Builder, Repository

**Implicazione:** il linguaggio non deve essere OO. Deve essere abbastanza espressivo da far scomparire i pattern OOP-specifici, e avere costrutti nativi per i pattern universali.

**Antipattern:** stessa logica. Deep Inheritance, Yo-Yo Problem, Anemic Domain Model sono OOP-specifici. NPE, Temporal Coupling, Race Condition, Stringly-typed sono universali.

### I 5 pilastri del linguaggio

L'analisi del database (93 entry) mostra che il type-state da solo copre ~35-40% dei casi. Servono almeno 5 meccanismi ortogonali:

| Pilastro | Cosa risolve | Copertura |
|---|---|---|
| 1. Type-state | Protocolli, risorse, errori, nullabilità | ~35% |
| 2. Tipi nominali obbligatori | Stringly-typed, primitive obsession, injection | ~15% |
| 3. Vincoli strutturali | God object, accoppiamento eccessivo | ~10% |
| 4. Module system con DAG | Circular deps, encapsulation, organizzazione | ~15% |
| 5. Composizione (no ereditarietà) | Deep inheritance, fragile base class | ~10% |

Il restante ~15% (spaghetti code, copy-paste, golden hammer) non è risolvibile dal linguaggio — è problema umano/organizzativo.

### Unificazione tramite type-state

Molti di questi territori convergono verso il **type-state** come meccanismo fondamentale:
- Open senza Close → stati espliciti di una risorsa
- Option (no null) → stato `Some | None`
- Result (errori) → stato `Success | Failure`
- Service → stato `Initialized | Running | Stopped`

Questo suggerisce che il costrutto `state` potrebbe essere il **primitivo fondamentale** del linguaggio, da cui derivano gli altri.

### Gradiente di novità

| Territorio | Già risolto altrove | Parzialmente risolto | Genuinamente nuovo |
|---|---|---|---|
| Nessun null | ✅ (Rust, Kotlin) | | |
| Errori obbligatori | | ✅ (Rust, ma con unwrap) | |
| Open senza Close | | ✅ (Plaid, accademico) | |
| Tipi nominali obbligatori | | ✅ (newtype esiste, obbligo no) | |
| Singleton vs Global State | | | ✅ |
| Log inerente | | | ✅ |
