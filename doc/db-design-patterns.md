# Database Design Pattern

## Formato

Ogni pattern ha: nome, categoria, intento, problema che risolve, struttura essenziale, e analisi rispetto al linguaggio.

---

## GoF — Creazionali (5/5)

### Singleton
- **Intento:** Garantire che una classe abbia una sola istanza e fornire un punto di accesso globale.
- **Problema:** Risorse condivise (config, pool, registry) che devono esistere in una sola copia.
- **Struttura:** Costruttore privato + metodo statico `getInstance()`.
- **Riducibile a type-state?** No direttamente. È un vincolo di cardinalità (esattamente 1), non di transizione di stato. Serve un costrutto dedicato (`service`).

### Factory Method
- **Intento:** Definire un'interfaccia per creare oggetti, lasciando alle sottoclassi la decisione su quale classe istanziare.
- **Problema:** Il codice client non deve dipendere da classi concrete.
- **Struttura:** Metodo astratto che restituisce un'interfaccia; le sottoclassi implementano.
- **Riducibile a type-state?** No. È un problema di polimorfismo e decoupling. Scompare con sum types + costruttori nominati.

### Abstract Factory
- **Intento:** Fornire un'interfaccia per creare famiglie di oggetti correlati senza specificare le classi concrete.
- **Problema:** Coerenza tra oggetti di una famiglia (es. UI toolkit cross-platform).
- **Struttura:** Interfaccia factory con metodi per ogni prodotto; implementazioni concrete per famiglia.
- **Riducibile a type-state?** No. È un problema di coerenza tra tipi, non di stato. Serve un module system con vincoli di coerenza.

### Builder
- **Intento:** Separare la costruzione di un oggetto complesso dalla sua rappresentazione.
- **Problema:** Oggetti con molti parametri opzionali, costruzione step-by-step.
- **Struttura:** Oggetto builder con metodi chainabili + metodo `build()`.
- **Riducibile a type-state?** SÌ. Il builder è un caso classico di type-state: `Empty -> HasName -> HasEmail -> Complete`. Il metodo `build()` è disponibile solo nello stato `Complete`.

### Prototype
- **Intento:** Creare nuovi oggetti copiando un'istanza esistente.
- **Problema:** Creazione costosa o configurazione complessa da duplicare.
- **Struttura:** Metodo `clone()` sull'oggetto.
- **Riducibile a type-state?** No. È semantica di copia, non di stato. Serve un trait `Clone` nel type system.

---

## GoF — Strutturali (7/7)

### Adapter
- **Intento:** Convertire l'interfaccia di una classe in un'altra interfaccia attesa dal client.
- **Problema:** Incompatibilità tra interfacce esistenti.
- **Struttura:** Wrapper che traduce le chiamate.
- **Riducibile a type-state?** No. È un problema di interoperabilità tra tipi.

### Bridge
- **Intento:** Separare un'astrazione dalla sua implementazione in modo che possano variare indipendentemente.
- **Problema:** Esplosione combinatoria di sottoclassi (es. Shape × Color).
- **Struttura:** Astrazione contiene riferimento a implementazione via interfaccia.
- **Riducibile a type-state?** No. È composizione di dimensioni ortogonali. Serve un type system con trait/typeclass.

### Composite
- **Intento:** Comporre oggetti in strutture ad albero per rappresentare gerarchie parte-tutto.
- **Problema:** Trattare uniformemente oggetti singoli e composizioni.
- **Struttura:** Interfaccia comune per foglie e nodi; nodi contengono figli.
- **Riducibile a type-state?** No. È un tipo ricorsivo (sum type). Scompare con `enum` ricorsivi + pattern matching.

### Decorator
- **Intento:** Aggiungere responsabilità a un oggetto dinamicamente.
- **Problema:** Estendere comportamento senza ereditarietà.
- **Struttura:** Wrapper che implementa la stessa interfaccia e delega + aggiunge.
- **Riducibile a type-state?** Parzialmente. La catena di decoratori potrebbe essere vista come composizione di stati, ma è più naturale come composizione di funzioni/trait.

