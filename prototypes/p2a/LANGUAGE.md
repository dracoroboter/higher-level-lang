# p2a "result chain" — Riferimento del linguaggio

## Keyword

| Keyword | Significato | Esempio |
|---|---|---|
| `function` | Dichiara una funzione | `function add(Int a, Int b) -> Int { ... }` |
| `let` | Dichiara una variabile immutabile | `let x = 5` o `let Int x = 5` |
| `let mut` | Dichiara una variabile mutabile (riassegnabile) | `let mut Int i = 0` |
| `type` | Dichiara un tipo nominale (alias con identità) | `type Email = String where validate.email()` |
| `struct` | Dichiara una struttura dati con campi nominati | `struct Point { x: Int, y: Int }` |
| `if` / `else` | Condizionale | `if x > 0 { ... } else { ... }` |
| `while` | Loop con condizione | `while i < 10 { i = i + 1 }` |
| `match` | Pattern matching (decostruzione di un valore) | `match opt { Some(v) => v, None => 0 }` |
| `return` | Ritorna un valore dalla funzione | `return 42` |
| `import` | Importa una libreria (HLL o Java) | `import hll.validation as validate` |
| `as` | Assegna un alias a un import | `import java "..." as DB` |
| `where` | Vincolo di validazione su un tipo nominale | `type Age = Int where validate.positive()` |
| `Option` | Tipo che rappresenta un valore presente o assente | `address: Option<Address>` |
| `Some` | Costruttore: il valore è presente | `Some(address)` |
| `None` | Costruttore: il valore è assente | `None` |
| `Result` | Tipo che rappresenta successo o errore | `Result<Config, IOError>` |
| `Ok` | Costruttore: operazione riuscita | `Ok(value)` |
| `Err` | Costruttore: operazione fallita | `Err(InvalidEmail(input))` |
| `?` | Operatore di propagazione: se Option è None o Result è Err, ritorna immediatamente | `let cfg = read_config(path)?` |
| `mut` | Modificatore: la variabile può essere riassegnata | `let mut count = 0` |
| `true` / `false` | Letterali booleani | `let done = false` |

## Tipi

| Tipo | Descrizione |
|---|---|
| `Int` | Numero intero |
| `Float` | Numero decimale |
| `String` | Testo |
| `Bool` | Vero o falso |
| `Option<T>` | Valore di tipo T che può essere assente (Some o None) |
| `Result<T, E>` | Operazione che produce T in caso di successo o E in caso di errore |
| `List<T>` | Lista di elementi di tipo T |
| Tipi nominali | Definiti con `type`, sono distinti dal tipo base (Email ≠ String) |
| Struct | Definiti con `struct`, raggruppano campi nominati |

## Operatori

| Operatore | Significato |
|---|---|
| `+` `-` `*` `/` `%` | Aritmetica (% = modulo/resto) |
| `==` `!=` `<` `>` `<=` `>=` | Confronto |
| `&&` `\|\|` `!` | Logica (e, o, non) |
| `.` | Accesso a campo di una struct |
| `?` | Propagazione di Option/Result |
| `=` | Assegnamento (solo su variabili `mut`) |

## Costrutti

### Variabili

```hll
let x = 5              // immutabile, tipo inferito
let Int x = 5          // immutabile, tipo esplicito
let mut Int i = 0      // mutabile, tipo esplicito
let mut i = 0          // mutabile, tipo inferito
i = i + 1              // riassegnamento (solo se dichiarata con mut)
```

`mut` significa "mutabile" — la variabile può cambiare valore dopo la dichiarazione. Senza `mut`, il valore è fisso. Il tipo prima del nome è opzionale (inferenza).

### Funzioni

```hll
function nome(Int param1, String param2) -> TipoRitorno {
    // corpo
}

function add(Int a, Int b) -> Int {
    return a + b
}
```

### Tipi nominali

```hll
type Email = String where validate.email()
```

Crea un tipo `Email` che è distinto da `String`. Il `where` specifica una funzione di validazione che viene eseguita quando si costruisce il valore: `Email("alice@example.com")`. Se la validazione fallisce, errore a runtime.

### Struct

```hll
struct Customer {
    String name,
    Email email,
    Option<Address> address
}
```

### Pattern matching

```hll
match espressione {
    Some(valore) => // usa valore
    None => // gestisci assenza
}

match risultato {
    Ok(valore) => // successo
    Err(errore) => // gestisci errore
}
```

### Propagazione con ?

```hll
function load() -> Result<Data, AppError> {
    let cfg = read_config(path)?    // se Err, ritorna Err immediatamente
    let conn = connect(cfg)?        // idem
    Ok(conn.query("..."))
}
```

L'operatore `?` è zucchero sintattico per: "se il risultato è Err (o None), ritorna subito con quell'errore; altrimenti estrai il valore e continua".

### Import

```hll
import hll.validation as validate          // libreria HLL
import java "java.sql.Connection" as Conn  // classe Java (null → Option)
```

## Cosa NON esiste

| Concetto | Perché non c'è |
|---|---|
| `null` | Non esiste. Usa `Option<T>` per valori assenti. |
| `try` / `catch` | Non esiste. Usa `Result<T, E>` e `?` per gli errori. |
| `throw` | Non esiste. Usa `Err(...)` per segnalare errori. |
| `class` / `extends` | Non esiste. Usa `struct` + `fn`. |
| `unwrap()` | Non esiste. Devi fare `match` per accedere al valore. |
