# p2b "effect" — Riferimento del linguaggio

## Differenze da p2a

p2b condivide tutto con p2a (variabili, struct, tipi nominali, Option, while, match) ma sostituisce `Result<T,E>` con **effetti algebrici**.

## Keyword specifiche di p2b

| Keyword | Significato | Esempio |
|---|---|---|
| `effect` | Dichiara un effetto (tipo di errore/evento che una funzione può sollevare) | `effect InvalidEmail { input: String }` |
| `effects` | Clausola nella firma: dichiara quali effetti una funzione può avere | `fn parse() -> Email effects { InvalidEmail }` |
| `raise` | Solleva un effetto (interrompe il flusso, il chiamante decide cosa fare) | `raise InvalidEmail(input)` |
| `handle` | Gestisce gli effetti di un'espressione, decidendo la strategia | `handle load() { InvalidEmail(e) => ... }` |

## Come funziona

### Dichiarare un effetto

```hll
effect InvalidEmail { input: String }
effect DBError { message: String }
```

Un effetto è simile a una struct — ha campi che descrivono il problema.

### Dichiarare che una funzione può avere effetti

```hll
function parse_email(String input) -> Email
    effects { InvalidEmail } {
    if !valid(input) {
        raise InvalidEmail(input)
    }
    Email(input)
}
```

La clausola `effects { ... }` nella firma dice: "questa funzione può sollevare questi effetti". Il compilatore verifica che il chiamante li gestisca.

### Gestire gli effetti

```hll
let email = handle parse_email(raw_input) {
    InvalidEmail(e) => default_email
}
```

Il `handle` è come un match sugli effetti: per ogni effetto dichiarato, specifichi cosa fare. Il chiamante decide la strategia (fallback, retry, abort).

### Propagazione implicita

Se una funzione chiama un'altra che ha effetti, e li dichiara nella propria clausola `effects`, gli effetti propagano automaticamente:

```hll
function build_order(List<String> args) -> Order
    effects { InvalidEmail, DBError } {
    let email = parse_email(args.get(0))  // InvalidEmail propaga
    let conn = connect()                   // DBError propaga
    ...
}
```

## Differenza concettuale da p2a

| | p2a (Result) | p2b (Effects) |
|---|---|---|
| Chi decide la strategia? | La funzione stessa (con match su Result) | Il chiamante (con handle) |
| L'errore è nel tipo di ritorno? | Sì (`-> Result<T, E>`) | No (`-> T` con `effects { E }`) |
| Propagazione | Esplicita con `?` | Implicita se dichiarata in effects |
| Retry/fallback | Pattern manuale | Naturale nel handler |

## Cosa NON esiste (in aggiunta a p2a)

| Concetto | Perché |
|---|---|
| `Result<T, E>` | Sostituito da effects |
| `Ok()` / `Err()` | Sostituiti da return normale / raise |
| `?` per errori | Non serve — la propagazione è implicita |

(`?` resta disponibile per `Option`)
