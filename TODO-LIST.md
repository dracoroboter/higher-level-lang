# TODO List

## Priorità alta

- [x] **Fix bug NPE p2b** — risolto: type checker reso null-safe, rimossi `;` dal benchmark
- [ ] **Batteria di test del compilatore** — test che verificano la correttezza del compilatore stesso (non del linguaggio). Per ogni feature del type checker, un test che verifica: a) codice valido accettato, b) codice invalido rifiutato con il messaggio giusto. Usa il costrutto `expect_error` quando implementato, altrimenti file separati valid/invalid.
- [ ] **Fix parametri scambiati** — ✅ RISOLTO per p2a/p2b/p2c
- [ ] **Implementare verifica gestione errori** — la feature differenziante dei prototipi L2 non è implementata:
  - p2a: verificare che Result sia consumato + match esaustivo su Result
  - p2b: verificare che effetti dichiarati siano gestiti dal chiamante
  - p2c: verificare che fails dichiarato sia gestito con | handler
  - SENZA QUESTE FEATURE IL CONFRONTO TRA PROTOTIPI NON È SIGNIFICATIVO
- [ ] **Scrivere più test invalid** — il punteggio antipattern è 6.5% (3/46). Servono test per: Result ignorato, match non esaustivo, effect non gestito
- [ ] **Fix codegen (while, match-expr, costruttori)** — il Java generato ha placeholder (`__while__`) e costruttori non corretti. Serve per eseguire il codice e confrontare output con reference oracle

## Priorità media

- [ ] **Unit test e mocking inerenti** — il linguaggio deve avere un meccanismo nativo per scrivere unit test e mockare comportamenti. Costrutto `test` + `assert` + `expect_error` (compile-time) + `mock` (futuro). Vedi `doc/08-test-framework.md`.
- [ ] **Fase 2b: critica dei pilastri** — per ogni pilastro: limiti, trade-off, tensioni con gli altri. Documentata nel piano ma non ancora fatta
- [ ] **Migliorare ciclo di test** — creare jar eseguibile + script batch (`tools/compare/run_all.sh`) per non lanciare Maven ogni volta
- [ ] **Separatore di statement** — decidere se usare `;` opzionale, newline significativo, o lasciare l'approccio attuale (newline implicito come Go/Kotlin). Attualmente il linguaggio non ha separatore esplicito e il parser distingue gli statement dalla struttura sintattica.
- [x] **Keyword `fn` vs `function`** — decisione: `function` (leggibilità prioritaria)
- [x] **Allineare grammatica ANTLR alla nuova sintassi** — fatto: `function`, tipo-prima (`let Int x`, `function add(Int a, Int b)`), struct con `Type field`. Tutti i prototipi aggiornati e testati.
- [ ] **Fix p1 per benchmark L1** — il parser di p1 non supporta match-expr (aggiunto solo in p2a+)
- [ ] **Aggiungere test specifici per eccezioni** — test che discriminano p2a vs p2b vs p2c (es. "errore non gestito deve essere rifiutato")

## Priorità bassa

- [ ] **Documentazione multilingua** — uniformare italiano/inglese, tradurre doc di ricerca in EN
- [ ] **Validazione con codice Java reale** — tradurre un frammento di progetto vero in HLL
- [ ] **Script di scoring automatico** — implementare `tools/compare/score.sh` che calcola il punteggio
- [ ] **Implementare p2b/p2c codegen** — generare Java funzionante (non solo parsing/type-check)

## Completati (2026-05-10)

- [x] Documentazione di ricerca (9 documenti in `doc/`)
- [x] Database pattern/antipattern (93 entry)
- [x] Compilatore p1 funzionante (parser + type checker + codegen base)
- [x] Compilatore p2a funzionante (+ Result, ?, while, mut)
- [x] Compilatore p2b funzionante (+ effects/handle/raise) — benchmark math OK, L2 ha bug
- [x] Compilatore p2c funzionante (+ fails/fail/| handler)
- [x] Benchmark Java (L1, L2, Math) con output di riferimento
- [x] Benchmark HLL per tutti i prototipi
- [x] Framework di scoring (BENCHMARK.md)
- [x] LANGUAGE.md per ogni prototipo
- [x] INTENT.md per ogni prototipo
- [x] LINEAGE.md con grafo di derivazione
- [x] Libreria hll.validation
- [x] STYLE.md
