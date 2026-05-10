# Mapping Antipattern → Meccanismo HLL (p4a)

Per ogni antipattern del database (46 totali): come HLL lo previene, se lo previene, e con quale meccanismo.

Legenda stato:
- ✅ **Bloccato** — test esplicito che dimostra il rifiuto
- 🚫 **Impossibile** — il costrutto che lo causa non esiste nel linguaggio (con Java reference)
- ⚠️ **Parziale** — mitigato ma non completamente bloccato
- ❌ **Non bloccato** — HLL non lo previene
- 🔮 **Futuro** — bloccabile con feature pianificate

---

## Antipattern di correttezza

### 1. Null Pointer Exception (NPE) ✅
**Meccanismo:** `Option<T>` obbligatorio. Nessun valore può essere null. L'accesso al contenuto richiede `match` con gestione esplicita di `None`.
**Test:** `09_direct_option_access.hll`, `11_null_usage.hll`

### 2. Temporal Coupling (Open senza Close) ✅
**Meccanismo:** `state` machine. Le operazioni sono disponibili solo nello stato corretto. Il compilatore verifica le transizioni.
**Test:** `06_use_after_close.hll`, `16_use_before_open.hll`, `17_double_close.hll`

### 3. Race Condition / Data Race 🔮
**Meccanismo attuale:** nessuno — HLL non ha concorrenza.
**Piano:** quando la concorrenza verrà aggiunta, usare ownership o actor model per prevenire shared mutable state. Vedi Padovani (2018) per deadlock-free typestate.

### 4. Unchecked Error / Exception Swallowing ✅
**Meccanismo:** `fails E` obbligatorio. Se una funzione può fallire, il chiamante deve gestire l'errore con `| handler` o propagarlo dichiarando `fails`. Nessun catch vuoto possibile.
**Test:** `05_unhandled_fails.hll`

### 5. Use After Free / Dangling Reference ✅
**Meccanismo:** `state` machine. Dopo una transizione a stato terminale (es. `Closed`), nessun metodo è disponibile.
**Test:** `06_use_after_close.hll`

### 6. Double Free ✅
**Meccanismo:** `state` machine. Dopo `close()`, lo stato è `Closed` — `close()` non è più disponibile.
**Test:** `17_double_close.hll`

### 7. Buffer Overflow ❌
**Meccanismo:** nessuno. Richiederebbe bounds checking a runtime o dependent types. HLL transpila a Java che ha bounds checking nativo (ArrayIndexOutOfBoundsException), ma non è un check compile-time di HLL.

### 8. Integer Overflow ❌
**Meccanismo:** nessuno. Richiederebbe checked arithmetic o tipi con range. Possibile futuro: `type Port = Int where range(1, 65535)`.

### 9. Type Confusion ✅
**Meccanismo:** type system forte senza cast. I tipi nominali impediscono di trattare un `Email` come un `URL`. Nessun cast unsafe nel linguaggio.
**Test:** `10_wrong_nominal_type.hll`, `12_swapped_nominal_params.hll`

---

## Antipattern di design

### 10. God Object / God Class 🚫
**Meccanismo:** module system con `service` (interfacce focalizzate) + `provide` (implementazioni separate). Non esiste il concetto di "classe" monolitica.
**Java reference:** `blocked/Poltergeist.java` (correlato), `benchmark/BenchmarkL4.java` (AppManager)

### 11. Mutable Global State ✅
**Meccanismo:** il parser rifiuta variabili top-level. Tutto lo stato vive nei service, iniettati via `needs`. Nessun `static`, nessun singleton.
**Test:** `20_mutable_global_state.hll` (parse error)

### 12. Deep Inheritance / Inheritance Hell 🚫
**Meccanismo:** HLL non ha `class extends`. Solo composizione + service/provide.
**Java reference:** `blocked/DeepInheritance.java`

### 13. Circular Dependencies ✅
**Meccanismo:** il compilatore multi-file costruisce il grafo dei moduli e verifica che sia un DAG. Cicli → errore compile-time.
**Test:** `01_circular_dependency.hll`, `tests/multifile/invalid/multifile_circular/`

