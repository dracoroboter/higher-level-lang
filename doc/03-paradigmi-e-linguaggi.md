# Paradigmi di programmazione e analisi dei linguaggi nel mondo reale

## 1. I paradigmi di programmazione

### Tassonomia

```
Programmazione
├── Imperativa (come fare)
│   ├── Procedurale (C, Pascal, Fortran)
│   ├── Object-Oriented (Java, C#, C++, Python)
│   └── Scripting (Bash, Perl, Python)
├── Dichiarativa (cosa fare)
│   ├── Funzionale (Haskell, Erlang, Lisp, ML)
│   ├── Logica (Prolog, Datalog) ← sistemi esperti
│   ├── Query (SQL)
│   └── Reactive (Elm, RxJS)
└── Multi-paradigma (la maggior parte dei linguaggi moderni)
    └── Python, Rust, Scala, Kotlin, TypeScript, C++
```

### Caratteristiche di ogni paradigma

| Paradigma | Principio | Punto di forza | Punto di debolezza |
|---|---|---|---|
| **Imperativo/Procedurale** | Sequenza di istruzioni che modificano lo stato | Vicino alla macchina, performance, controllo | Stato mutabile → bug, difficile da parallelizzare |
| **Object-Oriented** | Oggetti con stato + comportamento, ereditarietà | Modellazione del dominio, riuso, incapsulamento | Complessità accidentale, pattern come workaround, ereditarietà fragile |
| **Funzionale** | Funzioni pure, immutabilità, composizione | Ragionamento formale, parallelismo, testabilità | Curva di apprendimento, performance (allocazioni), IO scomodo |
| **Logico** | Fatti + regole → inferenza | Sistemi esperti, constraint solving, AI simbolica | Nicchia, performance imprevedibile, difficile per I/O e UI |
| **Reactive** | Flussi di dati e propagazione dei cambiamenti | UI, sistemi event-driven | Debugging difficile, curva di apprendimento |

---

## 2. Perché un linguaggio domina: i fattori reali

Non è (quasi mai) la qualità tecnica a determinare il successo. I fattori sono:

| Fattore | Descrizione | Esempio |
|---|---|---|
| **Monopolio di nicchia** | È l'unico modo per fare X | JavaScript (browser), SQL (database), COBOL (mainframe bancari) |
| **Vicinanza alla macchina** | Performance e controllo hardware | C, C++, Assembly (OS, embedded, gaming) |
| **Ecosistema/tradizione** | Parco software esistente, librerie, community | Java (enterprise), Python (data science/ML) |
| **Multi-piattaforma** | Write once, run anywhere | Java (JVM), Python (interpretato) |
| **Riduzione problemi specifici** | Elimina classi di bug | Kotlin (NPE), Rust (memory safety), Go (concorrenza semplice) |
| **Facilità di apprendimento** | Bassa barriera d'ingresso | Python, JavaScript |
| **Backing aziendale** | Supporto di un'azienda dominante | C# (Microsoft), Go (Google), Swift (Apple), Kotlin (JetBrains/Google) |
| **Inerzia/costo di migrazione** | Troppo costoso riscrivere | COBOL, Fortran, C++ |

---

## 3. Top 10 linguaggi: pro e contro nel mondo reale

Classifica composita basata su TIOBE (Feb 2026), Stack Overflow Developer Survey 2025, e GitHub Octoverse 2025.

### 1. Python
- **Paradigma:** Multi (imperativo, OO, funzionale)
- **Perché domina:** Ecosistema ML/AI sterminato, facilità di apprendimento, versatilità
- **Pro:** Leggibilità, librerie (NumPy, TensorFlow, Django), prototipazione rapida
- **Contro:** Lento (interpretato), GIL (concorrenza limitata), tipizzazione debole (duck typing → bug a runtime), indentazione come sintassi
- **Fattore dominante:** Ecosistema + facilità + monopolio AI/data science

### 2. JavaScript / TypeScript
- **Paradigma:** Multi (imperativo, funzionale, event-driven, prototipale)
- **Perché domina:** Monopolio browser, Node.js per backend, ubiquità
- **Pro:** Ovunque (browser, server, mobile, desktop), ecosistema npm enorme, async nativo
- **Contro:** Linguaggio nato male (coercion, `this`, `==` vs `===`), frammentazione framework, dependency hell. TypeScript mitiga ma non risolve.
- **Fattore dominante:** Monopolio di nicchia (browser) → espansione

