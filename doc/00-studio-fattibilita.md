# Studio di Fattibilità: Linguaggio con Design Pattern Nativi

## Domanda di ricerca

È possibile costruire un linguaggio di programmazione in cui:
- I **design pattern** sono costrutti semplici del linguaggio stesso
- Gli **antipattern** sono resi difficili dalla formalità stessa del linguaggio

### Domande di secondo livello
- Ammesso che esista, è **utile**?
- È **efficiente**?

## Precedenti

| Linguaggio | Cosa formalizza | Meccanismo |
|---|---|---|
| Rust | Elimina data races, dangling pointers | Ownership + borrow checker |
| Eiffel | Design by Contract | Pre/post-conditions native |
| Kotlin | Null safety | Nullable types nel type system |
| Go | Producer-Consumer, CSP | Channels + goroutines nativi |
| Haskell | Separazione side effects | Monadi + type system |

## Osservazioni critiche

1. **Molti pattern sono workaround a limiti del linguaggio.** Visitor esiste perché manca il pattern matching. Iterator è superfluo con generatori nativi. Un linguaggio espressivo fa *scomparire* pattern anziché renderli costrutti.

2. **I pattern sono context-dependent.** Singleton è pattern o antipattern a seconda del contesto. Rendere un pattern "facile" rischia di incentivarne l'abuso.

3. **Trade-off espressività/complessità.** Troppi costrutti nativi rendono il linguaggio difficile da imparare.

4. **Efficienza.** Costrutti ad alto livello possono avere costi runtime nascosti. Serve decidere: compilato, interpretato, o transpilato?

## Piano di lavoro

| Fase | Output | Scopo |
|------|--------|-------|
| 1. Catalogazione | Pattern/antipattern classificati per formalizzabilità | Capire cosa è fattibile |
| 2. Analisi linguistica | Per ogni pattern: come sarebbe il costrutto? Per ogni antipattern: quale regola lo blocca? | Validare fattibilità |
| 2b. Critica dei pilastri | Per ogni pilastro: limiti, trade-off, tensioni con gli altri pilastri | Capire i costi |
| 3. Design del core | Grammatica minimale con 3-5 costrutti | Prototipo concreto |
| 4. Prototipo | Interprete o transpiler minimale | Proof of concept |
| 5. Valutazione | Confronto ergonomia/efficienza | Rispondere alle domande di secondo livello |

### Fase 2b: Critica dei pilastri (da sviluppare)

Per ogni pilastro, rispondere a:
1. **Limiti intrinseci:** cosa NON può fare questo pilastro? Dove fallisce?
2. **Costo per il programmatore:** quanta complessità aggiunge? Quanta libertà toglie?
3. **Costo per il compilatore:** quanto è complesso da implementare? È decidibile?
4. **Trade-off con gli altri pilastri:** dove due pilastri sono in tensione?
5. **Precedenti falliti:** chi ha provato e perché non ha funzionato?

Tensioni note da esplorare:
- Type-state vs semplicità (Plaid era troppo complesso)
- Tipi nominali obbligatori vs ergonomia (troppi tipi = boilerplate)
- Vincoli strutturali vs flessibilità (limiti arbitrari frustrano)
- Module system rigido vs prototipazione rapida (troppa struttura rallenta)
- No ereditarietà vs riuso del codice (la composizione è più verbosa)

## Struttura documentazione

```
doc/
├── 00-studio-fattibilita.md       (questo file — overview e piano)
├── 01-catalogo-pattern.md         (Fase 1: classificazione in 4 quadranti)
├── 02-territori-inesplorati.md    (6 territori di ricerca con proposte di costrutto)
├── 03-paradigmi-e-linguaggi.md    (paradigmi, top 10 linguaggi, linee storiche)
├── 04-bibliografia.md             (paper accademici, decisioni di design, problemi aperti)
├── 05-cinque-pilastri.md          (i 5 meccanismi del linguaggio + 15% irriducibile)
├── 06-strategia-prototipi.md      (decisioni pragmatiche, aliasing, piano prototipi)
├── 07-prossimi-passi.md           (gestione eccezioni, validazione codice reale)
├── 08-test-framework.md           (costrutto test/assert/expect_error/mock)
├── 09-loop-sviluppo.md            (ciclo INTENT→BENCHMARK→IMPLEMENT→SCORE→COMPARE)
├── 10-mapping-antipattern.md      (mapping completo: 46 antipattern → meccanismo HLL)
├── STYLE.md                       (naming convention, lingue, builtin minimi)
├── db-design-patterns.md          (database: 47 design pattern con analisi)
└── db-antipatterns.md             (database: 46 antipattern con analisi)
```

## Stato del progetto (2026-05-10)

- **Fase 1 (Catalogazione):** ✅ completata — 93 pattern/antipattern classificati
- **Fase 2 (Analisi linguistica):** ✅ parziale — 6 territori con proposte di costrutto
- **Fase 2b (Critica pilastri):** 🔲 da fare
- **Fase 3 (Design core):** ✅ grammatica definita per tutti i prototipi
- **Fase 4 (Prototipo):** ✅ 9 prototipi (p1, p2a, p2b, p2c, p3a, p3b, p4a, p5a, p5b). 6 attivi.
- **Fase 5 (Valutazione):** ✅ scoring framework attivo con `--strict` e pesi configurabili

### Risultati principali
- **p5b "iterate query"** è il prototipo più avanzato (score 62, 80% antipattern bloccati)
- Purezza delle clausole di iterazione verificata a compile-time (originalità)
- Compilatore multi-file con DAG check e risoluzione cross-module
- Module system: service/provide/needs/export + mock nativo per test
- Actor model: spawn/await con no-aliasing (previene race condition)
- Type-state verificato a compile-time (ereditato da p3b)
- Checked errors con `fails` (ereditato da p2c)
- 37/46 antipattern bloccati (29 testati + 8 by design)

### Prototipi attivi
| Proto | Score | Ruolo |
|---|---|---|
| p5b | 62 | Più avanzato — iterazione con purezza enforced |
| p5a | 59 | Livello 5 — iterazione con lambda |
| p4a | 54 | Livello 4 — module system + actor model |
| p3a | 52 | Livello 3 — typestate con rebinding |
| p3b | 49 | Vincitore L3 — typestate in-place |
| p2c | 40 | Vincitore L2 — checked errors |

### Decisioni di design prese
- Nessuna ereditarietà (composizione + service). Problemi aperti documentati.
- Newline come separatore (no `;`)
- `--strict` promuove warning a errori (usato nello scoring)
- Magic numbers non bloccati separatamente (coperti da tipi nominali)
- Concorrenza non ancora presente — decisione presa: **Actor Model** (vedi `doc/11-concorrenza.md`). Si integra con service/provide/needs. Bloccherà Race Condition, Deadlock, Busy Waiting.
- Ogni prototipo ha i propri test (inclusi quelli ereditati dai padri, nella propria sintassi)
