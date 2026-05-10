# Limiti trovati nella traduzione Java → HLL (p4a)

## Progetto: OrderSystem (server + client + DB + cache + coda)

Java: 291 LOC, 38 test passano
HLL: 218 LOC, 4/5 test passano (1 limite strutturale)

## Limiti trovati

### 1. `expect_error` verifica solo errori compile-time

**Problema:** il test "cannot create order with invalid amount" in Java verifica che `createOrder(amount=0)` lancia un'eccezione a runtime. In HLL, `expect_error` verifica solo che il type checker rifiuti il codice. Ma `fail ValidationError(...)` è sintassi valida — l'errore avviene a runtime, non a compile-time.

**Impatto:** non puoi testare la business logic (validazione a runtime) con `expect_error`. Serve un meccanismo di test che esegua il codice e verifichi il risultato.

**Possibile soluzione:** aggiungere `expect_fail` che esegue il codice e verifica che produca un `fail` specifico. Oppure un costrutto `assert_fails ErrorType { expr }`.

### 2. Keyword collision con nomi di variabili

**Problema:** `service`, `state`, `module`, `export`, `provide`, `needs`, `mock`, `spawn`, `await` sono keyword. Non puoi usarle come nomi di variabili o parametri. In Java, `service` è un nome di variabile comune.

**Impatto:** minore — basta rinominare (`svc`, `srv`, `orderSvc`). Ma è un friction point per chi viene da Java.

**Possibile soluzione:** rendere le keyword context-sensitive (keyword solo in posizione di dichiarazione, non come identificatore). Complica il parser.

### 3. Nessun tipo generico per collezioni

**Problema:** `List<OrderData>` è usato nel service ma HLL non ha un tipo `List` nativo. Il type checker lo accetta come tipo opaco ma non può verificare operazioni su di esso.

**Impatto:** non puoi iterare su una lista, accedere per indice, o verificare la lunghezza a compile-time.

**Possibile soluzione:** aggiungere `List<T>` come tipo builtin con metodi (`map`, `filter`, `size`, `get`).

### 4. Nessun meccanismo di concatenazione stringa tipizzata

**Problema:** `"201:" + id` funziona ma `id` è un `OrderId` (tipo nominale). La concatenazione con `+` dovrebbe richiedere una conversione esplicita? Attualmente il type checker non verifica.

**Impatto:** minore — la concatenazione è un'operazione di presentazione, non di logica.

### 5. Cache e DB richiedono tipi generici o Any

**Problema:** in Java, `Cache` usa `Object` come tipo del valore. In HLL non c'è `Object`/`Any`. Il cache deve essere tipizzato per ogni uso o avere un tipo generico.

**Impatto:** il cache nell'esempio usa `Option<String>` — funziona per stringhe ma non per oggetti complessi.

**Possibile soluzione:** generics (`service Cache<T> { function get(String key) -> Option<T> }`)

### 6. Nessun lambda/closure per subscriber

**Problema:** in Java, `queue.subscribe(msg -> notifications.add(msg))` usa una lambda. HLL non ha lambda/closure. Il subscriber deve essere un service con un metodo.

**Impatto:** il pattern observer/subscriber è più verboso in HLL.

**Possibile soluzione:** aggiungere lambda come espressioni (`|msg| notifications.add(msg)`) o usare il pattern service per i listener.

## Cosa funziona bene

1. **Tipi nominali** — `Email`, `OrderId`, `Product`, `Amount` prevengono parametri scambiati
2. **State machine** — il lifecycle dell'ordine (Pending→Confirmed→Shipped) è verificato a compile-time
3. **Error handling** — `fails` + handler inline è più chiaro di try/catch
4. **Module system** — service/provide/needs rende le dipendenze esplicite
5. **Mock** — i test possono mockare Database/Cache/Queue senza framework esterni
6. **Conciseness** — 218 LOC vs 291 LOC Java (25% più conciso)

## Priorità dei fix

1. **Alta:** `expect_fail` per test di business logic runtime
2. **Alta:** `List<T>` come tipo builtin
3. **Media:** Lambda/closure per callback
4. **Bassa:** Keyword context-sensitive
5. **Bassa:** Generics per Cache
