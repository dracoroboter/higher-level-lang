# Database Antipattern

## Formato

Ogni antipattern ha: nome, categoria, descrizione, conseguenze, e analisi rispetto al linguaggio.

---

## Antipattern di correttezza

### Null Pointer Exception (NPE)
- **Descrizione:** Ogni riferimento può essere null; il linguaggio non obbliga a controllare.
- **Conseguenze:** Crash a runtime, errori imprevedibili, codice difensivo ovunque.
- **Bloccabile con type-state?** SÌ. `Option<T>` è uno stato `Some | None`; il valore è accessibile solo in `Some`.

### Temporal Coupling (Open senza Close)
- **Descrizione:** Il codice funziona solo se le operazioni avvengono in un ordine preciso non enforced.
- **Conseguenze:** Resource leak, uso di risorse non inizializzate, doppia chiusura.
- **Bloccabile con type-state?** SÌ. È il caso d'uso primario del type-state.

### Race Condition / Data Race
- **Descrizione:** Accesso concorrente a dati mutabili senza sincronizzazione.
- **Conseguenze:** Corruzione dati, comportamento non deterministico.
- **Bloccabile con type-state?** Parzialmente. L'ownership (Rust) è più appropriato. Ma lo stato `Locked | Unlocked` di una risorsa è type-state.

### Unchecked Error / Exception Swallowing
- **Descrizione:** Errori ignorati, catch vuoti, return code non controllati.
- **Conseguenze:** Fallimenti silenziosi, stato corrotto, debug impossibile.
- **Bloccabile con type-state?** SÌ. `Result<T,E>` è uno stato `Success | Failure`; il valore è accessibile solo dopo gestione.

### Use After Free / Dangling Reference
- **Descrizione:** Accesso a memoria già deallocata.
- **Conseguenze:** Crash, vulnerabilità di sicurezza.
- **Bloccabile con type-state?** SÌ. Dopo `free()`, il tipo è in stato `Freed` — nessun metodo disponibile.

### Double Free
- **Descrizione:** Deallocare la stessa risorsa due volte.
- **Conseguenze:** Corruzione heap, crash, vulnerabilità.
- **Bloccabile con type-state?** SÌ. Dopo `free()` il tipo è terminale — `free()` non è più disponibile.

### Buffer Overflow
- **Descrizione:** Scrivere oltre i limiti di un buffer.
- **Conseguenze:** Corruzione memoria, vulnerabilità di sicurezza critica.
- **Bloccabile con type-state?** No. Serve bounds checking a runtime o tipi dipendenti (dependent types).

### Integer Overflow
- **Descrizione:** Operazione aritmetica che supera il range del tipo intero.
- **Conseguenze:** Risultati errati, vulnerabilità.
- **Bloccabile con type-state?** No. Serve checked arithmetic di default (come Rust in debug) o tipi con range.

### Type Confusion
- **Descrizione:** Trattare un valore come se fosse di un tipo diverso (cast unsafe).
- **Conseguenze:** Comportamento indefinito, vulnerabilità.
- **Bloccabile con type-state?** No. Si elimina con un type system forte senza cast unsafe.

---

## Antipattern di design

### God Object / God Class
- **Descrizione:** Una classe che fa troppo, accumula responsabilità, diventa il centro del sistema.
- **Conseguenze:** Impossibile da testare, modificare, o capire. Accoppiamento totale.
- **Bloccabile con type-state?** NO. È un problema di cardinalità/struttura. Serve un vincolo strutturale (max dipendenze, max metodi).

### Mutable Global State
- **Descrizione:** Variabili globali mutabili accessibili da ovunque.
- **Conseguenze:** Effetti collaterali nascosti, impossibile ragionare sul codice, test non isolabili.
- **Bloccabile con type-state?** Parzialmente. Serve injection obbligatoria + immutabilità di default.

### Deep Inheritance / Inheritance Hell
- **Descrizione:** Gerarchie di ereditarietà profonde e fragili.
- **Conseguenze:** Fragile base class problem, accoppiamento verticale, override inattesi.
- **Bloccabile con type-state?** NO. Si elimina rimuovendo l'ereditarietà dal linguaggio (solo composizione + trait).

### Circular Dependencies
- **Descrizione:** Moduli che dipendono l'uno dall'altro ciclicamente.
- **Conseguenze:** Impossibile compilare separatamente, ordine di inizializzazione indefinito.
- **Bloccabile con type-state?** NO. È un problema del module system (grafo aciclico enforced).

### Primitive Obsession
- **Descrizione:** Usare tipi primitivi (int, string) per concetti di dominio.
- **Conseguenze:** Nessuna type safety, parametri scambiabili, validazione sparsa.
- **Bloccabile con type-state?** NO. Serve tipi nominali obbligatori (newtype enforced).

### Stringly-typed
- **Descrizione:** Usare String per tutto: email, URL, SQL, path, ID.
- **Conseguenze:** Injection, parametri scambiati, nessuna validazione.
- **Bloccabile con type-state?** NO. Caso specifico di Primitive Obsession. Serve newtype + validazione.