### Facade
- **Intento:** Fornire un'interfaccia semplificata a un sottosistema complesso.
- **Problema:** Ridurre l'accoppiamento tra client e sottosistema.
- **Struttura:** Classe che espone metodi semplici e coordina il sottosistema.
- **Riducibile a type-state?** No. È organizzazione del codice, non un problema di tipi.

### Flyweight
- **Intento:** Condividere oggetti per supportare grandi quantità di istanze efficientemente.
- **Problema:** Memoria: troppi oggetti simili.
- **Struttura:** Pool di oggetti condivisi + stato estrinseco passato dall'esterno.
- **Riducibile a type-state?** No. È un'ottimizzazione di memoria, non di correttezza.

### Proxy
- **Intento:** Fornire un surrogato che controlla l'accesso a un altro oggetto.
- **Problema:** Lazy loading, access control, logging trasparente.
- **Struttura:** Stessa interfaccia dell'oggetto reale, intercetta le chiamate.
- **Riducibile a type-state?** Parzialmente. Un proxy lazy ha stati (NotLoaded → Loaded). Ma il proxy di accesso è più un decorator.

---

## GoF — Comportamentali (11/11)

### Chain of Responsibility
- **Intento:** Evitare di accoppiare il mittente di una richiesta al suo ricevitore, dando a più oggetti la possibilità di gestirla.
- **Problema:** Chi gestisce una richiesta non è noto a priori.
- **Struttura:** Lista di handler; ogni handler decide se gestire o passare al successivo.
- **Riducibile a type-state?** No. È una pipeline/composizione di funzioni. Costrutto nativo: `pipeline`.

### Command
- **Intento:** Incapsulare una richiesta come oggetto, permettendo undo, queue, logging.
- **Problema:** Reificare le azioni per manipolarle (undo, replay, queue).
- **Struttura:** Interfaccia Command con `execute()` e opzionalmente `undo()`.
- **Riducibile a type-state?** SÌ. Un command ha stati: `Created -> Executed -> Undone`. `undo()` è disponibile solo dopo `execute()`.

### Interpreter
- **Intento:** Definire una rappresentazione della grammatica di un linguaggio e un interprete che usa la rappresentazione per interpretare frasi.
- **Problema:** Valutare espressioni o linguaggi domain-specific.
- **Struttura:** Albero sintattico con nodi che implementano `interpret()`; ogni nodo è un tipo di espressione.
- **Riducibile a type-state?** No. È un sum type ricorsivo (AST) + pattern matching. Scompare con enum ricorsivi e match expressions.

### Iterator
- **Intento:** Accedere sequenzialmente agli elementi di una collezione senza esporne la struttura.
- **Problema:** Attraversamento generico di strutture dati diverse.
- **Struttura:** Interfaccia con `hasNext()` / `next()`.
- **Riducibile a type-state?** Scompare. Con generatori/lazy sequences nativi non serve il pattern.

### Mediator
- **Intento:** Definire un oggetto che incapsula come un insieme di oggetti interagisce.
- **Problema:** Ridurre le dipendenze dirette tra componenti (N×N → N×1).
- **Struttura:** Oggetto centrale che coordina la comunicazione.
- **Riducibile a type-state?** No. È un pattern architetturale di decoupling.

### Memento
- **Intento:** Catturare lo stato interno di un oggetto per poterlo ripristinare.
- **Problema:** Undo/snapshot senza violare l'incapsulamento.
- **Struttura:** Oggetto opaco che contiene lo stato salvato.
- **Riducibile a type-state?** Parzialmente. Il ciclo `Normal -> Saved -> Restored` è type-state, ma il contenuto del memento è un problema di serializzazione.

