# p1 "null train" — Riferimento del linguaggio

## Keyword

| Keyword | Significato | Esempio |
|---|---|---|
| `function` | Dichiara una funzione | `function add(Int a, Int b) -> Int { ... }` |
| `let` | Dichiara una variabile immutabile | `let x = 5` o `let Int x = 5` |
| `type` | Dichiara un tipo nominale | `type Email = String where validate.email()` |
| `struct` | Dichiara una struttura dati | `struct Point { Int x, Int y }` |
| `if` / `else` | Condizionale | `if x > 0 { ... } else { ... }` |
| `match` | Pattern matching | `match opt { Some(v) => v, None => 0 }` |
| `return` | Ritorna un valore | `return 42` |
| `import` | Importa una libreria | `import hll.validation as validate` |
| `as` | Alias per import | `import java "..." as DB` |
| `where` | Vincolo di validazione su tipo nominale | `type Age = Int where validate.positive()` |
| `Option` | Tipo: valore presente o assente | `Option<Address>` |
| `Some` | Il valore è presente | `Some(addr)` |
| `None` | Il valore è assente | `None` |
| `?` | Propagazione: se None, ritorna None | `let x = find(id)?` |
| `true` / `false` | Letterali booleani | `let done = false` |

## Tipi

| Tipo | Descrizione |
|---|---|
| `Int` | Numero intero |
| `Float` | Numero decimale |
| `String` | Testo |
| `Bool` | Vero o falso |
| `Option<T>` | Valore che può essere assente |
| Tipi nominali | Definiti con `type`, distinti dal tipo base |
| Struct | Campi nominati raggruppati |

## Operatori

| Operatore | Significato |
|---|---|
| `+` `-` `*` `/` | Aritmetica |
| `==` `!=` `<` `>` `<=` `>=` | Confronto |
| `&&` `\|\|` `!` | Logica |
| `.` | Accesso a campo |
| `?` | Propagazione Option |

## Sintassi

```hll
// Variabili
let x = 5              // tipo inferito
let Int x = 5          // tipo esplicito

// Funzioni
function add(Int a, Int b) -> Int {
    return a + b
}

// Struct
struct Customer {
    String name,
    Email email,
    Option<Address> address
}

// Pattern matching
match order.customer.address {
    Some(addr) => addr.city
    None => "Unknown"
}
```

## Cosa NON esiste

| Concetto | Alternativa |
|---|---|
| `null` | `Option<T>` |
| `unwrap()` | `match` obbligatorio |
| `class` / `extends` | `struct` + `function` |
