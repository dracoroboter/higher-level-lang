# hll-p1 "null train"

## Stato: PROTOTIPO FUNZIONANTE (2026-05-10)

## Obiettivo

Eliminare NPE e violazioni della Legge di Demeter a compile-time.

## Pilastri testati

- **Pilastro 1** (Type-state): `Option<T>` come stato `Some | None`, accesso solo dopo pattern matching
- **Pilastro 2** (Tipi nominali): no `String`/`Int` nudi nelle interfacce pubbliche

## Architettura

```
Source .hll → [Parser ANTLR] → Parse Tree → [AstBuilder] → AST → [TypeChecker] → errori/ok → [JavaCodeGen] → Java source
```

### Componenti

| File | Ruolo |
|---|---|
| `grammar/Hll.g4` | Grammatica ANTLR4 |
| `src/.../ast/Node.java` | AST (sealed interfaces + records) |
| `src/.../ast/AstBuilder.java` | Visitor ANTLR → AST |
| `src/.../checker/TypeChecker.java` | Type checker (Option, nominali, Demeter) |
| `src/.../codegen/JavaCodeGen.java` | Genera Java source |
| `src/.../Main.java` | Entry point CLI |

### Stack tecnico

- Java 21
- ANTLR 4.13.1
- Maven
- Target: Java source (record + Optional)

## Risultati dei test

### Test invalidi (devono essere RIFIUTATI)

| Test | Risultato | Messaggio |
|---|---|---|
| `01_direct_option_access.hll` | ✅ RIFIUTATO | "Cannot access field 'city' on Option\<Address\> without matching" |
| `02_wrong_nominal_type.hll` | ✅ RIFIUTATO | "Cannot pass String as Email (parameter 'to' of 'send')" |
| `03_null_usage.hll` | ✅ RIFIUTATO | "'null' does not exist in HLL. Use Option\<T\> for absent values" |
| `04_demeter_violation.hll` | ✅ WARNING | "Law of Demeter: chain depth 4 exceeds max 2" |

### Test validi (devono COMPILARE)

| Test | Risultato | Note |
|---|---|---|
| `01_option_navigation.hll` | ✅ COMPILA | Genera Java con record + Optional |
| `02_option_propagation.hll` | ⚠️ FALSO POSITIVO | Import Java non collegato al type checker |

## Esempio di output

Input HLL:
```hll
type CityName = String
type Email = String where matches("^.+@.+$")

struct Address {
    city: CityName
    street: String
}

struct Customer {
    name: String
    email: Email
    address: Option<Address>
}

fn shipping_city(order: Order) -> CityName {
    match order.customer.address {
        Some(addr) => addr.city
        None => CityName("Unknown")
    }
}
```

Java generato:
```java
import java.util.Optional;

public class HllGenerated {
    public record CityName(String value) {
        @Override public String toString() { return value; }
    }

    public record Email(String value) {
        @Override public String toString() { return value; }
    }

    public record Address(CityName city, String street) {}

    public record Customer(String name, Email email, Optional<Address> address) {}

    public static CityName shipping_city(Order order) {
        if (order.customer().address().isPresent()) {
            var addr = order.customer().address().get();
            addr.city();
        } else {
            new CityName("Unknown");
        }
    }
}
```

## Come usare

```bash
cd prototypes/hll-p1/src

# Generare il parser (una volta)
java -jar target/antlr4-4.13.1-complete.jar -visitor -no-listener \
    -package dev.hll.parser \
    -o target/generated-sources/antlr4/dev/hll/parser \
    -Xexact-output-dir src/main/antlr4/Hll.g4

# Compilare
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn compile

# Eseguire (check only)
mvn exec:java -Dexec.mainClass=dev.hll.Main \
    -Dexec.args="path/to/file.hll --check-only" -q

# Eseguire (genera Java)
mvn exec:java -Dexec.mainClass=dev.hll.Main \
    -Dexec.args="path/to/file.hll" -q
```

## Limiti noti

1. **Import Java non collegato al type checker** — le funzioni importate da Java non sono riconosciute dal type checker, causando falsi positivi sull'operatore `?`
2. **Codegen incompleto** — il match non genera `return`, i costruttori struct non sono corretti nel Java generato
3. **Nessun builtin** — `println` e simili non sono definiti
4. **Single file** — nessun module system, tutto in un file
5. **Demeter è solo warning** — non errore hard (scelta di design)

## Cosa dimostra

Il prototipo dimostra che:
1. **NPE è eliminabile a compile-time** senza `unwrap()` o scappatoie
2. **I tipi nominali prevengono parametri scambiati** con zero costo runtime
3. **La Legge di Demeter è verificabile staticamente** come warning configurabile
4. **La sintassi è leggibile** da qualsiasi programmatore Java/Kotlin in pochi minuti
5. **Il transpiler a Java è fattibile** e produce codice leggibile

## Prossimi passi

- [ ] Collegare import Java al type checker
- [ ] Fixare codegen (return in match, costruttori)
- [ ] Aggiungere builtin (println, toString)
- [ ] Scrivere il benchmark completo
- [ ] Confrontare LOC HLL vs Java equivalente
