# Test Framework Nativo

## Costrutto `test`

```hll
test "description" {
    // test body
}
```

Un test è un blocco di codice che verifica un comportamento. Il compilatore li esegue con `hll --test file.hll`.

## Keyword

| Keyword | Significato | Tipo di check |
|---|---|---|
| `test "desc" { }` | Dichiara un test case | — |
| `assert expr` | Verifica che expr sia true | Runtime |
| `expect_error { }` | Verifica che il blocco sia RIFIUTATO dal compilatore | Compile-time |

## Esempi

```hll
// Test che verifica correttezza a runtime
test "fibonacci 10 is 55" {
    assert fibonacci(10) == 55
}

// Test che verifica che il compilatore RIFIUTA codice errato
test "cannot access Option without match" {
    expect_error {
        let Order order = Order(1, Customer("Alice", Email("a@b.com"), None))
        order.customer.address.city
    }
}

test "nominal types are not interchangeable" {
    expect_error {
        let Email e = Email("a@b.com")
        let URL u = URL("https://x.com")
        send_notification(u, e)
    }
}

test "null does not exist" {
    expect_error {
        let Customer c = null
    }
}

test "SQL injection prevented by nominal types" {
    expect_error {
        let String user_input = "Robert'; DROP TABLE users;--"
        execute(user_input)
    }
}
```

## Semantica di `expect_error`

`expect_error { block }` è un **meta-test sul compilatore**:
1. Il compilatore type-checka il blocco in isolamento
2. Se il type checker produce almeno un errore → il test **passa**
3. Se il type checker non produce errori → il test **fallisce** ("expected error but code compiled")

Il blocco dentro `expect_error` non viene mai eseguito — è solo analizzato staticamente.

## Esecuzione

```bash
hll --test tests/all_tests.hll

# Output:
# ✅ fibonacci 10 is 55
# ✅ cannot access Option without match
# ✅ nominal types are not interchangeable
# ✅ null does not exist
# ✅ SQL injection prevented by nominal types
# 
# 5/5 tests passed
```

## Mocking (attività futura)

Per testare codice che dipende da servizi esterni, serve un meccanismo di mock:

```hll
test "order creation with mock DB" {
    mock DBConn {
        function query(Query sql) -> Result<Row, DBError> {
            Ok(Row("Alice"))
        }
    }

    let order = build_order(mock_db, "alice@x.com")
    assert order.customer.name == "Alice"
}
```

Il `mock` crea un'implementazione fake di un tipo/service che sostituisce quella reale nel contesto del test. Collegato al Pilastro 4 (injection obbligatoria): se tutte le dipendenze sono iniettate, il mock è banale — sostituisci l'inject con il mock.

### Requisiti per il mocking
- Il tipo mockato deve essere un `service` o un parametro iniettato
- Il mock deve implementare la stessa interfaccia (stessi metodi, stessi tipi)
- Il mock è locale al test — non esiste fuori dal blocco `test`
- Il compilatore verifica che il mock sia type-safe
