# p1 "null train"

## Dichiarazione di intenti

### Deriva da
Nessuno (prototipo radice).

### Ipotesi generale
I bug più comuni nel codice quotidiano (NPE, parametri scambiati, catene di accesso non sicure) sono eliminabili a compile-time con due meccanismi semplici: assenza di null con Option obbligatorio, e tipi nominali che distinguono i dati per significato, non per rappresentazione.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| Null Pointer Exception | `null` non esiste; valori assenti sono `Option<T>` con match obbligatorio |
| Stringly-typed / Primitive Obsession | Tipi nominali obbligatori nelle interfacce pubbliche |
| Parametri scambiati | Tipi nominali diversi non sono intercambiabili |
| Violazione Legge di Demeter | Warning se catena di accesso > N livelli |

### Trade-off accettati
- Il programmatore deve wrappare i valori nei tipi nominali (più verboso di Java raw)
- Il match su Option aggiunge righe rispetto a un accesso diretto (ma elimina NPE)
- La validazione `where` è a runtime (il compilatore non può verificare regex a compile-time)

### Stato
✅ Prototipo funzionante (2026-05-10)