### Observer
- **Intento:** Definire una dipendenza uno-a-molti: quando un oggetto cambia stato, tutti i dipendenti vengono notificati.
- **Problema:** Reazione a cambiamenti senza accoppiamento diretto.
- **Struttura:** Subject mantiene lista di observer; notifica su cambiamento.
- **Riducibile a type-state?** No direttamente. È un problema di comunicazione/eventi. Serve un costrutto `event`/`signal` nativo.

### State
- **Intento:** Permettere a un oggetto di alterare il suo comportamento quando il suo stato interno cambia.
- **Problema:** Comportamento che dipende dallo stato, senza catene di if/switch.
- **Struttura:** Interfaccia State con metodi; oggetto delega allo stato corrente.
- **Riducibile a type-state?** SÌ — è letteralmente il type-state pattern. È il caso più diretto.

### Strategy
- **Intento:** Definire una famiglia di algoritmi, incapsularli, e renderli intercambiabili.
- **Problema:** Scegliere un algoritmo a runtime senza if/switch.
- **Struttura:** Interfaccia con metodo; implementazioni concrete; il client sceglie.
- **Riducibile a type-state?** No. È polimorfismo puro. Scompare con first-class functions.

### Template Method
- **Intento:** Definire lo scheletro di un algoritmo, lasciando alle sottoclassi la ridefinizione di alcuni passi.
- **Problema:** Riuso della struttura con variazione dei dettagli.
- **Struttura:** Metodo nella classe base che chiama metodi astratti.
- **Riducibile a type-state?** No. Scompare con trait + default methods.

### Visitor
- **Intento:** Definire nuove operazioni su una struttura di oggetti senza modificare le classi.
- **Problema:** Aggiungere operazioni a una gerarchia chiusa.
- **Struttura:** Double dispatch: accept() + visit().
- **Riducibile a type-state?** No. Scompare con pattern matching + sum types.

---

## Pattern di concorrenza

### Active Object
- **Intento:** Disaccoppiare l'invocazione di un metodo dalla sua esecuzione, eseguendolo in un thread separato.
- **Problema:** Concorrenza senza esporre thread al chiamante.
- **Struttura:** Proxy + coda di richieste + scheduler + servant.
- **Riducibile a type-state?** Parzialmente. La richiesta ha stati `Queued -> Executing -> Completed`.

### Monitor Object
- **Intento:** Sincronizzare l'accesso a un oggetto garantendo mutua esclusione.
- **Problema:** Accesso concorrente sicuro a stato condiviso.
- **Struttura:** Lock implicito su ogni metodo dell'oggetto.
- **Riducibile a type-state?** Parzialmente. Lo stato `Locked | Unlocked` è type-state, ma la sincronizzazione è meglio gestita da ownership.

### Producer-Consumer
- **Intento:** Disaccoppiare la produzione di dati dal loro consumo tramite un buffer.
- **Problema:** Velocità diverse tra produttore e consumatore.
- **Struttura:** Coda condivisa con sincronizzazione.
- **Riducibile a type-state?** No. È un pattern di comunicazione. Serve un costrutto `channel` nativo (come Go).

### Read-Write Lock
- **Intento:** Permettere letture concorrenti ma scritture esclusive.
- **Problema:** Performance: le letture non si bloccano tra loro.
- **Struttura:** Lock con contatore lettori + flag scrittore.
- **Riducibile a type-state?** SÌ. Risorsa in stato `Free | Reading(n) | Writing`. `write()` disponibile solo in `Free`.

### Thread Pool
- **Intento:** Riusare un insieme fisso di thread per eseguire task.
- **Problema:** Costo di creazione/distruzione thread.
- **Struttura:** Coda di task + N worker thread.
- **Riducibile a type-state?** No. È gestione di risorse del runtime.

