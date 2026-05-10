# Benchmark App — OrderSystem

Applicazione Java di riferimento per validare i prototipi HLL. Ogni prototipo deve tradurre questa app nella propria sintassi e produrre lo stesso comportamento nei test.

## Componenti

- **Server** — endpoint REST (simulati con metodi)
- **Client** — chiama il server
- **Database** — in-memory (HashMap), CRUD + query
- **Cache** — con TTL e invalidazione
- **MessageQueue** — coda messaggi con subscriber pattern
- **OrderService** — business logic con lifecycle (pending → confirmed → shipped)

## Esecuzione

```bash
cd benchmarkapp
javac OrderSystem.java OrderSystemTest.java
java OrderSystemTest        # 38 test
java OrderSystem            # demo output
```

## Output di riferimento

```
201:ORD-1
200:ORD-1,alice@example.com,Laptop,1,pending
200:confirmed
200:shipped
400:Amount must be positive
404:not found
400:Cannot ship order in status: shipped
Notifications: 3
  ORDER_CREATED:ORD-1
  ORDER_CONFIRMED:ORD-1
  ORDER_SHIPPED:ORD-1
```

## Regola

Ogni prototipo che traduce questa app deve:
1. Compilare senza errori
2. Passare test funzionalmente equivalenti a quelli Java (stessa semantica, stessa copertura, sintassi del prototipo)

Il **comportamento** è ciò che conta: stessi test, stessi risultati. Il codice generato, la struttura interna, e l'output testuale possono essere diversi. I test devono essere il più approfonditi possibile — sono la misura della correttezza.

## Evoluzione

L'app verrà resa più complessa nel tempo per stress-testare i prototipi:
- [ ] Autenticazione (token, sessioni)
- [ ] Concorrenza (più client simultanei)
- [ ] Transazioni (rollback su errore)
- [ ] Rate limiting
- [ ] Retry con backoff
