# HLL — Higher Level Language

A research project exploring whether a programming language can make **design patterns native constructs** and **antipatterns structurally impossible**.

## Research Question

> Is it possible to build a programming language where design patterns are simple language constructs and antipatterns are made difficult by the language's own formality?

Secondary questions:
- Is such a language **useful** in practice?
- Is it **efficient** enough for real-world use?

## Current State

**Phase:** Prototyping (feasibility study)

Four prototype compilers exist, each testing different hypotheses about error handling:

| Prototype | Nickname | Hypothesis | Status |
|---|---|---|---|
| p1 | "null train" | Null safety + nominal types + Demeter | ✅ Working |
| p2a | "result chain" | Errors as values (Result + ?) | ✅ Working |
| p2b | "effect" | Algebraic effects | ✅ Working |
| p2c | "checked simple" | Improved checked exceptions | ✅ Working |

All prototypes:
- Parse and type-check benchmark programs
- Reject programs with NPE, wrong nominal types, and null usage
- Warn on Law of Demeter violations
- Transpile to Java source

## Language Features (current)

```hll
import hll.validation as validate

type Email = String where validate.email()
type OrderId = Int

struct Customer {
    String name,
    Email email,
    Option<Address> address
}

function shipping_city(Order order) -> String {
    match order.customer.address {
        Some(addr) => addr.city.name
        None => "Unknown"
    }
}
```

Key characteristics:
- **No null** — `Option<T>` with mandatory pattern matching
- **Nominal types** — `Email` ≠ `String`, enforced at compile time
- **Validation as library** — `where` accepts any `(T) -> Bool` function
- **Demeter warnings** — deep access chains flagged at compile time
- **Strongly typed** with inference
- **Transpiles to Java** source (records + Optional)

## Project Structure

```
doc/                    Research documentation (Italian)
prototypes/
├── LINEAGE.md          Prototype derivation graph
├── p1/                 "null train" — null safety + nominal types
├── p2a/                "result chain" — Result<T,E> + ? operator
├── p2b/                "effect" — algebraic effects
├── p2c/                "checked simple" — improved checked exceptions
└── shared/             Shared code between prototypes
tools/compare/          Benchmark framework and scoring
TODO-LIST.md            Current task list
```

## Building

Requires Java 21 and Maven.

```bash
cd prototypes/p2a/src

# Generate parser (once)
java -jar target/antlr4-4.13.1-complete.jar -visitor -no-listener \
    -package dev.hll.parser \
    -o target/generated-sources/antlr4/dev/hll/parser \
    -Xexact-output-dir src/main/antlr4/Hll.g4

# Compile
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn compile

# Check a file
mvn exec:java -Dexec.mainClass=dev.hll.Main \
    -Dexec.args="path/to/file.hll --check-only" -q
```

## License

This project is licensed under the GNU General Public License v2.0 — see [LICENSE](LICENSE) for details.
