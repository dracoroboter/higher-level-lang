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
| (da p3b) Resource Leak | Warning su path non terminali |
| (da p2c) Exception swallowing | Handler obbligatorio |
| (da p2c) Unchecked exceptions | `fails` dichiarato |
| (da p2c) throws Exception generico | Errori specifici obbligatori |
| (da p1) NPE, Stringly-typed, Demeter | Ereditati |
| Circular Dependencies | DAG verificato a compile-time |
| God Class / God Module | Moduli con interfaccia esplicita (solo export visibili) |
| Hidden Dependencies | Import espliciti, nessun global state |
| Service Locator | Injection dichiarativa (il compilatore verifica le dipendenze) |
| Tight Coupling | Dipendenze su interfacce (service), non implementazioni |
| Singleton abuse | Service con lifecycle gestito dal module system |

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
🔲 Da implementare
