# Decisioni pragmatiche e strategia di prototipazione

## Il problema dell'aliasing (critico per il Pilastro 1)

### Cos'è

L'aliasing è quando due o più variabili puntano allo stesso oggetto in memoria. È il nemico principale del type-state perché il compilatore traccia lo stato **per variabile**, non per oggetto.

```
let conn = Connection.new()    // Connection<Disconnected>
let alias = conn               // alias punta allo stesso oggetto
conn.connect()                 // conn → Connection<Connected>
alias.send("hello")            // alias in che stato è? Il compilatore non lo sa
```

### Soluzioni esistenti

| Approccio | Linguaggio | Pro | Contro |
|---|---|---|---|
| Ownership (move) | Rust | Sicuro, zero-cost | Borrow checker frustrante |
| Permessi | Plaid | Flessibile | Troppo complesso, Plaid ha fallito |
| Linear types | Clean, Idris | Formalmente elegante | Troppo restrittivo |
| Ignorare | Java, Python | Semplice | Type-state non verificabile |

### Proposta per HLL

**Ownership di default solo per i tipi con `state`**, aliasing libero per il resto.

Chi dichiara un type-state accetta il vincolo: il valore si muove (move semantics), non si copia. Per i tipi senza `state`, il linguaggio si comporta normalmente (reference semantics con GC).

Questo è un compromesso: meno potente di Rust (che applica ownership a tutto), ma più pragmatico e più semplice da imparare.

---

## Vincoli del progetto

### Realtà
- Progetto di ricerca hobbistico, tempo limitato
- L'obiettivo è **esplorare e confrontare**, non produrre un linguaggio production-ready
- Servono prototipi multipli con variazioni delle idee
- Serve un modo oggettivo/automatico per confrontare i prototipi

### Obiettivi concreti
1. Prototipi abbastanza grandi da scrivere programmi "giocattolo ma utili"
2. Confronto oggettivo tra varianti su espressività, facilità, applicabilità
3. Costruzione rapida dei prototipi (settimane, non mesi)

---

## Critica e risposte ai punti sollevati

### a) Tempo limitato → prototipi minimali

**Giusto.** La strategia corretta è:
- Non implementare un linguaggio completo — implementare un **sottoinsieme** sufficiente
- Ogni prototipo testa **1-2 pilastri**, non tutti e 5
- Il "compilatore" è un transpiler minimale, non un compilatore ottimizzante

**Rischio:** prototipi troppo piccoli per essere significativi. Serve un "programma benchmark" definito in anticipo che ogni prototipo deve poter esprimere.

### b) Confronto oggettivo/automatico

**Buona idea, ma difficile.** Metriche possibili:

| Metrica | Cosa misura | Automatizzabile? |
|---|---|---|
| LOC per risolvere il benchmark | Concisione/espressività | ✅ |
| Numero di tipi/annotazioni richiesti | Boilerplate | ✅ |
| Errori catturati a compile-time vs runtime | Sicurezza | ✅ (con test suite di programmi errati) |
| Tempo per scrivere il benchmark | Facilità d'uso | ❌ (soggettivo, ma misurabile) |
| Errori nel programma finale | Correttezza | ✅ (test suite) |
| Leggibilità | Manutenibilità | ❌ (soggettivo) |

**Proposta concreta:** definire una **test suite di programmi** (corretti + errati) che ogni prototipo deve compilare/rifiutare. I programmi errati contengono antipattern noti — il prototipo migliore è quello che ne rifiuta di più a compile-time con meno boilerplate.

### c) Dimensione sufficiente ma non eccessiva

**D'accordo.** Il "programma benchmark" dovrebbe essere qualcosa come:
- Un piccolo servizio REST (3-5 endpoint)
- Con un database (connessione = risorsa con stato)
- Con autenticazione (token = tipo nominale)
- Con error handling (errori tipizzati)
- Con un paio di state machine di business logic

