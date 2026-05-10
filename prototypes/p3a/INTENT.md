# p3a "state strict"

## Dichiarazione di intenti

### Deriva da
- **p2b** "effect" — eredita: null safety, tipi nominali, Demeter warning, effetti algebrici

### Ipotesi generale
Le risorse con protocollo (file, connessioni, transazioni) dichiarano i loro **stati e transizioni** come costrutto del linguaggio. Il compilatore verifica a compile-time che le operazioni avvengano solo nello stato corretto. L'ownership è **obbligatoria** per tutti i tipi con `state` — nessun aliasing possibile, il valore si muove (come Rust).

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
    let conn = Connection.new()       // Connection<Disconnected>
    let conn = conn.connect()          // Connection<Connected>
    let conn = conn.send(data)         // Connection<Connected>
    let conn = conn.disconnect()       // Connection<Closed>
    // conn.send(data)                 // ERRORE: send non esiste in Closed
}
```

### Trade-off accettati
- Ownership obbligatoria: ogni operazione "consuma" il valore e ne produce uno nuovo (rebinding)
- Più verboso: `let conn = conn.connect()` invece di `conn.connect()`
- Non si può passare una risorsa con stato a due funzioni contemporaneamente
- Curva di apprendimento: il programmatore deve capire il move semantics

### Stato
🔲 Da implementare
