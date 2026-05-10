# I 5 pilastri del linguaggio

## Premessa

Dall'analisi del database (47 pattern + 46 antipattern = 93 entry) e dalla verifica dell'ipotesi type-state, emerge che nessun singolo meccanismo copre tutti i casi. Servono **5 meccanismi ortogonali** che insieme coprono ~85% dei pattern/antipattern formalizzabili.

Il restante ~15% (spaghetti code, copy-paste, golden hammer, premature optimization) non è risolvibile dal linguaggio — è problema umano/organizzativo.

**Lezione da Plaid:** il typestate come paradigma totale è troppo invasivo (anche i suoi creatori l'hanno abbandonato). I nostri pilastri sono **opzionali e dichiarativi** — il programmatore li usa dove servono, non è forzato a usarli ovunque.

---

## Pilastro 1: Type-state (dichiarativo e opzionale)

### Cosa risolve (~35% dei casi)
- Temporal Coupling (Open senza Close)
- NPE (Option come type-state)
- Exception swallowing (Result come type-state)
- Use After Free / Double Free
- Builder pattern
- Command pattern (Created → Executed → Undone)
- Circuit Breaker (Closed → Open → HalfOpen)
- Future/Promise (Pending → Resolved | Rejected)
- Unit of Work (Active → Committed | RolledBack)
- Saga (step-by-step con compensazioni)
- Read-Write Lock (Free → Reading | Writing)

### Costrutto proposto

```
state Connection {
    Disconnected {
        connect() -> Connected
    }
    Connected {
        send(data: Bytes) -> Connected
        disconnect() -> Closed
    }
    Closed { }
}
```

### Cosa fa il compilatore
- Verifica che i metodi siano chiamati solo nello stato corretto
- Traccia lo stato attraverso il flusso di controllo (branch, loop)
- Segnala resource leak (path dove non si raggiunge lo stato terminale)
- Impedisce doppia chiusura / uso dopo chiusura
- Costo runtime: **zero** (è tutto a compile-time)

### Differenza da Plaid
- **Opzionale:** non tutto è type-state. Solo le risorse/protocolli che lo dichiarano.
- **Dichiarativo:** la state machine è esplicita, non implicita nella struttura dei tipi.
- **Semplice:** niente permessi (unique/shared/immutable). L'ownership è sufficiente.

### Domande aperte
1. Come gestire l'aliasing? (Ownership come Rust? O più permissivo?)
2. Come gestire i branch? (Entrambi i rami devono portare allo stesso stato finale?)
3. Come comporre type-state di oggetti diversi?
4. Inferenza: il compilatore può inferire lo stato senza annotazioni esplicite?

---

## Pilastro 2: Tipi nominali obbligatori

### Cosa risolve (~15% dei casi)
- Stringly-typed
- Primitive Obsession
- SQL Injection
- XSS
- Parametri scambiati
- Boolean Blindness
- Hardcoded Credentials (tipo `Secret` non costruibile da literal)

### Costrutto proposto

```
import hll.validation as validate

type Email = String where validate.email()
type URL = String where validate.url()
type UserId = Int
type Query = opaque  // non costruibile da String, solo da prepared statements

fn sendEmail(to: Email, subject: Subject, body: Body) { ... }
```

### Principio: meccanismo vs policy

Il `where` accetta qualsiasi funzione `(BaseType) -> Bool`. Il linguaggio fornisce il **meccanismo** (tipo nominale + vincolo), le librerie forniscono la **policy** (cosa è valido). La regex non è un costrutto del linguaggio — se serve, la libreria la usa internamente.

### Regole
- Le funzioni **pubbliche** non possono usare `String`, `Int`, `Bool` nudi — devono usare tipi nominali
- Le funzioni **private** possono usare tipi primitivi (pragmatismo)
- La conversione `String → Email` è un punto di validazione esplicito
- Costo runtime: **zero** (è lo stesso tipo sotto, il compilatore distingue solo a compile-time)
- I vincoli `where` sono opzionali ma raccomandati

### Cosa elimina
- `sendEmail(subject, body, to)` → errore di compilazione (tipi diversi)
- `query("SELECT * FROM users WHERE id = " + input)` → errore (Query non costruibile da String)
- `if (isAuthorized)` → errore (usa `Authorized | Denied`, non `Bool`)

### Domande aperte
1. Come interagire con librerie esterne che usano String?
2. I vincoli `where` sono verificati a compile-time o runtime?
3. Quanto è granulare l'obbligo? (Solo API pubbliche? Anche tra moduli interni?)

---

## Pilastro 3: Vincoli strutturali

### Cosa risolve (~10% dei casi)
- God Object / God Class
- God Interface
- Blob
- Accoppiamento eccessivo

### Costrutto proposto

```
module OrderService {
    max_dependencies: 5      // max 5 inject
    max_public_methods: 10   // max 10 metodi esposti
    max_consumers: 8         // max 8 moduli possono dipendere da questo
}
```

### Regole
- Limiti **configurabili per progetto** (come `max-line-length` ma enforced dal compilatore)
- Default ragionevoli forniti dal linguaggio
- Il compilatore rifiuta se superati — forza la decomposizione
- Warning prima dell'errore (es. a 80% del limite)

### Cosa elimina
- Classi da 3000 righe con 50 dipendenze
- Interfacce con 30 metodi
- Hub centrali da cui dipende tutto il sistema

### Controversie
- **Chi decide i numeri?** Default + override per progetto. Come i limiti di complessità ciclomatica.
- **È troppo paternalistico?** Come il borrow checker di Rust: fastidioso ma utile. La differenza è che qui i limiti sono configurabili.
- **Può essere aggirato?** Sì, alzando i limiti. Ma il fatto di doverlo fare esplicitamente forza la riflessione.

### Domande aperte
1. Quali metriche esattamente? (Dipendenze, metodi, LOC, complessità ciclomatica?)
2. I limiti sono per tipo, per modulo, o per entrambi?
3. Come gestire i casi legittimi di tipi grandi? (Es. un parser con molti metodi)

---

## Pilastro 4: Module system con DAG enforced

### Cosa risolve (~15% dei casi)
- Circular Dependencies
- Mutable Global State (via injection obbligatoria)
- Singleton vs Global State (via service + injection)
- Big Ball of Mud (via struttura forzata)
- Leaky Abstraction (via encapsulation forte)

### Costrutto proposto

```
module OrderService {
    inject db: Database
    inject logger: Logger
    inject pool: ConnectionPool

    public fn createOrder(order: Order) -> OrderId fails DBError { ... }
    private fn validate(order: Order) -> ValidOrder { ... }
}
```

```
service Logger { ... }                    // immutabile, singleton gestito dal runtime
mutable service ConnectionPool {          // mutabile, vincoli extra
    max_consumers: 3
}
```

### Regole
- **DAG enforced:** il grafo delle dipendenze tra moduli deve essere aciclico. Il compilatore rifiuta cicli.
- **Injection obbligatoria:** nessun accesso globale. Le dipendenze sono dichiarate e iniettate.
- **Service gestiti dal runtime:** i singleton sono `service`, non variabili globali. Il runtime li crea e li inietta.
- **Visibilità esplicita:** `public` / `private` / `internal` (visibile nel package ma non fuori)

### Cosa elimina
- `import` circolari → errore di compilazione
- Accesso diretto a globali → errore (devi usare `inject`)
- Singleton non testabili → impossibile (l'injection permette il mock)
- Dipendenze nascoste → tutte visibili nella dichiarazione del modulo

### Domande aperte
1. Come gestire dipendenze opzionali? (`inject?` con default?)
2. Come gestire il wiring? (Container automatico? Dichiarativo?)
3. Come gestire i moduli che devono comunicare bidirezionalmente? (Event bus? Mediator?)

---

## Pilastro 5: Composizione (no ereditarietà)

### Cosa risolve (~10% dei casi)
- Deep Inheritance / Inheritance Hell
- Fragile Base Class Problem
- Yo-Yo Problem
- Diamond Problem
- Poltergeist (classi vuote che delegano)

### Costrutto proposto

```
trait Printable {
    fn print(self) -> String
}

trait Serializable {
    fn serialize(self) -> Bytes
    fn deserialize(bytes: Bytes) -> Self
}

type Order {
    id: OrderId
    items: List<Item>
    total: Money
}

impl Printable for Order {
    fn print(self) -> String { ... }
}

impl Serializable for Order {
    fn serialize(self) -> Bytes { ... }
    fn deserialize(bytes: Bytes) -> Order { ... }
}
```

### Regole
- **Nessuna ereditarietà di classi.** Non esiste `extends`.
- **Composizione tramite trait:** i comportamenti si aggiungono implementando trait.
- **Trait con default methods:** riuso del codice senza ereditarietà.
- **Delegation esplicita:** se un tipo contiene un altro tipo, la delega è esplicita (no magia).

### Cosa elimina
- Gerarchie profonde → impossibili (non esiste `extends`)
- Override accidentali → impossibili (non c'è ereditarietà)
- Fragile base class → non esiste base class
- Diamond problem → i trait sono lineari, conflitti risolti esplicitamente

### Precedenti
- Rust (trait + impl, no inheritance)
- Go (interfaces implicite, no inheritance)
- Kotlin (delegation esplicita con `by`)

### Domande aperte
1. I trait possono avere stato? (Probabilmente no — solo comportamento)
2. Come gestire il riuso di codice tra tipi simili? (Trait con default + composizione)
3. Serve un meccanismo di delegation automatica? (`delegate field: InnerType`)

---

## Interazione tra i pilastri

I 5 pilastri non sono indipendenti — si rafforzano a vicenda:

| Combinazione | Effetto |
|---|---|
| Type-state + Tipi nominali | `Connection<Connected>` accetta solo `Query`, non `String` |
| Type-state + Module system | I `service` hanno stati (`Initialized → Running → Stopped`) |
| Tipi nominali + Module system | Le interfacce pubbliche dei moduli usano tipi di dominio |
| Vincoli strutturali + Module system | Max dipendenze = max `inject` nel modulo |
| Composizione + Type-state | I trait possono dichiarare protocolli (metodi disponibili solo in certi stati) |

---

## Copertura complessiva

| Pilastro | Pattern facilitati | Antipattern bloccati | Copertura |
|---|---|---|---|
| 1. Type-state | State, Builder, Command, Circuit Breaker, Future, Saga | NPE, Temporal Coupling, Exception swallowing, Use After Free | ~35% |
| 2. Tipi nominali | — (rende inutili workaround) | Stringly-typed, Primitive Obsession, SQL Injection, XSS, Boolean Blindness | ~15% |
| 3. Vincoli strutturali | — (forza decomposizione) | God Object, God Interface, Blob | ~10% |
| 4. Module system | DI, Repository, Facade | Circular Deps, Global State, Big Ball of Mud | ~15% |
| 5. Composizione | Strategy, Decorator, Template Method (scompaiono) | Deep Inheritance, Yo-Yo, Fragile Base Class | ~10% |

**Totale copertura: ~85%**

Il restante 15% (Spaghetti Code, Copy-Paste, Golden Hammer, Premature Optimization, Feature Envy, Shotgun Surgery) richiede disciplina umana, code review, e al massimo lint/analisi statica — non è risolvibile a livello di linguaggio.

---

## Il 15% irriducibile: antipattern causati dagli umani

Questi antipattern non sono errori tecnici — sono errori di **giudizio, disciplina, e organizzazione**. Nessun type system, per quanto avanzato, può impedirli perché non violano regole formali: producono codice che compila e funziona, ma è cattivo codice.

### Lista completa

| Antipattern | Perché è irriducibile |
|---|---|
| **Spaghetti Code** | Il codice è sintatticamente valido, solo mal strutturato. Non esiste una definizione formale di "struttura buona" universale. |
| **Copy-Paste Programming** | Il codice duplicato è corretto — è solo ridondante. Il compilatore non può sapere che due blocchi simili dovrebbero essere una funzione. |
| **Golden Hammer** | Usare lo stesso pattern/tool per tutto è una scelta di design, non un errore di tipo. |
| **Premature Optimization** | Ottimizzare nel posto sbagliato produce codice corretto ma inutilmente complesso. Il compilatore non sa dove sia il bottleneck. |
| **Feature Envy** | Un metodo che usa più dati di un altro oggetto è sintatticamente valido. Serve comprensione del dominio per capire che è nel posto sbagliato. |
| **Shotgun Surgery** | Un cambiamento che richiede modifiche in 20 file è un problema architetturale. Il compilatore non sa che quei 20 file sono logicamente collegati. |
| **Lava Flow** | Codice morto che nessuno osa toccare. Il compilatore può segnalare codice unreachable, ma non codice "inutile ma raggiungibile". |
| **Boat Anchor** | Codice mantenuto "perché potrebbe servire". Stessa situazione del Lava Flow. |

### Cosa può fare il linguaggio (mitigazione, non prevenzione)

Anche se non può bloccarli, il linguaggio può **rendere più difficile** cadere in questi antipattern:

| Mitigazione | Meccanismo | Antipattern mitigato |
|---|---|---|
| **Dead code warning** | Il compilatore segnala codice/tipi non usati | Lava Flow, Boat Anchor |
| **Complessità ciclomatica** | Warning se una funzione supera N branch | Spaghetti Code |
| **Clone detection** | Analisi statica che rileva blocchi duplicati | Copy-Paste |
| **Metriche di accoppiamento** | Warning se un modulo è usato da troppi altri | Shotgun Surgery |
| **Vincoli strutturali (Pilastro 3)** | Max metodi/dipendenze forza la decomposizione | God Object → previene Spaghetti |

### La vera soluzione: processo, non linguaggio

Per questo 15%, la soluzione è:
- **Code review** — un umano che giudica la qualità del design
- **Refactoring guidato** — tool che suggeriscono (non forzano) miglioramenti
- **Architettura esplicita** — documentazione delle decisioni di design
- **Metriche e dashboard** — visibilità sulla salute del codebase

### Implicazione per il progetto

Il nostro linguaggio **non deve promettere di risolvere tutto**. L'85% è già un risultato eccellente — nessun linguaggio esistente copre più del 40-50% (Rust è probabilmente il migliore con ~45%). Il 15% irriducibile è un limite teorico, non un fallimento del design.

La comunicazione corretta è: "Il linguaggio rende **impossibili** gli errori tecnici e rende **visibili** gli errori di giudizio."
