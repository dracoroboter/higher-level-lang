# Input di test per il benchmark

## Livello 1

### Input validi (devono produrre output)

```bash
# Happy path con indirizzo
java BenchmarkL1 "Alice" "alice@example.com" "19.99" "true"
# Output atteso:
# Order #1
# Customer: Alice
# Email: alice@example.com
# Total: 19.99
# City: Milano
# Zip: 20100

# Happy path senza indirizzo — IN JAVA CRASHA (NPE), in HLL deve gestire
java BenchmarkL1 "Bob" "bob@test.com" "5.00" "false"
# Output atteso (HLL):
# Order #1
# Customer: Bob
# Email: bob@test.com
# Total: 5.00
# City: Unknown
# Zip: Unknown
```

### Input invalidi (devono produrre errore in HLL, in Java passano silenziosamente)

```bash
# Email invalida — Java accetta, HLL rifiuta
java BenchmarkL1 "Charlie" "not-an-email" "10.00" "true"

# Amount invalido — Java accetta, HLL rifiuta
java BenchmarkL1 "Dave" "dave@x.com" "abc" "true"

# Amount con troppe cifre decimali — Java accetta, HLL rifiuta
java BenchmarkL1 "Eve" "eve@x.com" "19.999" "true"
```

## Livello 2 (aggiunge validazione + eccezioni)

### Input validi

```bash
# Happy path completo
java BenchmarkL2 "Alice" "alice@example.com" "19.99" "true" "42"

# Senza indirizzo
java BenchmarkL2 "Bob" "bob@test.com" "5.00" "false" "8"
```

### Input che causano errore (gestito)

```bash
# Email invalida
java BenchmarkL2 "Charlie" "not-an-email" "10.00" "true" "4"
# Output: ERROR: invalid email

# Amount invalido
java BenchmarkL2 "Dave" "dave@x.com" "abc" "true" "6"
# Output: ERROR: invalid amount

# Numero dispari dove serve pari
java BenchmarkL2 "Eve" "eve@x.com" "10.00" "true" "7"
# Output: ERROR: not an even number
```

## Criterio di uguaglianza

| Input | Java | HLL | Uguale? |
|---|---|---|---|
| Valido con indirizzo | Output X | Output X | ✅ se identico |
| Valido senza indirizzo | NPE crash | "City: Unknown" | ❌ Java crasha, HLL gestisce |
| Email invalida | Accetta silenziosamente | Errore compile-time o runtime | ❌ comportamento diverso (HLL è migliore) |

**Nota:** il benchmark Java è scritto "male" di proposito. L'obiettivo non è che HLL produca lo stesso output di Java — è che HLL produca l'output **corretto** dove Java crasha o accetta dati invalidi.

Il confronto è:
- Sugli input validi: **stesso output**
- Sugli input invalidi: **HLL rifiuta, Java accetta o crasha** (HLL è migliore)