### 14. Primitive Obsession ✅
**Meccanismo:** tipi nominali obbligatori. `type Email = String where validate.email()` — non puoi passare una `String` dove serve un `Email`.
**Test:** `10_wrong_nominal_type.hll`, `13_sql_injection.hll`

### 15. Stringly-typed ✅
**Meccanismo:** stesso dei tipi nominali. `type Query = String` impedisce SQL injection passando raw String.
**Test:** `13_sql_injection.hll`

### 16. Anemic Domain Model ❌
**Meccanismo:** nessuno. HLL permette struct con solo dati e logica separata nelle funzioni. Possibile futuro: warning se uno struct con >N campi non ha metodi associati.

### 17. Feature Envy ❌
**Meccanismo:** nessuno. Richiederebbe analisi di quali campi una funzione accede e di quale tipo. Complesso da implementare.

### 18. Poltergeist / Gypsy Wagon 🚫
**Meccanismo:** HLL non ha classi istanziabili senza stato. Un `provide` senza `needs` e con metodi che fanno solo forwarding verrebbe catturato dal dead code detector (lava flow).
**Java reference:** `blocked/Poltergeist.java`

### 19. Blob / Winnebago ⚠️
**Meccanismo:** parziale. Il module system incoraggia separazione, ma non impedisce un singolo modulo enorme. Possibile futuro: warning su moduli con troppi simboli.

### 20. Yo-Yo Problem 🚫
**Meccanismo:** nessuna ereditarietà → nessuna navigazione su/giù tra classi.
**Java reference:** `blocked/YoYoProblem.java`

---

## Antipattern di struttura/codice

### 21. Spaghetti Code ⚠️
**Meccanismo:** parziale. Il module system forza boundaries. La Law of Demeter (warning) limita l'accoppiamento. Ma non impedisce funzioni lunghe o flusso contorto.

### 22. Copy-Paste Programming ❌
**Meccanismo:** nessuno. Richiederebbe analisi di duplicazione (come PMD/CPD). Non è un check compile-time.

### 23. Lava Flow ✅
**Meccanismo:** dead code detection. Funzioni dichiarate ma mai chiamate → warning (errore con `--strict`).
**Test:** `21_lava_flow_dead_code.hll`

### 24. Golden Hammer ❌
**Meccanismo:** nessuno. È un problema di design umano, non verificabile dal compilatore.

### 25. Magic Numbers / Magic Strings ⚠️
**Meccanismo:** parziale. I tipi nominali catturano il caso in cui un literal viene passato dove serve un tipo di dominio. Ma non blocca `let x = 42` senza contesto. Vedi `doc/04-bibliografia.md` per la decisione di design.

### 26. Shotgun Surgery ❌
**Meccanismo:** nessuno. Richiederebbe analisi di impatto (quanti moduli cambiano per una modifica). Possibile futuro con analisi delle dipendenze.

### 27. Boat Anchor ✅
**Meccanismo:** dead code detection (stesso di Lava Flow). Codice inutilizzato viene segnalato.
**Test:** `21_lava_flow_dead_code.hll`

### 28. Big Ball of Mud ⚠️
**Meccanismo:** parziale. Module system + DAG + visibility forzano struttura. Ma un singolo modulo può essere internamente caotico.

---

## Antipattern di concorrenza

### 29. Callback Hell 🚫
**Meccanismo:** HLL non ha callback asincroni. Il flusso è sequenziale con `fails` per errori.
**Java reference:** `blocked/CallbackHell.java`

### 30. Deadlock 🔮
**Meccanismo attuale:** nessuno (no concorrenza). Piano: structured concurrency o actor model.

### 31. Thread Unsafe Singleton 🚫
**Meccanismo:** no `static`, no global state, no lazy initialization manuale. I service sono gestiti dal module system.
**Java reference:** `blocked/ThreadUnsafeSingleton.java`