Questo è abbastanza per testare tutti e 5 i pilastri ma abbastanza piccolo da scrivere in un weekend.

### d) Target: JVM (Java)

**Buona scelta, ma non l'unica.** Confronto:

| Target | Pro | Contro |
|---|---|---|
| **JVM (Java bytecode)** | GC gratis, ecosistema enorme, ben documentato, cross-platform | Bytecode complesso, startup lento |
| **JavaScript** | Ubiquo, facile da generare, REPL immediato | Performance mediocre, semantica strana |
| **WASM** | Veloce, sandboxed, futuro del web | Ecosistema giovane, no GC nativo (per ora) |
| **LLVM IR** | Performance nativa, ottimizzazioni gratis | Complessità enorme, devi gestire la memoria |
| **Interprete custom** | Massima velocità di sviluppo del prototipo | Lento a runtime, non confrontabile con linguaggi reali |

**La mia raccomandazione: transpila a un linguaggio ad alto livello, non a bytecode.**

Transpilare a **Java source** (non bytecode) o a **Kotlin** è molto più veloce da implementare:
- Non devi capire il bytecode JVM
- Il codice generato è leggibile e debuggabile
- Ottieni GC, librerie, e performance gratis
- Puoi usare il compilatore Java/Kotlin come backend

Alternativa: transpilare a **TypeScript** — ancora più veloce da generare, eseguibile ovunque, e il type system di TS può validare parte del codice generato.

**Per un prototipo hobbistico, la velocità di implementazione del transpiler è più importante della performance del codice generato.**

### e) Gestione della memoria

**Risolto dalla scelta del target.** Se transpili a JVM o JS, il GC è gratis. Non devi affrontare il problema.

L'unica interazione con la memoria è:
- I tipi con `state` hanno move semantics **a livello del nostro linguaggio** (il compilatore HLL verifica che non ci siano alias)
- Ma il codice generato usa normali reference + GC del target
- Il "move" è un vincolo del type checker HLL, non un'operazione runtime

Questo è un vantaggio enorme: ottieni le garanzie di Rust (no use-after-free, no double-close) senza dover implementare un allocatore. Il GC del target gestisce la memoria, il type checker di HLL gestisce la correttezza.

### f) Velocità di costruzione dei prototipi

**Critico.** Per costruire prototipi velocemente:

1. **Parser:** usa un parser generator (ANTLR per JVM, tree-sitter, o PEG). Non scrivere il parser a mano.
2. **Type checker:** è la parte più importante e più complessa. Dedica qui il tempo.
3. **Code generation:** transpila a source code leggibile. È la parte più facile.
4. **Runtime:** zero. Usa il runtime del target.

**Stima tempi per un prototipo minimale:**
- Parser (con ANTLR): 2-3 giorni
- AST + type checker (1 pilastro): 1-2 settimane
- Transpiler a Java/Kotlin: 2-3 giorni
- Programma benchmark: 1-2 giorni
- **Totale: ~3 settimane per prototipo**

Con variazioni tra prototipi (cambi il type checker, non il parser/transpiler), i prototipi successivi sono più veloci.

---

## Strategia proposta

### Motivazione dal dolore reale

Il primo prototipo deve risolvere un problema che **senti ogni giorno**. I due candidati:

1. **NPE** — il classico `NullPointerException` di Java. Kotlin l'ha mitigato ma non eliminato (interop Java reintroduce null). Il nostro linguaggio non ha null, punto.

2. **Violazione della Legge di Demeter** — catene tipo `order.getCustomer().getAddress().getCity().getName()` dove ogni `.` è un potenziale NPE e un accoppiamento nascosto. Le librerie Java che usi quotidianamente ti forzano a farlo.

Questi due problemi sono **collegati**: la Legge di Demeter è violata perché Java non ha un modo ergonomico di navigare strutture nullable in profondità. Risultato: catene di `.get()` con NPE a ogni passo.

