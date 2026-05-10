# p2c "checked simple" — Riferimento del linguaggio

## Differenze da p2a

p2c condivide tutto con p2a (variabili, struct, tipi nominali, Option, while, match) ma sostituisce `Result<T,E>` con **eccezioni checked semplificate**.

## Keyword specifiche di p2c

| Keyword | Significato | Esempio |
|---|---|---|
| `fails` | Clausola nella firma: dichiara quali errori una funzione può produrre | `fn parse() -> Email fails InvalidEmail` |
| `fail` | Produce un errore (interrompe la funzione) | `fail InvalidEmail(input)` |
| `\|` | Handler inline: gestisce un errore specifico dopo una chiamata | `let x = load() \| IOError(e) => default` |

## Come funziona

### Dichiarare che una funzione può fallire

```hll
function parse_email(String input) -> Email fails InvalidEmail {
    if !valid(input) {
        fail InvalidEmail(input)
    }
    Email(input)
}
```

La clausola `fails` nella firma dice: "questa funzione può produrre questi errori". Il compilatore verifica che il chiamante li gestisca.

### Gestire gli errori con | handler

```hll
let email = parse_email(raw_input)
    | InvalidEmail(e) => default_email
```

Il `|` dopo una chiamata è un handler inline: per ogni errore dichiarato nella `fails` della funzione chiamata, specifichi cosa fare.

### Propagazione automatica

Se una funzione chiama un'altra che `fails`, e dichiara lo stesso errore nella propria `fails`, l'errore propaga automaticamente senza handler:

```hll
function build_order(List<String> args) -> Order fails InvalidEmail, DBError {
    let email = parse_email(args.get(0))  // InvalidEmail propaga (dichiarato)
    let conn = connect()                   // DBError propaga (dichiarato)
    ...
}
```

Se NON dichiari l'errore nella tua `fails`, il compilatore ti obbliga a gestirlo con `|`.

### Errori multipli

```hll
let order = build_order(args)
    | InvalidEmail(e) => { println("bad email"); return }
    | DBError(e) => { println("db down"); return }
```

## Differenza concettuale da p2a e p2b

| | p2a (Result) | p2b (Effects) | p2c (Checked) |
|---|---|---|---|
| Errore nel tipo di ritorno? | Sì (`Result<T,E>`) | No (effects separati) | No (fails separato) |
| Propagazione | Esplicita (`?`) | Implicita (effects) | Implicita (fails) |
| Gestione | `match` su Result | `handle { }` | `\| Error => ...` |
| Familiarità | Rust | Ricerca (Koka, Eff) | Java/Kotlin |
| Keyword per produrre errore | `Err(...)` | `raise ...` | `fail ...` |

## Cosa NON esiste (in aggiunta a p2a)

| Concetto | Perché |
|---|---|
| `Result<T, E>` | Sostituito da `fails` |
| `Ok()` / `Err()` | Sostituiti da return normale / `fail` |
| `?` per errori | Non serve — propagazione automatica se dichiarato in `fails` |
| `try` / `catch` | Non esiste — `\|` handler è inline e obbligatorio |
| `throws Exception` | Non esiste — devi dichiarare errori specifici |
| catch vuoto | Impossibile — ogni `\|` deve avere un corpo |

(`?` resta disponibile per `Option`)
