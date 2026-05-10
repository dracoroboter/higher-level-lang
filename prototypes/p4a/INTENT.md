# p4a "module"

## Dichiarazione di intenti

### Deriva da
- **p3b** "state light" — eredita: null safety, tipi nominali, Demeter warning, type-state in-place
- **p2c** "checked simple" — eredita: `fails E` con handler inline, nessun catch vuoto

### Ipotesi generale
Il codice reale non vive in un singolo file. Servono confini tra moduli, dipendenze esplicite, e la possibilità di iniettare implementazioni diverse (per test, per ambienti diversi). Il module system rende queste pratiche **strutturali**: le dipendenze formano un DAG verificato a compile-time, i cicli sono impossibili, e l'injection è un costrutto del linguaggio (non un framework esterno).

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p3b) Temporal Coupling | State machine verificata |
| (da p3b) Use After Free / Double Close | Type-state |
| (da p2c) Exception swallowing | Handler obbligatorio |
| (da p2c) Unchecked exceptions | `fails` dichiarato |
| (da p1) NPE, Stringly-typed, Demeter | Ereditati |
| (da p1) Type Confusion | Tipi nominali, no cast |
| (da p1) SQL Injection | Tipo nominale Query |
| Circular Dependencies | DAG verificato a compile-time |
| God Class / God Module | Moduli con interfaccia esplicita (solo export visibili) |
| Hidden Dependencies | Import espliciti, nessun global state |
| Service Locator | Injection dichiarativa (il compilatore verifica le dipendenze) |
| Tight Coupling | Dipendenze su interfacce (service), non implementazioni |
| Mutable Global State | Parser rifiuta variabili top-level |
| Lava Flow / Dead Code | Warning su funzioni mai chiamate |
| Inconsistent Return Types | Check che tutti i path ritornino un valore |
| God Interface | Warning su service con >7 metodi |
| Incomplete Mock | Mock deve implementare tutti i metodi del service |
| Mock non-service | Solo i service sono mockabili |

### Bloccati per assenza del costrutto (by design)
| Antipattern | Costrutto assente |
|---|---|
| Deep Inheritance | No `class extends` |
| Yo-Yo Problem | No ereditarietà |
| Callback Hell | No callback asincroni |
| Poltergeist | No classi stateless (solo struct + service) |
| Thread Unsafe Singleton | No `static`, no global state |

### Meccanismo
```hll
// file: auth/service.hll
module auth

export service AuthService {
    function login(Email email, Password pwd) -> Session fails AuthError
    function verify(Token token) -> UserId fails AuthError
}

// file: auth/impl.hll
module auth.impl

import auth.AuthService
import db.UserRepo

provide AuthService {
    needs UserRepo repo

    function login(Email email, Password pwd) -> Session fails AuthError {
        let user = repo.findByEmail(email) | AuthError => fail AuthError("not found")
        // ...
    }
}

// file: app.hll
module app

import auth.AuthService

function handleRequest(AuthService auth, Request req) -> Response fails AppError {
    let userId = auth.verify(req.token) | AuthError(e) => fail AppError(e.message)
    // ...
}
```

### Trade-off accettati
- Più verboso per programmi piccoli (un modulo per un hello world è overkill)
- Il DAG check impedisce pattern legittimi di mutua ricorsione tra moduli (rari ma esistono)
- `provide` + `needs` è meno flessibile di un DI container completo (no scoping, no lazy)
- Il module system aggiunge complessità al compilatore (risoluzione nomi, multi-file)

### Stato
✅ Funzionante (2026-05-10)
- Score: 52/100 (28/46 antipattern = 60%)
- Compilatore multi-file con DAG check
- 23 test invalid rifiutati + 5 bloccati by design
- 6 test valid + 3 benchmark compilano
- Mock nativo per test
- `--strict` promuove warning a errori