### 3. C
- **Paradigma:** Imperativo/procedurale
- **Perché domina:** OS, kernel, embedded, performance critica
- **Pro:** Velocità massima, controllo totale, portabilità (compilatori ovunque), semplicità del linguaggio, tradizione 50+ anni
- **Contro:** Memory unsafety (buffer overflow, use-after-free, null), nessuna astrazione, gestione manuale di tutto
- **Fattore dominante:** Vicinanza alla macchina + inerzia (Linux, Windows kernel, firmware)

### 4. C++
- **Paradigma:** Multi (imperativo, OO, generico, funzionale parziale)
- **Perché domina:** Gaming, HPC, sistemi, quando serve C con astrazioni
- **Pro:** Performance + astrazioni (template, RAII), ecosistema enorme, zero-cost abstractions
- **Contro:** Complessità mostruosa (il linguaggio è enorme), memory unsafety, tempi di compilazione, UB ovunque
- **Fattore dominante:** Performance + inerzia + nessuna alternativa per gaming/HPC (fino a Rust)

### 5. Java
- **Paradigma:** OO (con funzionale aggiunto tardi)
- **Perché domina:** Enterprise, Android (storico), multi-piattaforma (JVM)
- **Pro:** GC (no memory management manuale), JVM matura e ottimizzata, ecosistema enterprise sterminato, backward compatibility
- **Contro:** Verboso, boilerplate, pattern come workaround (Factory, Builder, Visitor), startup lento, innovazione lenta
- **Fattore dominante:** Multi-piattaforma + ecosistema enterprise + inerzia

### 6. C#
- **Paradigma:** Multi (OO, funzionale, generico)
- **Perché domina:** Ecosistema Microsoft (.NET, Azure, Unity), enterprise Windows
- **Pro:** Linguaggio ben progettato (evolve bene), LINQ, async/await nativo, Unity per gaming, .NET cross-platform ora
- **Contro:** Storicamente legato a Windows, ecosistema meno open di Java, GC pause
- **Fattore dominante:** Backing aziendale (Microsoft) + Unity

### 7. Go
- **Paradigma:** Imperativo/procedurale con concorrenza nativa
- **Perché domina:** Cloud infrastructure, microservizi, CLI tools
- **Pro:** Semplicità radicale, goroutine/channels (concorrenza facile), compilazione veloce, binary statico, GC a bassa latenza
- **Contro:** No generics (aggiunti tardi e limitati), no eccezioni (error handling verboso), poco espressivo, no immutabilità
- **Fattore dominante:** Backing Google + risolve problema specifico (concorrenza semplice per backend)

### 8. Rust
- **Paradigma:** Multi (imperativo, funzionale, generico) con ownership
- **Perché domina:** Systems programming sicuro, sostituto di C/C++
- **Pro:** Memory safety senza GC, zero-cost abstractions, no null, no data races, pattern matching, Result/Option
- **Contro:** Curva di apprendimento ripida (borrow checker), tempi di compilazione, ecosistema ancora giovane per alcuni domini
- **Fattore dominante:** Risoluzione problema specifico (memory safety) + "most admired language" 8 anni consecutivi

### 9. SQL
- **Paradigma:** Dichiarativo (query)
- **Perché domina:** Monopolio accesso dati relazionali
- **Pro:** Dichiarativo (dici cosa vuoi, non come), standard universale, ottimizzatore decide l'esecuzione
- **Contro:** Non è general-purpose, dialetti incompatibili, difficile da comporre, injection se usato male
- **Fattore dominante:** Monopolio di nicchia (database relazionali)

### 10. Kotlin
- **Paradigma:** Multi (OO, funzionale)
- **Perché domina:** Android ufficiale, alternativa moderna a Java sulla JVM
- **Pro:** Null safety, conciso, coroutines, interop Java 100%, data classes, sealed classes
- **Contro:** Ecosistema proprio ancora piccolo (dipende da Java), compilazione più lenta di Java, adozione enterprise lenta
- **Fattore dominante:** Risoluzione problemi specifici (NPE, verbosità Java) + backing Google/JetBrains

---

## 4. Linguaggi notevoli fuori dalla top 10

Questi linguaggi non dominano per diffusione ma rappresentano linee di sviluppo storico fondamentali per la nostra ricerca.

