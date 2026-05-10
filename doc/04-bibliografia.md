# Bibliografia e stato dell'arte

## Paper fondamentali

---

### 1. Strom & Yemini — "Typestate: A Programming Language Concept for Enhancing Software Reliability" (1986)

**Riferimento:** R.E. Strom, S. Yemini. IEEE Transactions on Software Engineering, vol. 12, no. 1, pp. 157-171, January 1986.
**DOI:** 10.1109/TSE.1986.6312929
**PDF:** https://www.cs.cmu.edu/~aldrich/papers/classic/tse12-typestate.pdf

#### Contenuto chiave

Introduce il concetto di **typestate** come raffinamento del tipo:
- Il **tipo** determina l'insieme delle operazioni *mai* permesse su un oggetto
- Il **typestate** determina il sottoinsieme di operazioni permesse *in un contesto specifico*

**Formalizzazione:**
- I typestate di un tipo formano un **semireticolo** (meet-semilattice) con ordine parziale
- Ogni operazione ha una **transizione di typestate**: richiede un typestate in input e garantisce un typestate in output
- Ai punti di convergenza del flusso di controllo (dopo if/else), il typestate è il **greatest lower bound** dei typestate dei rami
- Esiste un algoritmo **lineare** per verificare la typestate-consistency di un programma

**Esempio canonico:** variabile `int` con typestate "uninitialized" < "initialized". Una variabile `FILE*` con typestate "unallocated" < "allocated" < "opened".

**Sfide identificate:**
- **Aliasing:** se due riferimenti puntano allo stesso oggetto, un cambio di stato via un riferimento deve riflettersi sull'altro. Problema difficile in generale.
- **Loop:** lo stato deve essere invariante all'ingresso del loop, il che può richiedere down-coercion che perde informazione.

**Rilevanza per noi:** È la base teorica del nostro costrutto `state`. La formalizzazione come semireticolo e l'algoritmo lineare dimostrano che la verifica è fattibile ed efficiente. Le sfide (aliasing, loop) sono ancora aperte e rilevanti.

---

### 2. Aldrich, Sunshine et al. — "Foundations of Typestate-Oriented Programming" (2014)

**Riferimento:** R. Garcia, É. Tanter, R. Wolff, J. Aldrich. ACM TOPLAS, 2014.
**URL:** https://kilthub.cmu.edu/articles/journal_contribution/Foundations_of_Typestate-Oriented_Programming/6605861
**Linguaggio:** Plaid (CMU, 2009-2014)

#### Contenuto chiave

Propone il **Typestate-Oriented Programming (TSOP)** come paradigma:
- Gli oggetti non hanno solo una classe — hanno uno **stato** che può cambiare a runtime
- Lo stato determina quali metodi sono disponibili
- Il cambio di stato può cambiare la **rappresentazione** dell'oggetto (non solo i metodi)
- Il type system verifica staticamente che i protocolli siano rispettati

**Plaid** implementa queste idee:
- `state` è un costrutto di primo livello
- Gli oggetti cambiano classe dinamicamente (state change operator)
- Il sistema di **permessi** (permissions) gestisce l'aliasing: unique, immutable, shared, none
- Il type system garantisce **protocol fidelity** a compile-time

**Plaidcore** è il calcolo formale sottostante:
- Usa stati e permessi per garantire staticamente che i client usino i protocolli correttamente
- Dimostrazione di soundness del type system

**Perché non ha avuto successo:**
- Sintassi complessa
- Curva di apprendimento alta
- Nessun ecosistema
- Rimasto puramente accademico

**Rilevanza per noi:** È il precedente più diretto. Dimostra che l'idea è formalizzabile e sound. Le lezioni da imparare: serve sintassi semplice, serve pragmatismo, serve un ecosistema. Il nostro approccio dovrebbe essere più dichiarativo e meno invasivo di Plaid.

---

### 3. Bosch — "Design Patterns as Language Constructs" (1996)

**Riferimento:** J. Bosch. Journal of Object-Oriented Programming, November 1996.
**Anche:** "Language Support for Design Patterns" (1996, tech report)
**URL:** https://www.researchgate.net/publication/2356000_Design_Patterns_as_Language_Constructs

#### Contenuto chiave

Propone **LayOM** (Layered Object Model), un linguaggio OO esteso dove i design pattern sono costrutti nativi tramite **layer types**:

