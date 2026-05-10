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
├── 04-bibliografia.md             (paper accademici, Plaid, Wyvern, stato dell'arte)
├── 05-cinque-pilastri.md          (i 5 meccanismi del linguaggio + 15% irriducibile)
├── 06-strategia-prototipi.md      (decisioni pragmatiche, aliasing, piano prototipi)
├── 07-prossimi-passi.md           (gestione eccezioni, validazione codice reale)
├── 08-test-framework.md           (costrutto test/assert/expect_error/mock)
├── 09-loop-sviluppo.md            (ciclo INTENT→BENCHMARK→IMPLEMENT→SCORE→COMPARE)
├── STYLE.md                       (naming convention, lingue, builtin minimi)
├── db-design-patterns.md          (database: 47 design pattern con analisi)
└── db-antipatterns.md             (database: 46 antipattern con analisi)
```

## Stato del progetto (2026-05-10)

- **Fase 1 (Catalogazione):** ✅ completata — 93 pattern/antipattern classificati
- **Fase 2 (Analisi linguistica):** ✅ parziale — 6 territori con proposte di costrutto
- **Fase 2b (Critica pilastri):** 🔲 da fare
- **Fase 3 (Design core):** ✅ grammatica definita per tutti i prototipi
- **Fase 4 (Prototipo):** ✅ 6 prototipi funzionanti (p1, p2a, p2b, p2c, p3a, p3b)
- **Fase 5 (Valutazione):** ✅ parziale — scoring framework attivo, confronti in corso

### Risultati principali
- **p3b "state light"** è il prototipo più avanzato (score 55)
- Type-state verificato a compile-time: rifiuta use-before-open, use-after-close, double-close
- Codegen funzionante: HLL → Java → esecuzione → output identico al reference
- Test framework nativo nel linguaggio (`test`/`expect_error`)
- Ceremony ratio misurato: p3b ha 216% meno cerimonia di p3a