### Lisp (1958)
- **Paradigma:** Funzionale, meta-programmazione, homoiconico
- **Perché è storico:** Secondo linguaggio di programmazione mai creato (dopo Fortran). Ha introdotto: garbage collection, funzioni come valori, ricorsione, REPL, macro, codice come dati (homoiconicità).
- **Pro:** Macro system potentissimo (il linguaggio si estende da solo), codice = dati (AST manipolabile), REPL-driven development, ha generato interi paradigmi (funzionale, AI simbolica)
- **Contro:** Sintassi a parentesi (barriera psicologica), frammentazione (Common Lisp, Scheme, Clojure, Racket), performance variabile, ecosistema frammentato
- **Fattore storico:** Ha dimostrato che un linguaggio può **estendersi da solo** tramite macro. L'idea che il programmatore possa creare nuovi costrutti è nata qui. Clojure (2007) ne è l'erede moderno sulla JVM.
- **Rilevanza per noi:** Se il nostro linguaggio ha costrutti per pattern, la domanda è: servono costrutti hardcoded nel compilatore, o un macro system che permette di *definire* nuovi costrutti? Lisp dice: dai all'utente il potere di estendere il linguaggio.

### Prolog (1972)
- **Paradigma:** Logico/dichiarativo
- **Perché è storico:** Il linguaggio dei sistemi esperti e dell'AI simbolica (anni '80). Approccio radicalmente diverso: non dici *come* calcolare, dichiari *cosa* è vero e il motore inferisce.
- **Pro:** Dichiarativo puro (fatti + regole → inferenza), backtracking automatico, unificazione, eccellente per constraint solving, parsing, AI simbolica
- **Contro:** Performance imprevedibile (backtracking esponenziale), difficile per I/O e side effects, debugging opaco ("perché ha scelto questo path?"), nicchia estrema
- **Fattore storico:** Ha dimostrato che un linguaggio può **ragionare** sul programma. Il compilatore non esegue solo — inferisce, verifica, deduce. Il type-checking avanzato (Haskell, Rust) è figlio di questa idea.
- **Rilevanza per noi:** Il nostro compilatore che "sa" quali transizioni di stato sono valide e rifiuta quelle invalide è concettualmente un motore di inferenza alla Prolog. La differenza è che Prolog inferisce a runtime, noi vogliamo farlo a compile-time.

### Haskell (1990)
- **Paradigma:** Funzionale puro, lazy
- **Perché è storico:** Il laboratorio dei type system avanzati. Quasi ogni innovazione nei tipi è nata o maturata qui.
- **Pro:** Type system potentissimo (type classes, GADTs, higher-kinded types), purezza (no side effects senza monadi), lazy evaluation, ragionamento equazionale
- **Contro:** Curva di apprendimento estrema, lazy evaluation rende la performance imprevedibile, ecosistema piccolo per produzione, "monad tutorial" come barriera culturale
- **Fattore storico:** Ha dimostrato che il type system può **catturare invarianti complessi** — effetti, stati, protocolli. Rust, Kotlin, TypeScript hanno tutti preso idee da Haskell.
- **Rilevanza per noi:** Il nostro type-state è essenzialmente un'idea haskelliana (tipi che cambiano in base al flusso). La domanda è: possiamo avere la potenza di Haskell senza la complessità?

### Erlang (1986) / Elixir (2011)
- **Paradigma:** Funzionale + modello ad attori
- **Perché è storico:** Progettato per sistemi telecom con uptime 99.9999999%. Ha introdotto "let it crash" + supervisori.
- **Pro:** Fault tolerance nativa, hot code reloading, concorrenza massiva (milioni di processi leggeri), isolamento totale tra processi
- **Contro:** Performance single-thread mediocre, sintassi ostica (Erlang), ecosistema piccolo
- **Fattore storico:** Ha dimostrato che la **resilienza può essere un costrutto del linguaggio**, non un'aggiunta. I supervisori sono Circuit Breaker + Retry nativi.
- **Rilevanza per noi:** Il nostro `circuit_breaker` e `retry` come costrutti sono esattamente l'idea di Erlang portata a livello di sintassi anziché di runtime.

### Eiffel (1986)
- **Paradigma:** OO con Design by Contract
- **Perché è storico:** Primo linguaggio con pre-condizioni, post-condizioni e invarianti come costrutti nativi.
- **Pro:** Correttezza verificabile, contratti come documentazione eseguibile, ereditarietà ben progettata
- **Contro:** Nicchia estrema, ecosistema minuscolo, troppo accademico per l'industria
- **Fattore storico:** Ha dimostrato che si possono mettere **vincoli di correttezza nel linguaggio stesso**. È il precursore diretto della nostra idea.
- **Rilevanza per noi:** Eiffel ha fallito commercialmente nonostante l'idea giusta. Lezione: la correttezza da sola non basta — serve anche ecosistema, ergonomia, e un problema doloroso che la gente vuole risolvere.

