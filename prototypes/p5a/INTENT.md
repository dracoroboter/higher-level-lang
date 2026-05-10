# p5a "iterate classic"

## Dichiarazione di intenti

### Deriva da
- **p4a** "module" — eredita: module system, service/provide/needs, actor model, type-state, fails, mock, expectFail

### Ipotesi generale
L'iterazione segue il modello standard dell'industria: `for x in collection` per l'imperativo + pipeline con lambda (`.filter().map().reduce()`) per il dichiarativo. Due costrutti separati, come Rust e Kotlin.

### Meccanismo

**Imperativo (for-in):**
```hll
for order in orders {
    if order.amount > 100 {
        println(order.id)
    }
}
```

**Dichiarativo (pipeline + lambda):**
```hll
let bigIds = orders
    .filter(|o| o.amount > 100)
    .map(|o| o.id)

let total = orders
    .filter(|o| o.status.equals("confirmed"))
    .reduce(0, |acc, o| acc + o.amount)
```

**Combinato (Rust-style):**
```hll
for order in orders.filter(|o| o.amount > 100) {
    if found { break }
    println(order.id)
}
```

### Nuove feature
- `for x in expr { body }` — iterazione imperativa con break/continue
- `|params| expr` — lambda/closure
- `.filter()`, `.map()`, `.reduce()` — metodi su collezioni
- `List<T>` — tipo builtin

### Trade-off accettati
- Lambda aggiungono complessità al type system (closure capture)
- Due modi di iterare = più da imparare
- I/O nelle lambda è permesso (warning, non errore)
- Debugging delle pipeline resta difficile

### Fonti
- Rust Book cap. 13: zero-cost iterators
- Kotlin sequences: lazy evaluation
- Java Streams: Goetz "State of the Lambda" (2013)

### Stato
🔲 Da implementare