- **Adapter layer:** traduce messaggi tra interfacce incompatibili
- **Bridge layer:** specifica per ogni messaggio quale oggetto.metodo lo implementa
- **Observer layer:** notifica automaticamente gli osservatori su cambiamento di stato
- **Strategy layer:** seleziona l'implementazione in base a criteri

**Architettura:**
- LayOM estende il modello a oggetti con: stati, categorie, e **layers**
- I layer intercettano i messaggi e li processano (simile a middleware/AOP)
- Il linguaggio è **estensibile**: nuovi pattern possono essere aggiunti come nuovi layer types
- LayOM transpila a C++

**Conclusioni di Bosch:**
- I pattern strutturali e comportamentali si prestano bene a diventare costrutti
- I pattern creazionali sono più difficili (troppo context-dependent)
- Il supporto linguistico riduce il codice boilerplate del 50-80%
- Ma: troppi costrutti rendono il linguaggio complesso

**Rilevanza per noi:** È esattamente la nostra domanda di ricerca, posta 30 anni fa. LayOM dimostra la fattibilità ma anche i limiti: l'approccio a layer è potente ma aggiunge complessità. La nostra idea di type-state come primitivo unificante è diversa dall'approccio di Bosch (che usa layer separati per ogni pattern).

---

### 4. Baumgartner, Läufer, Russo — "On the Interaction of Object-Oriented Design Patterns and Programming Languages" (1996/2019)

**Riferimento:** G. Baumgartner, K. Läufer, V.F. Russo. Purdue University Technical Report 96-020.
**arXiv:** https://arxiv.org/abs/1905.13674

#### Contenuto chiave

Analizza come i design pattern sono **distorti o complicati** dalla mancanza di costrutti nel linguaggio. Propone costrutti general-purpose che semplificherebbero i pattern:

**Costrutti proposti:**
1. **Subtyping separato dall'ereditarietà** — permette di avere interfacce senza gerarchie di classi
2. **Closure objects lessicalmente scoped** — indipendenti dalle classi (elimina Command, Strategy come pattern)
3. **Multimethod dispatch** — elimina il Visitor pattern

**Tesi centrale:** i pattern non devono diventare costrutti specifici (come fa Bosch). Piuttosto, servono **costrutti generali** che rendono i pattern triviali da implementare. La differenza è:
- Bosch: un costrutto per ogni pattern (Observer layer, Adapter layer...)
- Baumgartner et al.: pochi costrutti generali che rendono molti pattern superflui

**Rilevanza per noi:** Conferma la nostra osservazione che molti pattern "scompaiono" con un linguaggio espressivo. Il nostro approccio è un ibrido: costrutti generali (type-state, first-class functions, sum types) + pochi costrutti specifici per pattern universali (circuit breaker, retry).

---

### 5. Padovani — "Deadlock-Free Typestate-Oriented Programming" (2018)

**Riferimento:** L. Padovani (Università di Torino). The Art, Science, and Engineering of Programming, 2018, Vol. 2, Issue 3.
**arXiv:** https://arxiv.org/abs/1803.10670

#### Contenuto chiave

Estende il TSOP alla **concorrenza**, garantendo non solo protocol fidelity ma anche **assenza di deadlock** a compile-time.

**Ingredienti chiave:**
- **Behavioral types** per specificare e enforced i protocolli degli oggetti
- **Dependency relations** per rappresentare dipendenze tra oggetti e rilevare circolarità
- **Join patterns** come astrazione di sincronizzazione ad alto livello

**Proprietà:**
- **Composizionale:** gli oggetti si type-checkano in isolamento
- **Scalabile:** la deadlock-freedom di una composizione dipende solo dai tipi degli oggetti composti
- **Modulare:** modifiche interne che non cambiano l'interfaccia pubblica non impattano altri oggetti
- Dimostrazione di **soundness** del type system
- Implementazione in Haskell disponibile

**Rilevanza per noi:** Dimostra che il type-state può estendersi alla concorrenza con garanzie forti. Il nostro linguaggio potrebbe garantire non solo "open senza close" ma anche "nessun deadlock" — tutto a compile-time. La composizionalità è cruciale per la scalabilità.

---

### 6. Typestate in Rust — Stanford CS242 Lecture Notes (2019)

