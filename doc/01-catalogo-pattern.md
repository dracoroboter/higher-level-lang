# Fase 1: Catalogo Pattern e Antipattern

## Matrice di classificazione

Ogni pattern/antipattern è classificato in uno dei 4 quadranti:

|  | Formalizzabile (costrutto/regola) | Non formalizzabile |
|---|---|---|
| **Pattern** | A: Candidato per costrutto nativo | B: Resta libreria/idioma |
| **Antipattern** | C: Bloccabile da type system/grammar | D: Richiede lint/analisi statica |

---

## Design Pattern (GoF + moderni)

### Creazionali

| Pattern | Quadrante | Note |
|---------|-----------|------|
| **Singleton** | A | Costrutto `unique` o `service`. Rischio: incentivare global state |
| **Factory Method** | A | Costruttori nominati + sealed types |
| **Abstract Factory** | B | Troppo context-dependent, meglio come modulo |
| **Builder** | A | Named parameters + default values lo rendono superfluo |
| **Prototype** | B | Clone/copy semantics nel type system (come Rust `Clone`) |

### Strutturali

| Pattern | Quadrante | Note |
|---------|-----------|------|
| **Adapter** | B | È un problema di interop, non di linguaggio |
| **Decorator** | A | Composizione di comportamenti, possibile con `wrap` o trait stacking |
| **Facade** | B | È organizzazione del codice, non un costrutto |
| **Proxy** | A | Intercettazione trasparente, possibile come costrutto |
| **Composite** | A | Tipi ricorsivi + pattern matching |

### Comportamentali

| Pattern | Quadrante | Note |
|---------|-----------|------|
| **Observer** | A | `event`/`signal` nativo (come Go channels ma tipizzato) |
| **Strategy** | A | First-class functions + type classes |
| **Command** | A | Reificazione delle azioni, possibile come tipo nativo |
| **State Machine** | A | `state` come costrutto con transizioni verificate a compile-time |
| **Iterator** | A | Generatori/lazy sequences nativi |
| **Visitor** | A → scompare | Pattern matching + sum types lo eliminano |
| **Chain of Responsibility** | A | Pipeline/middleware come costrutto |
| **Mediator** | B | Troppo architetturale |
| **Template Method** | A → scompare | Trait con default methods |

### Pattern moderni

| Pattern | Quadrante | Note |
|---------|-----------|------|
| **Dependency Injection** | A | `inject`/`provide` come costrutti del modulo system |
| **Circuit Breaker** | A | Costrutto `resilient` con policy dichiarativa |
| **Retry** | A | Costrutto `retry` con backoff configurabile |
| **CQRS** | B | Architetturale, non formalizzabile in un costrutto |
| **Event Sourcing** | B | Pattern di persistenza, non di linguaggio |

---

## Antipattern

### Bloccabili dal linguaggio (Quadrante C)

| Antipattern | Meccanismo di blocco |
|-------------|---------------------|
| **Null pointer** | Nullable types obbligatori (come Kotlin/Rust) |
| **Data races** | Ownership system (come Rust) |
| **God Object** | Limite di responsabilità per tipo (max N metodi? max N dipendenze?) |
| **Mutable global state** | Nessun `var` globale; stato solo in `service` espliciti |
| **Callback hell** | Async/await nativo, no callback raw |
| **Stringly-typed** | Tipi nominali obbligatori per domini (no `String` generico per email, URL, etc.) |
| **Primitive obsession** | Newtype a costo zero (come Rust/Haskell) |
| **Deep inheritance** | No ereditarietà classica; solo composizione + trait |
| **Circular dependencies** | Module system che rifiuta cicli a compile-time |
| **Temporal coupling** | Type-state pattern nativo: metodi disponibili solo in certi stati |

### Difficili da bloccare (Quadrante D)

| Antipattern | Perché è difficile |
|-------------|-------------------|
| **Spaghetti code** | Problema di struttura, non di tipi |
| **Copy-paste programming** | Rilevabile solo con analisi statica avanzata |
| **Magic numbers** | Lint, non grammar |
| **Feature envy** | Richiede analisi semantica del dominio |
| **Shotgun surgery** | Problema architetturale |
| **Golden hammer** | Problema umano, non tecnico |

---

## Osservazioni dalla catalogazione

1. **Pattern che scompaiono**: Visitor, Template Method, Iterator — non servono costrutti dedicati, basta un linguaggio sufficientemente espressivo (sum types, generatori, trait).

2. **Pattern che diventano costrutti**: Observer, State Machine, DI, Circuit Breaker — questi hanno una semantica chiara e ripetitiva che beneficia di un costrutto dedicato.

3. **Antipattern più promettenti da bloccare**: Null, data races, circular deps, deep inheritance, mutable global state — tutti già dimostrati possibili da linguaggi esistenti.

4. **Territorio inesplorato**: God Object (limiti strutturali), Temporal Coupling (type-state nativo), Stringly-typed (tipi nominali obbligatori).

## Prossimi passi

→ Fase 2: Per i pattern in quadrante A, definire come sarebbe la sintassi concreta del costrutto.