### Future/Promise
- **Intento:** Rappresentare un valore che sarà disponibile in futuro.
- **Problema:** Composizione di operazioni asincrone.
- **Struttura:** Oggetto con stati Pending → Resolved | Rejected.
- **Riducibile a type-state?** SÌ. `Pending -> Resolved(T) | Rejected(E)`. Il valore è accessibile solo in `Resolved`.

---

## Pattern di resilienza

### Circuit Breaker
- **Intento:** Evitare chiamate ripetute a un servizio che sta fallendo.
- **Problema:** Cascading failures in sistemi distribuiti.
- **Struttura:** Tre stati: Closed (normale) → Open (blocca) → HalfOpen (prova).
- **Riducibile a type-state?** SÌ. È letteralmente una state machine con tre stati e transizioni basate su contatori/timeout.

### Retry with Backoff
- **Intento:** Riprovare un'operazione fallita con attese crescenti.
- **Problema:** Errori transienti in rete/servizi.
- **Struttura:** Loop con delay esponenziale e max tentativi.
- **Riducibile a type-state?** Parzialmente. Gli stati sono `Trying(n) -> Success | Failed`, ma la logica di backoff è più una policy che uno stato.

### Bulkhead
- **Intento:** Isolare le risorse di un sistema in compartimenti per evitare che un fallimento si propaghi.
- **Problema:** Un componente lento/rotto consuma tutte le risorse del sistema.
- **Struttura:** Pool di risorse separati per ogni servizio/funzionalità.
- **Riducibile a type-state?** No. È un vincolo di allocazione risorse. Possibile come annotazione/policy dichiarativa.

### Timeout
- **Intento:** Limitare il tempo di attesa per un'operazione.
- **Problema:** Operazioni che si bloccano indefinitamente.
- **Struttura:** Wrapper che interrompe dopo N ms.
- **Riducibile a type-state?** Parzialmente. `Waiting -> Completed | TimedOut`. Ma è più naturale come costrutto `with timeout(N)`.

### Fallback
- **Intento:** Fornire un valore/comportamento alternativo quando l'operazione principale fallisce.
- **Problema:** Degradazione graceful.
- **Struttura:** Try principale + alternativa.
- **Riducibile a type-state?** No. È composizione di Result. Costrutto: `fallback`.

---

## Pattern architetturali

### Dependency Injection
- **Intento:** Fornire le dipendenze dall'esterno anziché crearle internamente.
- **Problema:** Testabilità, decoupling, configurabilità.
- **Struttura:** Costruttore/metodo riceve le dipendenze; container le risolve.
- **Riducibile a type-state?** No. È un problema di wiring/composizione. Serve `inject` come costrutto del module system.

### Repository
- **Intento:** Mediare tra il dominio e il data mapping usando un'interfaccia simile a una collezione.
- **Problema:** Disaccoppiare la logica di dominio dalla persistenza.
- **Struttura:** Interfaccia con metodi CRUD; implementazione concreta per storage.
- **Riducibile a type-state?** No. È un'astrazione di accesso dati. Scompare con trait + DI.

### Unit of Work
- **Intento:** Mantenere una lista di oggetti modificati durante una transazione e coordinare la scrittura.
- **Problema:** Consistenza transazionale senza flush espliciti.
- **Struttura:** Oggetto che traccia dirty/new/deleted e fa commit atomico.
- **Riducibile a type-state?** SÌ. `Active -> Committed | RolledBack`. Operazioni disponibili solo in `Active`.

### Event Sourcing
- **Intento:** Persistere lo stato come sequenza di eventi anziché come snapshot.
- **Problema:** Audit trail, replay, ricostruzione dello stato.
- **Struttura:** Event store + proiezioni.
- **Riducibile a type-state?** No. È un pattern di persistenza/architettura.

### CQRS
- **Intento:** Separare il modello di lettura da quello di scrittura.
- **Problema:** Ottimizzare letture e scritture indipendentemente.
- **Struttura:** Command model + Query model separati.
- **Riducibile a type-state?** No. È architetturale.