### Come HLL risolve entrambi in un colpo

```
// In Java (il dolore quotidiano):
String city = order.getCustomer().getAddress().getCity().getName();
// 4 potenziali NPE, violazione Demeter, accoppiamento a 4 livelli

// In HLL — nessun null, navigazione sicura:
let city: CityName = order.customer.address.city.name
// Se qualsiasi campo è Option, il compilatore FORZA la gestione:

let city: CityName = order.customer?.address?.city?.name
    | None => "Unknown"
// Oppure errore compile-time se non gestisci il caso None

// Meglio ancora — rispetta Demeter con un metodo dedicato:
let city: CityName = order.shipping_city()
// Il compilatore può AVVISARE se accedi a più di N livelli di profondità
```

**Pilastri coinvolti:**
- Pilastro 1 (type-state): `Option` come stato `Some | None`, accesso solo dopo check
- Pilastro 2 (tipi nominali): `CityName` non è `String` — non puoi confonderlo
- Pilastro 3 (vincoli strutturali): warning se la catena supera N livelli (Demeter enforced)

### Primo prototipo: hll-p1 "null train"

**Codice formale:** `hll-p1`
**Nickname:** "null train" (null + train wreck / violazione Demeter)

```
hll-p1 "null train"
  → Pilastro 1 (Option senza unwrap) + Pilastro 2 (tipi nominali base)
  → Transpila a Java source
  → Benchmark: navigazione di un modello di dominio con dati opzionali
  → Test suite: programmi con NPE latenti → devono essere RIFIUTATI
  → Metrica Demeter: warning/errore se catena > 2 livelli su tipi esterni
  → Tempo stimato: 3 settimane
```

Questo prototipo è motivante perché:
- Risolve un problema che senti **ogni giorno**
- Il risultato è visibile: "questo programma Java crasherebbe, HLL lo rifiuta"
- È piccolo ma significativo
- Tocca 2 pilastri (type-state + tipi nominali) senza essere troppo ambizioso

### Il problema dell'interop con librerie Java

La Legge di Demeter è violata **dalle librerie che usi**, non dal tuo codice. Questo è un problema di interop:

```
// La libreria Java espone:
public Customer getCustomer() { ... }  // può tornare null
public Address getAddress() { ... }    // può tornare null

// In HLL, l'import da Java deve wrappare automaticamente:
import java OrderService {
    fn getCustomer() -> Option<Customer>  // null → None automatico
}

// Ora il compilatore ti forza a gestire:
let customer = orderService.getCustomer()
    | None => return OrderError.NoCustomer
```

Il transpiler genera il null-check nel codice Java prodotto. Zero costo concettuale per il programmatore HLL, sicurezza totale.

---

```
Prototipo 1: Type-state minimale
  → Solo Pilastro 1 (state machine verificate)
  → Transpila a Java
  → Benchmark: gestione connessione DB

Prototipo 2: Tipi nominali
  → Solo Pilastro 2 (no String nelle API)
  → Stesso parser, diverso type checker
  → Benchmark: API REST con validazione

Prototipo 3: Combinato
  → Pilastro 1 + 2 + 4 (type-state + tipi nominali + module system)
  → Benchmark: mini-servizio completo

Confronto automatico:
  → Stessa test suite su tutti i prototipi
  → Metriche: LOC, errori catturati, boilerplate
```

---

## Rischi e mitigazioni

| Rischio | Mitigazione |
|---|---|
| Prototipi troppo piccoli per essere significativi | Definire benchmark in anticipo |
| Troppo tempo sul parser, poco sul type checker | Usare parser generator |
| Confronto soggettivo | Definire metriche automatiche prima di iniziare |
| Scope creep (aggiungere feature) | Ogni prototipo testa max 2 pilastri |
| Perdere motivazione | Primo prototipo funzionante in 3 settimane |
