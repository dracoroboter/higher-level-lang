# p3b "state light"

## Dichiarazione di intenti

### Deriva da
- **p2b** "effect" — eredita: null safety, tipi nominali, Demeter warning, effetti algebrici

### Ipotesi generale
Come p3a, le risorse dichiarano stati e transizioni verificati a compile-time. Ma l'ownership è **solo per i tipi con `state`** — il resto del linguaggio usa reference semantics normale (con GC). Questo riduce la complessità: solo chi dichiara un protocollo paga il costo del move semantics.

La risorsa si usa con sintassi a metodo (non rebinding): `conn.connect()` muta lo stato in-place. Il compilatore traccia lo stato della variabile attraverso il flusso.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p2b) NPE, Stringly-typed, Demeter, Effects | Ereditati |
| Temporal Coupling (open senza close) | State machine verificata a compile-time |
| Use After Free | Dopo transizione a stato terminale, nessun metodo disponibile |
| Double Free / Double Close | Dopo close, il tipo non ha più close |
| Resource Leak | Warning se un path non raggiunge lo stato terminale |

### Meccanismo
```hll
state Connection {
    Disconnected {
        function connect() -> Connected
    }
    Connected {
        function send(Bytes data) -> Connected
        function disconnect() -> Closed
    }
    Closed { }
}

function main() {
    let conn = Connection.new()       // stato: Disconnected
    conn.connect()                     // stato diventa: Connected
    conn.send(data)                    // stato resta: Connected
    conn.disconnect()                  // stato diventa: Closed
    // conn.send(data)                 // ERRORE: send non esiste in Closed
}
```

### Trade-off accettati
- Aliasing limitato: una variabile con `state` non può essere aliasata (il compilatore lo impedisce)
- Ma il resto del codice (struct, tipi normali) non ha restrizioni
- Meno sicuro di p3a in teoria (aliasing accidentale possibile se il check non è perfetto)
- Più ergonomico: `conn.connect()` invece di `let conn = conn.connect()`

### Stato
✅ Funzionante (2026-05-10)
