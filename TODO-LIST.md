# TODO List

## Priorità alta

- [ ] **Naming convention + minimizzare builtin** — applicare camelCase a tutti i file .hll, rimuovere println/parseInt come builtin, sostituire con wrapper Java
- [ ] **Script di scoring automatico** — `tools/compare/score.sh` che calcola lo score di un prototipo con un comando
- [ ] **Validazione con codice Java reale** — tradurre un frammento di progetto vero in HLL per scoprire i limiti pratici del linguaggio
- [x] **Verificare procedura di loop** — aggiunto step 2b (confronto output Java) alla PROCEDURE.md

## Priorità media

- [ ] **Unit test e mocking inerenti** — costrutto `mock` per i test. Vedi `doc/08-test-framework.md`. Dipende dal module system (injection).
- [ ] **Derivare prototipi livello 4** — p3b è il vincitore. Tema possibile: module system (DAG, injection, service)
- [ ] **Fase 2b: critica dei pilastri** — per ogni pilastro: limiti, trade-off, tensioni con gli altri
- [ ] **Migliorare ciclo di test** — creare jar eseguibile + script batch (`tools/compare/run_all.sh`)
- [ ] **Separatore di statement** — decidere `;` opzionale o newline implicito
- [ ] **Fix codegen main signature** — `main(List args)` → `main(String[] args)` automatico
- [ ] **Fix p1 per benchmark L1** — il parser di p1 non supporta match-expr

## Priorità bassa

- [ ] **Formalizzare sistema di wrapper** — meccanismo di import/wrapping per librerie esterne. In futuro estendibile ad altri target (JS, WASM, etc.).
- [ ] **Documentazione multilingua** — uniformare italiano/inglese
- [x] **Copiare codegen fixato a p2b/p2c** — codegen copiato a tutti i prototipi (p2b, p2c, p3a, p3b)

## Prossimo passo (loop di sviluppo)

- [x] **Derivare prototipi livello 3** — p3a e p3b implementati (p3a=48, p3b=47). p3b vince per ceremony ratio (-216%).
- [ ] **Derivare prototipi livello 4** — p3b è il vincitore. Tema possibile: module system (DAG, injection, service)

## Completati (2026-05-10)

- [x] Documentazione di ricerca (9 documenti + loop sviluppo + style)
- [x] Database pattern/antipattern (93 entry)
- [x] 6 compilatori funzionanti (p1, p2a, p2b, p2c, p3a, p3b)
- [x] Test framework nativo (test/expect_error) su tutti i prototipi
- [x] Benchmark Java (L1, L2, L3, Math) con output di riferimento
- [x] Codegen funzionante (p2a): HLL → Java → esecuzione → output = reference
- [x] Type-state verificato a compile-time (p3a, p3b)
- [x] Framework di scoring con formula, denominatori corretti, ceremony ratio
- [x] LANGUAGE.md, INTENT.md, LINEAGE.md, PROCEDURE.md
- [x] Libreria hll.validation
- [x] Sintassi Java/C (function, tipo-prima)
- [x] Loop di sviluppo documentato (doc/09)
- [x] Fix parser: methodCall prima di fieldAccess (tutti i prototipi)
