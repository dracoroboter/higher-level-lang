# p4a "module" — Language Reference

## Keyword

| Keyword | Significato | Esempio |
|---|---|---|
| `module` | Dichiara il modulo corrente | `module auth` |
| `import` | Importa simboli da un altro modulo | `import auth.AuthService` |
| `export` | Rende un simbolo visibile fuori dal modulo | `export function ...` |
| `service` | Dichiara un'interfaccia iniettabile | `export service Repo { ... }` |
| `provide` | Implementa un service | `provide Repo { ... }` |
| `needs` | Dichiara una dipendenza in un provide | `needs Logger log` |
| `function` | Dichiarazione di funzione | `function foo() -> Int` |
| `let` | Binding immutabile | `let x = 5` |
| `mut` | Binding mutabile | `let mut x = 5` |
| `type` | Tipo nominale | `type Email = String where validate.email()` |
| `struct` | Record | `struct User { ... }` |
| `state` | Macchina a stati (da p3b) | `state Connection { ... }` |
| `match` | Pattern matching | `match opt { Some(x) => ... }` |
| `if`/`else` | Condizionale | `if x > 0 { ... }` |
| `while` | Loop | `while cond { ... }` |
| `fails` | Errori dichiarati (da p2c) | `function f() -> T fails E` |
| `fail` | Solleva un errore | `fail IOError("msg")` |
| `test` | Test compile-time | `test "name" { ... }` |
| `expect_error` | Verifica rifiuto | `expect_error { ... }` |

## Sintassi moduli

### Dichiarazione modulo
```hll
module auth.impl
```
Ogni file inizia con `module`. Il nome segue la struttura directory (convenzione, non obbligo).

### Export
```hll
export function publicFn() -> Int { ... }
export struct PublicData { ... }
export service AuthService { ... }

// Senza export → visibile solo nel modulo
function privateFn() -> Int { ... }
```

### Import
```hll
import auth.AuthService           // singolo simbolo
import db.{UserRepo, Connection}  // multipli
import utils                      // intero modulo (accesso con utils.foo())
```

### Service (interfaccia iniettabile)
```hll
export service UserRepo {
    function findById(UserId id) -> Option<User> fails DBError
    function save(User user) -> Unit fails DBError
}
```

### Provide (implementazione)
```hll
provide UserRepo {
    needs Connection conn
    needs Logger log

    function findById(UserId id) -> Option<User> fails DBError {
        log.debug("finding user")
        conn.query("SELECT ...")
    }

    function save(User user) -> Unit fails DBError {
        conn.execute("INSERT ...")
    }
}
```

### Wiring (entry point)
```hll
module main

import auth.AuthService
import auth.impl    // contiene il provide
import db.impl      // contiene il provide di Connection

function main(String[] args) {
    // Il compilatore verifica che tutti i `needs` sono soddisfatti
    // dal grafo dei provide importati
}
```

## Regole del module system

1. **DAG obbligatorio** — se A importa B, B non può importare A (diretto o transitivo). Errore a compile-time.
2. **Visibilità** — solo i simboli `export` sono visibili fuori dal modulo.
3. **Needs soddisfatti** — ogni `needs` in un `provide` deve essere soddisfatto da un altro `provide` nel grafo di import.
4. **No global state** — nessuna variabile top-level mutabile. Lo stato vive nei service.
5. **Service = interfaccia** — non puoi istanziare un service direttamente, solo usarlo come tipo parametro.

## Errori (da p2c)
```hll
function readFile(Path p) -> String fails IOError {
    // ...
}

function loadConfig() -> Config fails IOError, ParseError {
    let content = readFile(configPath)    // IOError propagato
    parse(content)                         // ParseError propagato
}

// Handler inline
let cfg = loadConfig() | IOError(e) => defaultConfig() | ParseError(e) => fail AppError(e)
```

## Type-state (da p3b)
```hll
state Transaction {
    Idle { function begin() -> Active }
    Active {
        function commit() -> Done
        function rollback() -> Done
    }
    Done { }
}
```