**Riferimento:** Stanford CS242: Programming Languages, Fall 2019. Lecture: Typestate.
**URL:** https://stanford-cs242.github.io/f19/lectures/08-2-typestate

#### Contenuto chiave

Mostra come implementare il typestate pattern **manualmente** in Rust usando il type system esistente:

**Tecnica:**
- Ogni stato è un tipo diverso (`File<Reading>`, `File<Eof>`)
- I metodi consumano `self` (ownership) → impediscono il riuso dello stato precedente
- I metodi restituiscono il nuovo tipo → forzano la transizione
- `PhantomData<S>` per parametrizzare un tipo sullo stato senza costo runtime
- Metodi condivisi implementati su `impl<S> File<S>` (generici su tutti gli stati)

**Errori prevenuti:**
1. Starting in a non-start state → costruttori privati
2. Invalid transition on invalid operation → Result types
3. Incorrect transition → tipi diversi per ogni stato
4. Reusing old states → ownership (move semantics)
5. Stopping before accept state → Drop trait

**Limiti in Rust:**
- Verboso: devi creare manualmente un tipo per ogni stato
- Non dichiarativo: la state machine è implicita nella struttura dei tipi
- Il programmatore deve "inventarsi" il pattern ogni volta

**Rilevanza per noi:** Dimostra che il typestate funziona in pratica con zero costo runtime. Ma conferma anche che senza supporto nativo è troppo verboso. Il nostro costrutto `state` è esattamente ciò che manca a Rust: una dichiarazione esplicita della state machine che il compilatore verifica automaticamente.

---

## Sintesi: cosa sappiamo dalla letteratura

| Domanda | Risposta dalla letteratura |
|---|---|
| Il typestate è formalizzabile? | SÌ — Strom & Yemini (1986), algoritmo lineare |
| È implementabile come paradigma? | SÌ — Plaid (CMU), ma troppo complesso per l'industria |
| I pattern possono essere costrutti? | SÌ — Bosch/LayOM, riduce boilerplate 50-80% |
| Servono costrutti specifici o generali? | ENTRAMBI — Baumgartner dice generali, Bosch dice specifici. Noi: ibrido |
| Si estende alla concorrenza? | SÌ — Padovani (2018), deadlock-free a compile-time |
| Funziona in pratica? | SÌ — Rust dimostra zero-cost, ma serve supporto nativo |
| Perché nessuno l'ha fatto? | Complessità (Plaid), nicchia (Eiffel), mancanza ecosistema |

## Gap nella letteratura (dove il nostro progetto è originale)

1. **Nessuno ha combinato** typestate + tipi nominali obbligatori + vincoli strutturali in un unico linguaggio
2. **Nessuno ha reso il typestate dichiarativo** con sintassi semplice (Plaid era complesso, Rust è manuale)
3. **Il log inerente** come costrutto del linguaggio non ha precedenti accademici
4. **La distinzione formale Singleton/Global State** non è stata formalizzata
5. **L'obbligo** di tipi nominali nelle interfacce pubbliche non è stato proposto

## Riferimenti aggiuntivi da esplorare

- DeLine & Fähndrich, "Typestates for Objects" (ECOOP 2004) — typestate per C#
- Bierhoff & Aldrich, "Modular Typestate Checking of Aliased Objects" (OOPSLA 2007) — risolve il problema dell'aliasing
- Coblenz, "The Obsidian Programming Language" (CMU) — typestate per smart contracts
- Session Types (Honda, 1993) — protocolli di comunicazione tipizzati
- "Typestate via Revocable Capabilities" (arXiv 2510.08889, 2025) — approccio recente basato su capabilities

## Decisioni di design informate dalla letteratura

### Magic Numbers: non un antipattern separato

**Fonti:**
- Martin, R.C. "Clean Code" (2008), Cap. 17 "Smells and Heuristics" — G25: "Replace Magic Numbers with Named Constants"
- SE Stack Exchange, "Magic numbers, locality and readability" (2016) — dibattito su costanti single-use
- SE Stack Exchange, "Eliminating Magic Numbers: When is it time to say No?" — consenso: il contesto determina se un literal è "magic"

**Decisione:** In HLL, i magic numbers non sono un antipattern separato perché sono già coperti dai **tipi nominali obbligatori** (Primitive Obsession). Se una funzione accetta `Int` dove dovrebbe accettare `Port` o `Timeout`, il tipo nominale cattura il problema. Creare costanti per literal usati una sola volta è considerato un antipattern a sua volta ("useless constant"). Il vero problema non è il literal ma l'assenza di significato nel tipo.

