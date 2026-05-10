# Stile della documentazione

## Lingue

| Tipo di file | Lingua | Motivo |
|---|---|---|
| Documenti di ricerca (`doc/`) | Italiano | Pubblico primario, discussione in italiano |
| INTENT.md, LANGUAGE.md | Inglese | Riferimento tecnico, standard internazionale |
| Codice sorgente (.java, .hll) | Inglese | Standard per codice |
| Commenti nel codice | Inglese | Standard per codice |
| README dei prototipi | Inglese | Tecnico |
| LINEAGE.md, BENCHMARK.md | Inglese | Tecnico |
| Messaggi di errore del compilatore | Inglese | Output per programmatori |

## Naming convention (codice HLL)

| Elemento | Formato | Esempio |
|---|---|---|
| Variabili | camelCase | `orderTotal`, `firstName` |
| Funzioni | camelCase | `findUser`, `parseEmail` |
| Tipi / Struct | PascalCase | `Email`, `OrderId`, `Customer` |
| Keyword | lowercase | `function`, `let`, `match`, `type` |
| Costanti | UPPER_SNAKE (futuro) | `MAX_RETRIES` |

Motivazione: coerenza con Java (il target di transpilazione). Un programmatore Java legge codice HLL senza adattamento.

## Builtin minimi

Il linguaggio ha il **minimo** di costrutti nativi. Tutto il resto è libreria:

**Nel linguaggio (keyword/costrutti):**
- Controllo flusso: `function`, `let`, `if`, `else`, `while`, `match`, `return`
- Tipi: `type`, `struct`, `Option`, `Some`, `None`, `Result`, `Ok`, `Err`
- Modificatori: `mut`, `where`
- Import: `import`, `as`
- Test: `test`, `assert`, `expect_error`
- Letterali: `true`, `false`, numeri, stringhe

**NON nel linguaggio (libreria):**
- IO: `printLn`, `readLine` → wrapper Java (`java.io`)
- Conversioni: `parseInt`, `parseFloat` → libreria `hll.convert`
- Validazione: `email()`, `url()` → libreria `hll.validation`
- Collezioni: `List`, `Map` → wrapper Java
- Math: `sqrt`, `abs` → wrapper Java (`java.lang.Math`)

## Regole generali

- Non mescolare italiano e inglese nello stesso documento
- I nomi dei file sono in inglese (kebab-case o UPPERCASE per i .md)
- I nomi delle keyword del linguaggio HLL sono in inglese
- Le variabili e funzioni nei benchmark sono in inglese
- I nickname dei prototipi sono in inglese (es. "null train", "result chain")

## Attività futura

Quando il progetto sarà più maturo, tradurre i documenti di ricerca in inglese per un pubblico accademico. Per ora la priorità è la velocità di scrittura, non la traduzione.

## Regola delle giustificazioni

Ogni scelta di design del linguaggio deve essere giustificata con almeno uno tra:
1. **Esempio vincente in produzione** — un linguaggio/sistema che usa lo stesso approccio con successo dimostrabile (es. Erlang actor model → WhatsApp)
2. **Pratica consolidata nell'industria** — pattern riconosciuto e adottato da più progetti/aziende (es. dependency injection)
3. **Supporto accademico** — paper, studio, o formalizzazione che dimostra la correttezza/fattibilità (es. Strom & Yemini 1986 per typestate)

Le giustificazioni vanno documentate in `doc/04-bibliografia.md` nella sezione "Decisioni di design informate dalla letteratura".

Se una scelta non ha giustificazione, va marcata come **sperimentale** e monitorata per validazione futura.
