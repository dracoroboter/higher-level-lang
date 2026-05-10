# p2c "checked simple"

## Dichiarazione di intenti

### Deriva da
- **p1** "null train" — eredita: blocco NPE, tipi nominali obbligatori, warning Demeter

### Ipotesi generale
Le eccezioni checked di Java erano **l'idea giusta con l'esecuzione sbagliata**. I problemi di Java non sono nel concetto (dichiarare gli errori nella firma) ma nell'implementazione: `throws Exception` generico, catch vuoti permessi, sintassi pesante. Se elimini queste scappatoie e rendi la sintassi leggera, il modello funziona.

La funzione dichiara `fails E` — il chiamante deve gestire `E` con un handler inline o propagarlo (ma solo se lo dichiara a sua volta). Nessuna scappatoia.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p1) NPE | Option obbligatorio |
| (da p1) Stringly-typed | Tipi nominali |
| (da p1) Demeter | Warning su catene profonde |
| Exception swallowing | Nessun catch vuoto — ogni errore ha un handler |
| Unchecked exceptions | Tutti gli errori sono dichiarati con `fails` |
| throws Exception generico | Non esiste — devi dichiarare errori specifici |
| Return null su errore | Non esiste null |
| Errori non gestiti | Il compilatore rifiuta errori non handled |

### Meccanismo
```hll
fn read_config(path: FilePath) -> Config fails IOError { ... }

fn load_user(path: FilePath) -> UserName fails IOError, DBError {
    let cfg = read_config(path)    // IOError propagato (dichiarato in fails)
    let conn = connect(cfg)        // DBError propagato
    conn.query("...").get("name")
}

// Il chiamante DEVE gestire ogni errore:
let name = load_user(path, id)
    | IOError(e) => "default"
    | DBError(e) => "offline"
```

### Trade-off accettati
- Familiare per programmatori Java/Kotlin (bassa curva di apprendimento)
- La propagazione è implicita se dichiarata in `fails` (meno visibile di `?`)
- Rischio di "fails pollution" (catene lunghe di errori dichiarati)
- Nessun meccanismo nativo per retry/fallback (è solo un pattern nel handler)
- Meno potente degli effetti algebrici (solo errori, non IO/logging/stato)

### Stato
🔲 Da implementare