### Assenza di ereditarietà: problemi aperti

**Fonti:**
- Pike, R. "Go at Google: Language Design in the Service of Software Engineering" (2012) — motivazioni per l'assenza di ereditarietà in Go
- Matsakis, N. & Klock, F. "The Rust Language" (ACM SIGAda Ada Letters, 2014) — trait come alternativa all'ereditarietà
- Gamma, E. et al. "Design Patterns" (1994), p. 20 — "Favor object composition over class inheritance"
- LWN.net, "Go and Rust — objects without class" (2013) — analisi comparativa
- Stepanov, A. "Elements of Programming" (2009) — distinzione valori/oggetti

**Contesto:** HLL non ha ereditarietà. Questo elimina 3 antipattern (Deep Inheritance, Yo-Yo Problem, Fragile Base Class) ma introduce 3 problemi aperti:

**1. Verbosità nella delega**

Senza ereditarietà, il riuso di codice richiede composizione esplicita. Se un tipo vuole "ereditare" 10 metodi da un altro, deve delegare manualmente ciascuno. Go risolve con l'embedding (promozione automatica dei metodi). Rust risolve con trait default methods + derive macro.

*Possibili soluzioni per HLL:*
- `delegate` keyword: `struct MyService { delegate BaseService base }` — promuove automaticamente i metodi di `base`
- Default methods nei service: `service Logger { function log(String msg) -> Unit { println(msg) } }`
- Composizione con forwarding automatico

**2. Polimorfismo limitato**

Il costrutto `service` fornisce polimorfismo a livello di injection (un `provide` diverso per ambiente diverso). Ma manca il polimorfismo **ad-hoc** (trattare tipi diversi in modo uniforme senza un service). Es: una funzione che accetta "qualsiasi cosa con un metodo `toString()`".

*Possibili soluzioni per HLL:*
- Trait/interface con implementazione: `trait Printable { function toString() -> String }`
- Structural typing (come Go): se un tipo ha i metodi giusti, soddisfa l'interfaccia implicitamente
- Bounded generics: `function print<T: Printable>(T item) -> Unit`

**3. Interoperabilità con framework/librerie**

I framework Java/C# si basano su "estendi questa classe base" (HttpServlet, Activity, TestCase). Senza ereditarietà, HLL non può interoperare direttamente con questi framework. Il transpile a Java mitiga il problema (il codice generato può usare ereditarietà) ma il programmatore HLL non ha accesso diretto al meccanismo.

*Possibili soluzioni per HLL:*
- Wrapper/adapter generati automaticamente dal compilatore
- `extern class` per dichiarare classi Java da estendere nel codice generato
- Limitare il target a librerie che usano composizione (Spring DI, Dagger)

**Stato:** Problema aperto. Da affrontare nel livello 5 o come variante di p4a.

### Testing di errori runtime: `expect_fail`

**Fonti:**
- Claessen, K. & Hughes, J. "QuickCheck: A Lightweight Tool for Random Testing of Haskell Programs" (ICFP 2000) — property-based testing, generazione automatica di input
- Kotlin: `assertThrows<ExceptionType> { code }` — verifica che un blocco lanci un'eccezione specifica a runtime
- Rust: `#[should_panic(expected = "message")]` — attributo su test che deve paniccare
- Swift Testing: `#expect(throws: ErrorType.self) { code }` — verifica errore runtime
- Kotest (Kotlin): `shouldThrow<T> { block }` — DSL per asserzioni su eccezioni

**Problema in HLL:** `expect_error` verifica solo errori del type checker (compile-time). Non può testare business logic che usa `fail` a runtime (es. "amount deve essere positivo"). Serve un costrutto che esegua il codice e verifichi che produca un `fail` specifico.

**Proposta per HLL:**
```hll
test "invalid amount fails" {
    expect_fail ValidationError {
        let svc = spawn OrderService
        svc.createOrder(Email("a@b.com"), Product("X"), Amount(0))
    }
}
```

`expect_fail ErrorType { block }` = esegui il blocco, verifica che produca `fail ErrorType`. Se non fallisce o fallisce con un errore diverso → test fallito.

