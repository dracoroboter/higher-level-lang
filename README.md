# HLL — Higher Level Language

A research project exploring whether a programming language can make **design patterns native constructs** and **antipatterns structurally impossible**.

## Research Question

> Is it possible to build a programming language where design patterns are simple language constructs and antipatterns are made difficult by the language's own formality?

Secondary questions:
- Is such a language **useful** in practice?
- Is it **efficient** enough for real-world use?

## Current State

**Phase:** Prototyping (feasibility study)

### Prototype Scores

| Proto | Nickname | Hypothesis | Score | Status |
|---|---|---|---|---|
| p1 | "null train" | Null safety + nominal types + Demeter | 28 | 🏚️ Retired |
| p2a | "result chain" | Errors as values (Result + ?) | 44 | 🏚️ Retired |
| p2b | "effect" | Algebraic effects | 43 | 🏚️ Retired |
| p2c | "checked simple" | Improved checked exceptions | 45 | ✅ Winner L2 |
| p3a | "state strict" | Typestate with mandatory rebinding | 48 | ✅ Working |
| p3b | "state light" | Typestate with in-place mutation | 47 | ✅ Winner L3 |
| p4a | "module" | Module system + DI + DAG deps | 47 | ✅ Working |

Score = weighted average of: correctness (30%), conciseness (25%, includes cyclomatic complexity), antipatterns blocked (25%), patterns included (20%). Scale 0–100, denominator is the full database (46 antipatterns + 47 patterns).

### Prototype Intents

**p1 "null train"** (root)
> The most common bugs (NPE, swapped parameters, unsafe access chains) are eliminable at compile-time with two simple mechanisms: no null with mandatory Option, and nominal types that distinguish data by meaning, not representation.

**p2a "result chain"** (parent: p1)
> Errors are values, not a separate control flow. A function that can fail declares it in the return type (Result<T, E>). The caller sees the error in the type and must handle or propagate it explicitly. No implicit exceptions, no empty catches, no ignored errors.

**p2b "effect"** (parent: p1)
> Functions declare which effects they can have (fail, IO, log), but don't decide how to handle them. The caller decides the strategy (retry, fallback, abort). This separates "what can go wrong" from "what to do when it goes wrong".

**p2c "checked simple"** (parent: p1)
> Java's checked exceptions were the right idea with the wrong execution. Remove `throws Exception`, remove empty catches, make the syntax lightweight, and the model works. The function declares `fails E` — the caller must handle with an inline handler or propagate.

**p3a "state strict"** (parent: p2b)
> Resources with protocols (files, connections, transactions) declare their states and transitions. The compiler verifies at compile-time that operations only happen in the correct state. Ownership is mandatory — every transition consumes the value and produces a new one (rebinding).

**p3b "state light"** (parent: p2b)
> Same as p3a but with in-place mutation instead of rebinding. The compiler tracks state internally. Less ceremony (+216% fewer tokens than p3a for the same logic), same compile-time safety.

**p4a "module"** (parents: p3b + p2c)
> The code needs boundaries. Modules declare explicit interfaces (services), dependencies form a DAG verified at compile-time, and injection is a language construct. No circular deps, no hidden dependencies, no god classes, no service locators.

### Development Loop

```
┌─────────────────────────────────────────────────────────┐
│  1. INTENT: write hypothesis + expected antipatterns     │
│                                                         │
│  2. JAVA BENCHMARK: write/extend the Java reference     │
│     with the antipatterns this level should eliminate    │
│                                                         │
│  3. IMPLEMENT: grammar + type checker + tests           │
│                                                         │
│  4. SCORE: run benchmarks, calculate score              │
│                                                         │
│  5. COMPARE: rank prototypes at same level              │
│                                                         │
│  6. CHOOSE: pick winner(s) as parent for next level     │
│                                                         │
│  7. DERIVE: new intent for next level, loop to 1        │
└─────────────────────────────────────────────────────────┘
```

**Rules:**
- A prototype inherits the **capabilities** (not necessarily the code) of its parents
- Multiple parents allowed (DAG, not tree) — merge of ideas from different lines
- Score is calculated against the full database (46 antipatterns + 47 patterns)
- Prototypes at the same level compete on the same benchmark
- After 5+ prototypes exist, lower-level ones are retired from testing
- Winner of each level becomes the base hypothesis for the next level

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
├── PROCEDURE.md        How to create/test/score prototypes
├── p1/                 "null train" — null safety + nominal types
├── p2a/                "result chain" — Result<T,E> + ? operator
├── p2b/                "effect" — algebraic effects
├── p2c/                "checked simple" — improved checked exceptions
├── p3a/                "state strict" — typestate with rebinding
├── p3b/                "state light" — typestate with in-place mutation
├── p4a/                "module" — module system + DI + DAG deps
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
