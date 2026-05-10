# TODO List

## Priorità alta

- [x] **Fix bug NPE p2b** — risolto
- [x] **Fix parametri scambiati** — risolto per p2a/p2b/p2c
- [x] **Implementare verifica gestione errori** — p2a: Result consumato + match esaustivo, p2b: effects, p2c: fails + | handler
- [x] **Fix codegen** — while, assign, if, println, parse_int generano Java corretto. Output identico al reference oracle.
- [x] **Batteria di test del compilatore** — p2a: 10/10, p2b: 8/8, p2c: 8/8. Copre: null safety, Option access, tipi nominali (scambio, raw String, SQL injection), Result consumed, match exhaustive.

## Priorità media

- [ ] **Naming convention** — decisione presa: **camelCase** per identificatori (come Java). `println` → `printLn` o meglio: nessun builtin di IO nel linguaggio, solo wrapper Java. Le keyword del linguaggio restano lowercase (`function`, `let`, `match`, `type`, `struct`, `while`, `if`, `return`, `test`, `assert`, `expect_error`).
- [ ] **Minimizzare i builtin** — il linguaggio deve avere il minimo di costrutti nativi. Tutto ciò che non è strettamente necessario diventa libreria HLL o wrapper Java:
  - `println` → NON è un builtin, è `import java "java.io.PrintStream" as io` poi `io.printLn(...)`
  - `parse_int` → NON è un builtin, è nella libreria `hll.convert`
  - Solo keyword sintattiche sono nel linguaggio: `function`, `let`, `type`, `struct`, `match`, `if`, `while`, `return`, `test`, `assert`, `expect_error`, `Option`, `Some`, `None`, `Result`, `Ok`, `Err`, `true`, `false`, `import`, `where`, `mut`
- [ ] **Unit test e mocking inerenti** — costrutto `mock` per i test. Vedi `doc/08-test-framework.md`.
- [ ] **Fase 2b: critica dei pilastri** — per ogni pilastro: limiti, trade-off, tensioni con gli altri
- [ ] **Migliorare ciclo di test** — creare jar eseguibile + script batch (`tools/compare/run_all.sh`)
- [ ] **Separatore di statement** — decidere `;` opzionale o newline implicito
- [ ] **Fix codegen main signature** — `main(List args)` → `main(String[] args)` automatico
- [ ] **Fix p1 per benchmark L1** — il parser di p1 non supporta match-expr

## Priorità bassa

- [ ] **Formalizzare sistema di wrapper** — meccanismo di import/wrapping per librerie esterne. Attualmente solo Java, in futuro estendibile ad altri target (JS, WASM, etc.). Il wrapper deve: convertire null→Option, tipizzare i parametri, gestire eccezioni del target.
- [ ] **Documentazione multilingua** — uniformare italiano/inglese
- [ ] **Validazione con codice Java reale** — tradurre un frammento di progetto vero in HLL
- [ ] **Script di scoring automatico** — implementare `tools/compare/score.sh`
- [ ] **Copiare codegen fixato a p2b/p2c** — attualmente solo p2a ha il codegen funzionante

## Prossimo passo (loop di sviluppo)

- [ ] **Derivare prototipi livello 3** — p2b è il vincitore (score 36). Tema: type-state o module system?

## Completati (2026-05-10)

- [x] Documentazione di ricerca (9 documenti + loop sviluppo)
- [x] Database pattern/antipattern (93 entry)
- [x] 4 compilatori funzionanti (p1, p2a, p2b, p2c)
- [x] Test framework nativo (test/expect_error) su tutti i prototipi
- [x] Benchmark Java (L1, L2, Math) con output di riferimento
- [x] Codegen funzionante (p2a): HLL → Java → esecuzione → output = reference
- [x] Framework di scoring con formula e denominatori corretti
- [x] LANGUAGE.md, INTENT.md, LINEAGE.md, PROCEDURE.md
- [x] Libreria hll.validation
- [x] Sintassi Java/C (function, tipo-prima)
- [x] Loop di sviluppo documentato (doc/09)
