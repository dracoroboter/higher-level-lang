# Grammatica hll-p1 "null train" — Design Decisions

## Principi di sintassi

1. **Leggibile da chiunque:** un programmatore Java/Kotlin/TypeScript deve capire un programma HLL di 50 righe in massimo qualche ora
2. **Familiare:** sintassi C-like (graffe, punto, parentesi) — no indentazione significativa, no parentesi Lisp
3. **Minimale:** poche keyword, poche regole, nessuna magia
4. **IO e librerie:** si usano direttamente le librerie Java, wrappate con import tipizzato

## Decisioni prese

| Scelta | Decisione | Motivazione |
|---|---|---|
| Blocchi | `{ }` | Familiare |
| Dichiarazione variabili | `let x: Type = value` | Chiaro, come Rust/Kotlin/Swift |
| Funzioni | `fn name(param: Type) -> ReturnType { }` | Chiaro, come Rust |
| Tipi opzionali | `Option<T>` con `?` per propagazione | Come Rust ma più semplice |
| Pattern matching | `match expr { case => body }` | Familiare |
| Nessun null | Non esiste la keyword `null` | Core del prototipo |
| Commenti | `//` e `/* */` | Standard |
| Stringhe | `"hello"` | Standard |
| Import Java | `import java class.path as LocalName` | Wrapping esplicito |
| Vincoli `where` | Funzione `(String) -> Bool`, non regex | Meccanismo nel linguaggio, policy in libreria |

### Principio: meccanismo vs policy

Il linguaggio fornisce il **meccanismo** (tipi nominali con vincolo `where`).
Le librerie forniscono la **policy** (cosa è un'email valida, cosa è un URL).

```hll
// Il where accetta qualsiasi funzione (String) -> Bool
import hll.validation as validate

type Email = String where validate.email()
type URL = String where validate.url()
type ItalianFiscalCode = String where validate.fiscal_code_it()

// Anche funzioni custom definite dall'utente:
fn short_enough(s: String) -> Bool { s.length() < 50 }
type ShortName = String where short_enough()
```

La regex NON è un costrutto del linguaggio. Se serve, la libreria di validazione la usa internamente — ma il tipo non la espone.

## Grammatica (pseudo-EBNF)

```ebnf
program     = declaration* ;

declaration = type_decl | fn_decl | import_decl ;

// --- Tipi nominali ---
type_decl   = "type" IDENT "=" base_type ("where" constraint)? ;
base_type   = "String" | "Int" | "Bool" | "Float" | IDENT ;
constraint  = IDENT "(" expr ")" ;

// --- Strutture ---
struct_decl = "struct" IDENT "{" field_decl* "}" ;
field_decl  = IDENT ":" type_expr ;

// --- Funzioni ---
fn_decl     = "fn" IDENT "(" params? ")" ("->" type_expr)? block ;
params      = param ("," param)* ;
param       = IDENT ":" type_expr ;

// --- Import Java ---
import_decl = "import" "java" STRING "as" IDENT ("{" java_mapping* "}")? ;
java_mapping = "fn" IDENT "(" params? ")" "->" type_expr ;

// --- Tipi ---
type_expr   = IDENT ("<" type_expr ">")? | "Option" "<" type_expr ">" ;

// --- Statements ---
block       = "{" statement* "}" ;
statement   = let_stmt | return_stmt | expr_stmt | match_stmt | if_stmt ;

let_stmt    = "let" IDENT (":" type_expr)? "=" expr ;
return_stmt = "return" expr? ;
expr_stmt   = expr ;

// --- Match (pattern matching obbligatorio per Option) ---
match_stmt  = "match" expr "{" match_arm+ "}" ;
match_arm   = pattern "=>" (expr | block) ;
pattern     = "Some" "(" IDENT ")" | "None" | IDENT | "_" ;

// --- If ---
if_stmt     = "if" expr block ("else" block)? ;

// --- Espressioni ---
expr        = primary (("." IDENT ("(" args? ")")?) | ("?"))* ;
primary     = IDENT | STRING | NUMBER | "true" | "false" 
            | "(" expr ")" | fn_call ;
fn_call     = IDENT "(" args? ")" ;
args        = expr ("," expr)* ;

// --- Operatori ---
expr        = ... | expr OP expr ;  // +, -, *, /, ==, !=, <, >, &&, ||
```

## Esempio: programma completo

```hll
// Tipi nominali (validazione via libreria, non regex)
import hll.validation as validate

type Email = String where validate.email()
type CityName = String
type OrderId = Int

// Strutture
struct Address {
    city: CityName
    street: String
    zip: String
}

struct Customer {
    name: String
    email: Email
    address: Option<Address>
}

struct Order {
    id: OrderId
    customer: Customer
    total: Float
}

// Funzione che in Java causerebbe NPE
fn shipping_city(order: Order) -> CityName {
    match order.customer.address {
        Some(addr) => addr.city
        None => CityName("Unknown")
    }
}

// Import da Java (null wrappato automaticamente in Option)
import java "java.sql.Connection" as DBConn {
    fn prepareStatement(sql: String) -> Option<Statement>
}

// Funzione con propagazione di Option via ?
fn find_order(db: DBConn, id: OrderId) -> Option<Order> {
    let stmt = db.prepareStatement("SELECT * FROM orders WHERE id = ?")? 
    // se prepareStatement torna None, find_order torna None
    let result = stmt.execute(id)?
    result.toOrder()
}

// Main
fn main() {
    let order = find_order(db, OrderId(42))
    match order {
        Some(o) => println("City: " + shipping_city(o))
        None => println("Order not found")
    }
}
```

## Cosa rifiuta il compilatore

```hll
// ERRORE 1: accesso diretto a Option senza match
fn bad_city(order: Order) -> CityName {
    order.customer.address.city  // ERRORE: address è Option<Address>, non Address
}

// ERRORE 2: null non esiste
let x: Customer = null  // ERRORE: 'null' non è una keyword valida

// ERRORE 3: catena troppo profonda (Demeter warning)
fn bad_deep(order: Order) -> String {
    order.customer.address?.city.name.first_char()  // WARNING: catena > 2 livelli
}

// ERRORE 4: tipo nominale sbagliato
fn send(to: Email, subject: String) { ... }
send("not an email", "hello")  // ERRORE: String non è Email
```

## Cosa NON è nello scope di hll-p1

- Nessun `state` (type-state completo) — sarà hll-p2
- Nessun module system — tutto in un file
- Nessun vincolo strutturale — niente max_dependencies
- Nessuna concorrenza
- Nessun trait/composizione — solo struct + fn
- IO via librerie Java wrappate