### Anemic Domain Model
- **Descrizione:** Oggetti di dominio che contengono solo dati, senza comportamento. La logica è in "service" esterni.
- **Conseguenze:** Logica sparsa, violazione dell'incapsulamento, duplicazione.
- **Bloccabile con type-state?** NO. È un problema di design OO. Possibile mitigazione: il linguaggio incoraggia metodi sui tipi (come Rust `impl`).

### Feature Envy
- **Descrizione:** Un metodo usa più dati di un altro oggetto che del proprio.
- **Conseguenze:** Accoppiamento sbagliato, logica nel posto sbagliato.
- **Bloccabile con type-state?** NO. Richiede analisi semantica del dominio.

### Poltergeist / Gypsy Wagon
- **Descrizione:** Classi che esistono solo per invocare metodi di altre classi, senza stato proprio.
- **Conseguenze:** Complessità inutile, indirezione senza valore.
- **Bloccabile con type-state?** NO. Problema di design. Possibile warning se un tipo non ha stato e delega tutto.

### Blob / Winnebago
- **Descrizione:** Variante del God Object: un oggetto che contiene troppi dati eterogenei.
- **Conseguenze:** Struttura dati ingestibile, violazione SRP.
- **Bloccabile con type-state?** NO. Stesso meccanismo del God Object: limiti strutturali.

### Yo-Yo Problem
- **Descrizione:** Gerarchia di ereditarietà dove devi saltare su e giù per capire il flusso.
- **Conseguenze:** Codice incomprensibile, debug impossibile.
- **Bloccabile con type-state?** NO. Si elimina rimuovendo l'ereditarietà profonda.

---

## Antipattern di struttura/codice

### Spaghetti Code
- **Descrizione:** Codice senza struttura, flusso di controllo intricato, nessuna separazione.
- **Conseguenze:** Impossibile da capire, modificare, o testare.
- **Bloccabile con type-state?** NO. Problema di disciplina che nessun type system risolve completamente.

### Copy-Paste Programming
- **Descrizione:** Duplicazione di codice anziché astrazione.
- **Conseguenze:** Bug duplicati, manutenzione moltiplicata.
- **Bloccabile con type-state?** NO. Rilevabile solo con analisi statica (clone detection).

### Lava Flow
- **Descrizione:** Codice morto o sperimentale che resta nel codebase perché nessuno osa toccarlo.
- **Conseguenze:** Complessità accidentale, confusione.
- **Bloccabile con type-state?** NO. Problema organizzativo. Il compilatore può segnalare codice unreachable.

### Golden Hammer
- **Descrizione:** Usare lo stesso tool/pattern per tutto.
- **Conseguenze:** Soluzioni inadatte, over-engineering.
- **Bloccabile con type-state?** NO. Problema umano.

### Magic Numbers / Magic Strings
- **Descrizione:** Valori letterali hardcoded senza nome o spiegazione.
- **Conseguenze:** Codice incomprensibile, errori di battitura, manutenzione difficile.
- **Bloccabile con type-state?** NO. Mitigabile con tipi nominali (forza a dare un nome) + lint.

### Shotgun Surgery
- **Descrizione:** Un cambiamento richiede modifiche in molti punti sparsi.
- **Conseguenze:** Rischio di dimenticanze, fragilità.
- **Bloccabile con type-state?** NO. Problema architetturale.

### Boat Anchor
- **Descrizione:** Codice o infrastruttura mantenuta "perché potrebbe servire" ma mai usata.
- **Conseguenze:** Complessità, costi di manutenzione.
- **Bloccabile con type-state?** NO. Il compilatore può segnalare codice/tipi non usati (dead code elimination).

### Big Ball of Mud
- **Descrizione:** Sistema senza architettura riconoscibile, cresciuto organicamente.
- **Conseguenze:** Impossibile da capire, estendere, o riscrivere parzialmente.
- **Bloccabile con type-state?** NO. Mitigabile con module system forte + vincoli di dipendenza.

---

## Antipattern di concorrenza

### Callback Hell
- **Descrizione:** Nesting profondo di callback per operazioni asincrone.
- **Conseguenze:** Codice illeggibile, error handling impossibile.
- **Bloccabile con type-state?** Parzialmente. Si elimina con async/await nativo.

### Deadlock
- **Descrizione:** Due o più thread si bloccano aspettando risorse detenute dall'altro.
- **Conseguenze:** Sistema bloccato.
- **Bloccabile con type-state?** Parzialmente. L'ordine di acquisizione dei lock è un protocollo (type-state), ma il deadlock in generale è indecidibile.

### Thread Unsafe Singleton
- **Descrizione:** Singleton senza sincronizzazione in contesto multi-thread.
- **Conseguenze:** Istanze multiple, stato corrotto.
- **Bloccabile con type-state?** SÌ se il service è gestito dal runtime (non dal programmatore).

