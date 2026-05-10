# p3c "state runtime"

## Dichiarazione di intenti

### Deriva da
- **p2b** "effect" — eredita: null safety, tipi nominali, Demeter warning, effetti algebrici

### Ipotesi generale
Le risorse dichiarano stati e transizioni, ma la verifica è **a runtime** (non compile-time). Il compilatore genera automaticamente i check di stato prima di ogni operazione. Se l'operazione è chiamata nello stato sbagliato, errore a runtime con messaggio chiaro.

Questo è il compromesso più semplice: nessun problema di aliasing, nessun move semantics, nessuna restrizione. Il costo è che gli errori si scoprono a runtime, non a compile-time.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p2b) NPE, Stringly-typed, Demeter, Effects | Ereditati |
| Temporal Coupling (open senza close) | Check runtime con messaggio chiaro |
| Use After Free | Check runtime: "operazione non disponibile in stato Closed" |
| Double Free / Double Close | Check runtime: "close non disponibile in stato Closed" |
| Resource Leak | ⚠️ NON bloccato (nessun check a compile-time sui path) |

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
    let conn = Connection.new()
    conn.connect()
    conn.send(data)
    conn.disconnect()
    conn.send(data)    // RUNTIME ERROR: "send not available in state Closed"
}
```

Il Java generato contiene:
```java
public void send(byte[] data) {
    if (this.state != State.CONNECTED) {
        throw new IllegalStateException("send not available in state " + this.state);
    }
    // ... body
}
```

### Trade-off accettati
- Errori a runtime, non compile-time (meno sicuro di p3a/p3b)
- Nessuna restrizione di aliasing (più semplice da usare)
- Resource leak non rilevato (nessuna analisi dei path)
- Ma: messaggio di errore chiaro e strutturato (meglio di Java raw)
- Implementazione molto più semplice (no analisi di flusso nel type checker)

### Stato
❌ Scartato — la verifica a runtime non è coerente con l'obiettivo del progetto (catturare errori a compile-time). Gli errori si scoprono troppo tardi. Resta come riferimento teorico.