### Saga
- **Intento:** Gestire transazioni distribuite come sequenza di transazioni locali con compensazioni.
- **Problema:** Consistenza in sistemi distribuiti senza 2PC.
- **Struttura:** Sequenza di step; ogni step ha un'azione e una compensazione. Se uno step fallisce, si eseguono le compensazioni in ordine inverso.
- **Riducibile a type-state?** SÌ. Ogni step è uno stato: `Step1Done -> Step2Done -> ... -> Completed`. Fallimento in qualsiasi punto triggera la catena di compensazione. Il compilatore può verificare che ogni step abbia una compensazione.

### Specification
- **Intento:** Incapsulare logica di business in oggetti combinabili e riusabili.
- **Problema:** Regole di business complesse, composte, e testabili.
- **Struttura:** Oggetto con `isSatisfiedBy()` + operatori `and`, `or`, `not`.
- **Riducibile a type-state?** No. È composizione di predicati. Scompare con first-class functions + operatori.

### Outbox
- **Intento:** Garantire che un evento venga pubblicato se e solo se la transazione locale ha successo.
- **Problema:** Consistenza tra database locale e message broker.
- **Struttura:** Tabella outbox scritta nella stessa transazione; processo separato pubblica.
- **Riducibile a type-state?** Parzialmente. Messaggio in stato `Pending -> Published`. Ma è più infrastrutturale.

### Strangler Fig
- **Intento:** Migrare incrementalmente un sistema legacy sostituendo pezzi uno alla volta.
- **Problema:** Riscrittura big-bang troppo rischiosa.
- **Struttura:** Proxy/router che dirige traffico al vecchio o nuovo sistema.
- **Riducibile a type-state?** No. È una strategia di migrazione, non un pattern di codice.

### Service Locator
- **Intento:** Fornire un registro centralizzato per ottenere servizi.
- **Problema:** Risolvere dipendenze a runtime.
- **Struttura:** Registry globale con metodo `get(ServiceType)`.
- **Riducibile a type-state?** No. È l'anti-DI — spesso considerato antipattern. Nel nostro linguaggio è sostituito da `inject`.

---

## Pattern funzionali

### Monad
- **Intento:** Sequenziare computazioni con contesto (errore, stato, IO, async).
- **Problema:** Comporre operazioni che hanno effetti collaterali in modo puro.
- **Struttura:** `bind`/`flatMap` che passa il contesto tra operazioni.
- **Riducibile a type-state?** Parzialmente. Le monadi IO/State sono sequenze di stati. Ma il concetto è più generale (è un pattern di composizione).

### Functor / Applicative
- **Intento:** Applicare funzioni a valori in un contesto (Option, List, Result).
- **Problema:** Trasformare valori senza estrarre dal contesto.
- **Struttura:** `map` / `apply`.
- **Riducibile a type-state?** No. È un pattern di trasformazione, non di stato.

### Lens
- **Intento:** Accedere e modificare parti di strutture dati immutabili in modo componibile.
- **Problema:** Aggiornare campi nested in strutture immutabili è verboso.
- **Struttura:** Coppia getter/setter componibile.
- **Riducibile a type-state?** No. È un problema di accesso a dati immutabili. Possibile come costrutto di accesso (`path.to.field`).

### Type Class
- **Intento:** Definire comportamento polimorfico senza ereditarietà, risolvibile a compile-time.
- **Problema:** Ad-hoc polymorphism senza sottotipi.
- **Struttura:** Dichiarazione di interfaccia + implementazioni separate per tipo.
- **Riducibile a type-state?** No. È un meccanismo del type system (trait/typeclass).

---

## Statistiche

- **GoF completi:** 23/23
- **Pattern di concorrenza:** 6
- **Pattern di resilienza:** 5
- **Pattern architetturali:** 9
- **Pattern funzionali:** 4
- **TOTALE:** 47 design pattern