---

## Plaid: risorse disponibili per la ricerca

### Codice sorgente

**Repository:** https://github.com/plaidgroup/plaid-lang (2,618 commit, open source)

Struttura del progetto:
- `parser/` — parser del linguaggio (grammatica concreta)
- `ast/` — definizione dell'Abstract Syntax Tree
- `typechecker/` — type checker (verifica dei protocolli typestate)
- `codegenerator/` — generazione codice target JVM
- `compilerjava/` — compilatore completo
- `runtime/` — runtime (Java + JavaScript)
- `stdlib/` — libreria standard
- `tests/` — test suite (programmi validi e invalidi)
- `docs/` — documentazione e specifica

Scritto in Java (67%). Compila verso JVM.

### Specifica formale

1. **Draft specification** del core dinamicamente tipizzato (nella cartella `docs/`)
2. **"Typed Core Specification"** — report tecnico DTIC ADA558841: formalizza Plaidcore
3. **"First-Class State Change in Plaid"** (OOPSLA 2011) — semantica formale del modello a oggetti
4. **"Foundations of Typestate-Oriented Programming"** (TOPLAS 2014) — calcolo formale + soundness proof

### Stato del progetto

**Il progetto è inattivo dal ~2014.** Il gruppo di Aldrich si è spostato su **Wyvern** (vedi sotto). Il codice è disponibile ma non mantenuto.

### Approccio per la nostra ricerca

Non serve reverse engineering. Le opzioni sono:
1. **Studiare i paper** per la semantica formale (Plaidcore)
2. **Studiare il typechecker** nel codice per i dettagli implementativi
3. **Studiare il parser** per capire la grammatica concreta
4. **Reimplementare da zero** con sintassi più semplice e la stessa semantica sottostante

---

## Wyvern: il successore di Plaid

**Wyvern** è il linguaggio su cui il gruppo di Aldrich (CMU) lavora dopo Plaid. Ha cambiato direzione rispetto a Plaid.

**URL:** https://wyvernlang.github.io/
**Repository:** https://github.com/wyvernlang

### Cosa è

Wyvern è un linguaggio OO puro, staticamente tipizzato, progettato per applicazioni web e mobile. Le sue caratteristiche principali sono:

1. **Type-Specific Languages (TSL):** il tipo di un'espressione determina quale sotto-linguaggio viene usato per parsarla. Es. se il tipo atteso è `HTML`, puoi scrivere HTML inline; se è `SQL`, scrivi SQL. Il compilatore sa quale parser usare in base al tipo.

2. **Capability-safe modules:** il module system controlla quali risorse (file, rete, etc.) un modulo può accedere. Sicurezza by construction.

3. **Architectural control:** il linguaggio enforced vincoli architetturali (chi può dipendere da chi).

4. **Basato su DOT (Dependent Object Types):** il calcolo sottostante è DOT, lo stesso di Scala 3. Più espressivo dei type system tradizionali.

### Cosa NON ha (rispetto a Plaid)

Wyvern **non ha typestate**. Il gruppo ha abbandonato il typestate come paradigma centrale, spostandosi verso:
- Type-specific languages (estensibilità sintattica)
- Capability safety (sicurezza)
- Architettura controllata

### Perché hanno abbandonato il typestate?

Non è documentato esplicitamente, ma le ragioni probabili sono:
- Il typestate era troppo complesso per gli utenti (curva di apprendimento)
- Il problema dell'aliasing rendeva il type system troppo restrittivo
- Il gradual typestate (tipizzazione graduale) non era abbastanza pratico
- Il focus si è spostato su problemi più "industriali" (web, sicurezza, multi-linguaggio)

### Rilevanza per noi

1. **Wyvern conferma che il typestate puro è difficile da rendere pratico** — anche i suoi creatori l'hanno abbandonato
2. **Ma le ragioni non invalidano l'idea** — il problema era l'implementazione (troppo invasivo, troppo complesso), non il concetto
3. **Il nostro approccio è diverso:** typestate dichiarativo e opzionale (solo dove serve), non come paradigma totale
4. **Le TSL di Wyvern sono interessanti:** l'idea che il tipo determini la sintassi è collegata ai nostri tipi nominali obbligatori
5. **Il capability-safe module system** è collegato al nostro vincolo di injection obbligatoria e limiti di accoppiamento