### 32. Busy Waiting / Spin Lock 🔮
**Meccanismo attuale:** nessuno (no concorrenza). Piano: primitive di sincronizzazione ad alto livello.

### 33. Priority Inversion 🔮
**Meccanismo attuale:** nessuno (no concorrenza/priorità).

---

## Antipattern di API/interfaccia

### 34. Leaky Abstraction ❌
**Meccanismo:** nessuno. Un service può esporre tipi interni. Possibile futuro: verificare che i tipi nell'interfaccia pubblica siano tutti `export`.

### 35. Boolean Blindness ❌
**Meccanismo:** nessuno attualmente. HLL permette parametri `Bool`.

**Cos'è:** usare `Bool` dove un enum nominale sarebbe più chiaro. Es: `setVisible(true)` vs `setVisibility(Visible)`. Con Bool, al call site non sai cosa significa `true`. Con un enum, il significato è nel tipo.

**Esempio problematico (Java):**
```java
widget.setEnabled(true, false, true); // cosa significano?
```

**Come sarebbe in HLL con enum:**
```hll
type Enabled = Bool    // non risolve
type Visibility = Visible | Hidden  // risolve!
```

**Possibile check:** warning se una funzione ha >1 parametro `Bool`. Richiederebbe un enum nominale.

### 36. Train Wreck / Law of Demeter ✅
**Meccanismo:** warning su catene di accesso > 2 livelli. Con `--strict` diventa errore.
**Test:** `14_demeter_violation.hll`, `15_demeter_deep_chain.hll`

### 37. God Interface ❌
**Meccanismo:** nessuno attualmente. Un service può avere 50 metodi.
**Possibile check:** warning se un service ha > 7 metodi (regola di Miller).

### 38. Inconsistent Return Types ❌
**Meccanismo:** nessuno. Il type checker non verifica che tutti i path di una funzione ritornino un valore.
**Possibile check:** analisi dei path di ritorno.

---

## Antipattern di sicurezza

### 39. SQL Injection ✅
**Meccanismo:** tipi nominali. `type Query = String` impedisce di passare input utente raw come query.
**Test:** `13_sql_injection.hll`

### 40. Cross-Site Scripting (XSS) ⚠️
**Meccanismo:** parziale. Tipi nominali (`type HtmlSafe = String`) possono prevenirlo, ma non è enforced di default. Dipende dal programmatore che definisce i tipi giusti.

### 41. Hardcoded Credentials ❌
**Meccanismo:** nessuno. Il compilatore non analizza il contenuto delle stringhe.

### 42. Insecure Deserialization ❌
**Meccanismo:** nessuno. È un problema di runtime/librerie.

---

## Antipattern di performance

### 43. N+1 Query ❌
**Meccanismo:** nessuno. È un problema di runtime/ORM.

### 44. Premature Optimization ❌
**Meccanismo:** nessuno. È un problema di design umano.

### 45. Memory Leak ⚠️
**Meccanismo:** parziale. Il type-state può rilevare risorse non chiuse (warning se un path non raggiunge lo stato terminale). Ma non copre tutti i casi di memory leak.

### 46. Chatty Interface ❌
**Meccanismo:** nessuno. Richiederebbe analisi del numero di chiamate cross-module.

---

## Riepilogo

| Stato | Conteggio | Percentuale |
|---|---|---|
| ✅ Bloccato (con test) | 21 | 46% |
| 🚫 Impossibile (by design) | 5 | 11% |
| ⚠️ Parziale | 5 | 11% |
| 🔮 Futuro (concorrenza) | 4 | 9% |
| ❌ Non bloccato | 11 | 24% |
| **Totale bloccati (✅ + 🚫)** | **26** | **56%** |

### Per arrivare al 70% (33/46) servono 7 in più:
- Concorrenza sicura: +4 (Race Condition, Deadlock, Busy Waiting, Priority Inversion)
- Boolean Blindness check: +1
- God Interface check: +1
- Inconsistent Return Types check: +1