### Idris / Agda
- **Paradigma:** Funzionale con tipi dipendenti
- **Perché è rilevante:** Dependent types = i tipi possono dipendere dai valori. Puoi esprimere "un array di lunghezza N" o "una lista ordinata" nel type system.
- **Pro:** Prove a compile-time, massima espressività dei tipi, programmi corretti per costruzione
- **Contro:** Estrema complessità, quasi inutilizzabile per software reale, compilazione lenta
- **Rilevanza per noi:** Rappresentano il "limite teorico" di cosa un type system può fare. Il nostro type-state è un sottoinsieme più pragmatico della stessa idea.

### Elm (2012)
- **Paradigma:** Funzionale reactive
- **Perché è rilevante:** Zero runtime exceptions. Se compila, non crasha.
- **Pro:** No null, no undefined, no runtime exceptions, messaggi di errore eccellenti, architettura forzata (TEA)
- **Contro:** Solo per frontend web, ecosistema piccolo, interop JS scomodo, sviluppo rallentato
- **Rilevanza per noi:** Dimostra che "se compila, funziona" è raggiungibile in pratica per un dominio specifico. Il trade-off è la restrizione del dominio.

---

### Sintesi storica: le linee di sviluppo

```
1958 Lisp ──────→ "il linguaggio si estende da solo" (macro)
1972 Prolog ────→ "il linguaggio ragiona" (inferenza)
1986 Erlang ────→ "il linguaggio è resiliente" (supervisori)
1986 Eiffel ────→ "il linguaggio verifica la correttezza" (contratti)
1990 Haskell ───→ "il type system cattura invarianti" (monadi, type classes)
2010 Rust ──────→ "il compilatore elimina classi di bug" (ownership)
2012 Elm ───────→ "se compila, non crasha" (no exceptions)
2015 Idris ─────→ "il tipo È la specifica" (dependent types)
20?? Nostro ────→ "pattern facili, antipattern impossibili" (type-state + vincoli)
```

Il nostro linguaggio è il passo successivo di questa linea evolutiva: prendere le idee di Eiffel (correttezza), Haskell (tipi espressivi), Rust (eliminazione bug), Erlang (resilienza) e unificarle in costrutti dichiarativi.

---

## 5. Pattern di giudizio: perché un linguaggio vince

```
Successo = f(monopolio_nicchia, ecosistema, backing, risoluzione_problemi, 
             facilità, performance, inerzia)
```

La qualità tecnica pesa poco da sola. I linguaggi "migliori" tecnicamente (Haskell, Eiffel, Elm) restano nicchia. I linguaggi dominanti hanno almeno 2-3 di questi fattori.

---

## 6. Implicazioni per il nostro linguaggio

### Cosa impariamo

1. **Senza un monopolio di nicchia o un ecosistema, un linguaggio non si diffonde.** Il nostro linguaggio è un progetto di ricerca — non compete con Python/JS. Ma se volesse diffondersi, dovrebbe:
   - Transpilare a un target esistente (JS, WASM, JVM) per ereditare l'ecosistema
   - Risolvere un problema così doloroso che la gente migra (come Rust per memory safety)

2. **I linguaggi che eliminano classi di bug hanno successo** (Rust, Kotlin, Go). Il nostro approccio (eliminare antipattern) è nella direzione giusta.

3. **La semplicità vince.** Go ha dimostrato che un linguaggio "stupido" ma semplice batte linguaggi potenti ma complessi. Troppi costrutti nativi sono un rischio.

4. **Multi-paradigma è lo standard.** Nessun linguaggio moderno di successo è mono-paradigma. Il nostro dovrebbe essere almeno imperativo + funzionale.

5. **Il paradigma logico (Prolog) è il precedente più vicino** alla nostra idea di "il linguaggio sa cosa è giusto e cosa è sbagliato". In Prolog dichiari fatti e regole, e il sistema inferisce. Analogamente, nel nostro linguaggio dichiari stati e transizioni, e il compilatore inferisce cosa è permesso.

### Cosa NON fare

- Non creare un linguaggio OO puro (i pattern GoF sono workaround all'OOP)
- Non ignorare la performance (deve almeno transpilare a qualcosa di efficiente)
- Non avere troppi costrutti (rischio "linguaggio da 1000 keyword")
- Non ignorare l'interop (deve poter chiamare codice esistente)

### Posizionamento

Il nostro linguaggio si posiziona come:
- **Paradigma:** Multi (imperativo + funzionale + type-state)
- **Fattore differenziante:** Correttezza by construction (antipattern impossibili)
- **Target:** Transpilato (WASM? JVM? LLVM?)
- **Nicchia potenziale:** Sistemi dove la correttezza è critica (fintech, healthcare, infrastruttura)
