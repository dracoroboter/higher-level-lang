# TODO List

## Priorità alta

- [x] **Naming convention + minimizzare builtin** — camelCase applicato, println/parseInt sono wrapper Java nel codegen (non keyword)
- [x] **Script di scoring automatico** — `tools/compare/score.sh`, usa solo test del prototipo, `--strict` per warning→error
- [ ] **Validazione con codice Java reale** — benchmarkapp creata (OrderSystem). p4a traduce con 5/5 test. Estendere nel tempo.
- [x] **Verificare procedura di loop** — aggiunto step 2b (confronto output Java) alla PROCEDURE.md

**Regola:** ogni scelta di design deve essere giustificata con almeno uno tra: esempio vincente in produzione, pratica consolidata nell'industria, supporto accademico (paper/studio). Le giustificazioni vanno in `doc/04-bibliografia.md`.

## Priorità media

- [ ] **Risolvere assenza ereditarietà** — 3 problemi aperti: verbosità delega, polimorfismo ad-hoc, interop framework. Vedi `doc/04-bibliografia.md` sezione "Assenza di ereditarietà"
- [ ] **Concorrenza sicura (Actor Model)** — `spawn` crea attori, `service` = interfaccia attore, stato privato, messaggi immutabili. Blocca Race Condition, Deadlock, Busy Waiting. Vedi `doc/11-concorrenza.md`
- [ ] **Unit test e mocking inerenti** — ✅ `mock` implementato in p4a. Manca: `expect_fail ErrorType { expr }` per test di errori runtime (non solo compile-time). Vedi Kotlin `assertThrows`, Rust `#[should_panic]`, QuickCheck property-based testing.
- [x] **Derivare prototipi livello 4** — p4a implementato (score 47). Padri: p3b + p2c. Multi-file compiler.
- [ ] **Fase 2b: critica dei pilastri** — per ogni pilastro: limiti, trade-off, tensioni con gli altri
- [ ] **Migliorare ciclo di test** — creare jar eseguibile + script batch (`tools/compare/run_all.sh`)
- [x] **Separatore di statement** — newline implicito (nessun `;` richiesto), già implementato
- [ ] **Fix codegen main signature** — `main(List args)` → `main(String[] args)` automatico
- [ ] **Fix p1 per benchmark L1** — il parser di p1 non supporta match-expr

## Priorità bassa

- [ ] **Formalizzare sistema di wrapper** — meccanismo di import/wrapping per librerie esterne. In futuro estendibile ad altri target (JS, WASM, etc.).
- [ ] **Documentazione multilingua** — uniformare italiano/inglese
- [x] **Copiare codegen fixato a p2b/p2c** — codegen copiato a tutti i prototipi (p2b, p2c, p3a, p3b)

## Prossimo passo (loop di sviluppo)

- [x] **Derivare prototipi livello 3** — p3a e p3b implementati (p3a=48, p3b=47). p3b vince per ceremony ratio (-216%).
- [x] **Derivare prototipi livello 4** — p4a implementato (score 46). Padri: p3b + p2c. Multi-file compiler.
- [ ] **Test semantici astratti** — definire antipattern come spec astratte (es. "use-after-close"), ogni prototipo genera la propria versione del test. Elimina la copia manuale tra prototipi.

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
