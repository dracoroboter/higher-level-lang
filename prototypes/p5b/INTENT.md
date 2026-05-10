# p5b "iterate query"

## Dichiarazione di intenti

### Deriva da
- **p4a** "module" — eredita: module system, service/provide/needs, actor model, type-state, fails, mock, expectFail

### Ipotesi generale
L'iterazione usa un costrutto unico ibrido SQL-style: `for x in collection when clausole dichiarative (`when`, `yield`, `into`) sono pure — il compilatore lo verifica. Gli effetti (I/O, DB) vanno nel body imperativo.

### Meccanismo

**Filtro + body imperativo:**
```hll
for order in orders
    when order.amount > 100
    when order.status != "cancelled"
{
    let details = fetchDetails(order.id) | IOError(e) => continue
    println(details.summary)
}
```

**Trasformazione (yield → produce lista):**
```hll
let ids = for order in orders
    when order.amount > 100
    yield order.id
```

**Accumulo (into → reduce):**
```hll
let total = for order in orders
    when order.status == "confirmed"
    into sum(order.amount)
```

**Limite + early exit:**
```hll
for order in orders
    when order.amount > 100
    take 10
{
    println(order.id)
}
```

### Regole
- Le espressioni in `when` e `yield` devono essere **pure** (no fails, no I/O, no mutazione) — errore compile-time se violato
- Il body `{ ... }` è imperativo: break, continue, I/O permessi
- `into` accetta funzioni di accumulo: `sum`, `count`, `max`, `min`, `collect`
- `take N` limita l'iterazione (equivalente di break con contatore)
- Nessuna lambda necessaria per filter/map/reduce

### Nuove feature
- `for x in expr where ... yield ...` — costrutto unico
- `when` — filtro puro (compile-time verified)
- `yield` — trasformazione (produce lista)
- `into` — accumulo (reduce)
- `take N` — limite
- `List<T>` — tipo builtin

### Trade-off accettati
- Meno potente delle lambda per casi complessi (trasformazioni multi-step)
- Sintassi nuova (non standard industria)
- `when` puro = non puoi filtrare su risultato di una chiamata esterna (devi pre-fetchare)
- Per i casi complessi (20%) potrebbe servire comunque lambda in futuro

### Fonti
- C# LINQ query syntax: Meijer "LINQ: Reconciling Object, Relations and XML" (2006)
- Python list comprehension: PEP 202 (2000)
- SQL SELECT/WHERE/GROUP BY: modello mentale familiare
- Khatchadourian et al. "An Empirical Study on the Use and Misuse of Java 8 Streams" (FASE 2020) — 39% parallelStream problematico, motivazione per clausole pure

### Originalità
Nessun linguaggio mainstream enforced la purezza nelle clausole di iterazione a compile-time. Questo previene il problema "I/O dentro filter/map" che affligge Java Streams.

### Stato
⚠️ Parzialmente implementato (2026-05-10)
- Grammatica: for-in con when/take/yield/into
- TypeChecker: purezza delle clausole `when` verificata (no fails, no actor calls)
- Test: 1 valid (clausole pure), 2 invalid (impure when rifiutato)
- Mancano: codegen completo, benchmark L5, List<T> builtin