### Busy Waiting / Spin Lock
- **Descrizione:** Loop attivo in attesa di una condizione, consumando CPU.
- **Conseguenze:** Spreco risorse, latenza.
- **Bloccabile con type-state?** NO. Serve un modello di concorrenza che non espone loop espliciti (async/channels).

### Priority Inversion
- **Descrizione:** Thread a bassa priorità blocca thread ad alta priorità detenendo un lock.
- **Conseguenze:** Starvation, latenza imprevedibile.
- **Bloccabile con type-state?** NO. Problema del runtime/scheduler.

---

## Antipattern di API/interfaccia

### Leaky Abstraction
- **Descrizione:** L'astrazione espone dettagli implementativi.
- **Conseguenze:** Il client dipende dall'implementazione, non dall'interfaccia.
- **Bloccabile con type-state?** NO. Problema di design dell'interfaccia.

### Boolean Blindness
- **Descrizione:** Funzioni che restituiscono `bool` perdendo informazione sul significato.
- **Conseguenze:** Il chiamante non sa cosa significa `true` vs `false`.
- **Bloccabile con type-state?** Parzialmente. Mitigabile con tipi nominali: `Authorized | Denied` anziché `bool`.

### Train Wreck / Law of Demeter violation
- **Descrizione:** Catene di chiamate `a.getB().getC().getD().doSomething()`.
- **Conseguenze:** Accoppiamento profondo, fragilità.
- **Bloccabile con type-state?** NO. Mitigabile limitando la profondità di accesso (lint) o con Lens.

### God Interface
- **Descrizione:** Interfaccia con troppi metodi che forza implementazioni parziali.
- **Conseguenze:** Violazione ISP, implementazioni con `throw NotImplemented`.
- **Bloccabile con type-state?** NO. Mitigabile con limiti strutturali (max metodi per trait) + composizione di trait piccoli.

### Inconsistent Return Types
- **Descrizione:** Funzione che restituisce tipi diversi a seconda del path (null, eccezione, valore speciale).
- **Conseguenze:** Il chiamante non sa cosa aspettarsi.
- **Bloccabile con type-state?** SÌ. Con Result/Option obbligatori, il tipo di ritorno è sempre esplicito e uniforme.

---

## Antipattern di sicurezza

### SQL Injection
- **Descrizione:** Input utente concatenato direttamente in query SQL.
- **Conseguenze:** Accesso non autorizzato, data breach, distruzione dati.
- **Bloccabile con type-state?** NO direttamente. Si elimina con tipi nominali: `Query` non è costruibile da `String` raw, solo da prepared statements.

### Cross-Site Scripting (XSS)
- **Descrizione:** Input utente inserito in HTML senza escaping.
- **Conseguenze:** Esecuzione di codice malevolo nel browser della vittima.
- **Bloccabile con type-state?** NO direttamente. Mitigabile con tipi nominali: `HtmlSafe` vs `RawInput`. Il template accetta solo `HtmlSafe`.

### Hardcoded Credentials
- **Descrizione:** Password, API key, secret nel codice sorgente.
- **Conseguenze:** Compromissione se il codice è esposto.
- **Bloccabile con type-state?** NO. Mitigabile con tipo `Secret` che non è costruibile da literal + injection da ambiente.

### Insecure Deserialization
- **Descrizione:** Deserializzare dati non fidati senza validazione.
- **Conseguenze:** Remote code execution, injection.
- **Bloccabile con type-state?** Parzialmente. Il dato deserializzato è in stato `Untrusted` fino a validazione esplicita → poi diventa `Validated<T>`.

---

## Antipattern di performance

### N+1 Query
- **Descrizione:** Eseguire una query per ogni elemento di una lista anziché una query batch.
- **Conseguenze:** Performance catastrofica con dataset grandi.
- **Bloccabile con type-state?** NO. Problema di pattern di accesso dati. Possibile warning se il compilatore rileva query in loop.

### Premature Optimization
- **Descrizione:** Ottimizzare codice prima di avere dati su dove sia il bottleneck.
- **Conseguenze:** Complessità inutile, codice illeggibile, ottimizzazione nel posto sbagliato.
- **Bloccabile con type-state?** NO. Problema umano.

### Memory Leak
- **Descrizione:** Allocare memoria senza mai rilasciarla.
- **Conseguenze:** Consumo crescente di memoria, crash eventuale.
- **Bloccabile con type-state?** SÌ (caso specifico di Open senza Close). Ownership + type-state garantiscono il rilascio.

### Chatty Interface
- **Descrizione:** API che richiede molte chiamate per un'operazione logica.
- **Conseguenze:** Latenza di rete, overhead.
- **Bloccabile con type-state?** NO. Problema di design API.

---

## Statistiche

- **Correttezza:** 9
- **Design:** 11
- **Struttura/codice:** 8
- **Concorrenza:** 5
- **API/interfaccia:** 5
- **Sicurezza:** 4
- **Performance:** 4
- **TOTALE:** 46 antipattern
