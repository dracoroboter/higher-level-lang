# Meta-programma benchmark

## Scopo

Un programma Java di riferimento che esercita tutti i pattern/antipattern rilevanti. Ogni prototipo HLL deve produrre lo **stesso output** sugli stessi input. L'uguaglianza si giudica pragmaticamente: stesso output su un campione di input scelti per essere problematici.

## Struttura del programma

Il programma:
1. Riceve input (da argomenti o stdin)
2. Costruisce una struttura dati complessa e nidificata
3. La stampa (serializzazione)

## Cosa esercita

### Antipattern testati

| Antipattern | Come lo esercita |
|---|---|
| NPE / null | Campi opzionali nella struttura nidificata |
| Stringly-typed | Email, URL, valuta, come stringhe in Java |
| Demeter | Navigazione profonda nella struttura |
| Exception swallowing | Parsing dell'input può fallire |
| Tipi primitivi sbagliati | Numeri con semantica (currency, even, prime) |

### Pattern testati

| Pattern | Come lo esercita |
|---|---|
| Option handling | Campi assenti gestiti senza NPE |
| Tipi nominali | Email, URL, Currency distinguibili |
| Error propagation | Catena di operazioni fallibili |
| Validazione al confine | Input stringa → tipo validato |

## Il problema della decidibilità

**ATTENZIONE:** alcuni vincoli di tipo sono facili da verificare (email = regex), altri sono **computazionalmente costosi o indecidibili**:

| Tipo | Verificabile a compile-time? | Verificabile a runtime? | Costo |
|---|---|---|---|
| Email | ❌ | ✅ regex | O(n) |
| URL | ❌ | ✅ regex | O(n) |
| EvenNumber | ❌ | ✅ `n % 2 == 0` | O(1) |
| PositiveInt | ❌ | ✅ `n > 0` | O(1) |
| Currency (2 decimali) | ❌ | ✅ check formato | O(1) |
| PrimeNumber | ❌ | ✅ test primalità | O(√n) |
| DivisorsOfPrime | ❌ | ✅ ma richiede fattorizzazione | O(√n) per ogni candidato |
| PerfectNumber | ❌ | ✅ ma costoso | O(n) |

**Lezione:** il linguaggio non può promettere che `PrimeNumber(x)` sia verificato a compile-time. Il `where` è **sempre** un check runtime al confine (quando il valore entra nel tipo). Il compilatore garantisce solo che il check **esista** — non che sia veloce.

**Implicazione per il benchmark:** includere almeno un tipo "tranello" (es. PrimeNumber) per verificare che il linguaggio gestisca correttamente il caso "validazione costosa" senza pretendere magia.

## Il problema della serializzazione

Stampare una struttura complessa (serializzazione) è un problema a sé:
- In Java: `toString()` manuale o libreria (Jackson, Gson)
- In HLL: serve un meccanismo. Opzioni:
  - Derivazione automatica (`struct` → `toString` generato)
  - Trait `Printable` implementato per ogni tipo
  - Funzione `serialize()` nella stdlib

Per il benchmark, la serializzazione è **fuori scope del type system** — usiamo una funzione builtin `to_string()` che il codegen traduce in un `toString()` Java generato automaticamente per i record.

## Input problematici (campione di test)

| Input | Perché è problematico |
|---|---|
| Email valida: `"alice@example.com"` | Happy path |
| Email invalida: `"not-an-email"` | Deve fallire con errore chiaro |
| Email vuota: `""` | Edge case |
| Numero pari: `42` | Happy path per EvenNumber |
| Numero dispari: `7` | Deve fallire per EvenNumber |
| Numero primo: `17` | Happy path per PrimeNumber |
| Numero grande primo: `104729` | Verifica che il check non sia O(n²) |
| Numero non primo: `15` | Deve fallire per PrimeNumber |
| Campo opzionale presente | Navigazione completa |
| Campo opzionale assente | Deve gestire None senza crash |
| Catena profonda con None a metà | Il punto dove Java crasherebbe con NPE |
| Currency: `"19.99"` | Happy path |
| Currency: `"19.999"` | Troppe cifre decimali — deve fallire |
| URL: `"https://example.com"` | Happy path |
| URL: `"ftp://wrong"` | Schema non accettato — deve fallire |

## Giudizio di uguaglianza

Due programmi (Java template e HLL prototipo) sono **equivalenti** se:
- Sugli input validi producono lo **stesso output** (stringa identica)
- Sugli input invalidi producono **entrambi un errore** (il messaggio può differire, ma entrambi rifiutano)

Non si richiede che il messaggio di errore sia identico — solo che il comportamento (accetta/rifiuta) sia lo stesso.

## Critica e decisioni

### Cosa funziona bene in questo approccio
- Il benchmark è concreto e misurabile
- Gli input problematici testano i casi reali
- Il "tranello" (PrimeNumber) verifica che il linguaggio non prometta troppo

### Cosa è rischioso
- **La serializzazione è un rabbit hole** — per il prototipo, generazione automatica di toString è sufficiente
- **Il benchmark potrebbe essere troppo piccolo** per testare tutti i pilastri — ma per il livello 1-2 è adeguato
- **Il confronto output-based non testa la leggibilità** — quella resta nelle metriche LOC/snellezza

### Decisione: cosa includere nel benchmark Java

Il programma Java template deve essere:
- ~50-80 LOC (abbastanza per essere significativo, non troppo per essere gestibile)
- Un singolo file `Benchmark.java`
- Eseguibile con `java Benchmark <input.json>`
- Output: la struttura serializzata o un messaggio di errore
