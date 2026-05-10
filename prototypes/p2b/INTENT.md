# p2b "effect"

## Dichiarazione di intenti

### Deriva da
- **p1** "null train" — eredita: blocco NPE, tipi nominali obbligatori, warning Demeter

### Ipotesi generale
Le funzioni dichiarano quali **effetti** possono avere (fallire, fare IO, loggare), ma **non decidono come gestirli**. Il chiamante decide la strategia (retry, fallback, abort, log). Questo separa "cosa può andare storto" da "cosa faccio quando va storto" — la stessa funzione può essere usata con strategie diverse in contesti diversi.

Gli effetti sono un concetto più generale degli errori: includono anche IO, logging, stato mutabile — tutto ciò che non è una computazione pura.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p1) NPE | Option obbligatorio |
| (da p1) Stringly-typed | Tipi nominali |
| (da p1) Demeter | Warning su catene profonde |
| Exception swallowing | Gli effetti devono essere gestiti dal chiamante |
| Unchecked exceptions | Tutti gli effetti sono dichiarati nella firma |
| throws Exception generico | Ogni effetto è tipizzato |
| Return null su errore | Non esiste null |
| Errori non gestiti | Il compilatore rifiuta effetti non handled |
| Retry/fallback hardcoded | Il handler decide la strategia, non la funzione |

### Meccanismo
```hll
fn read_config(path: FilePath) -> Config
    effects { IOError } { ... }

fn load_user(path: FilePath) -> UserName
    effects { IOError, DBError } {
    let cfg = read_config(path)      // IOError propagato implicitamente
    let conn = connect(cfg)          // DBError propagato
    conn.query("...").get("name")
}

// Il chiamante sceglie la strategia:
let name = handle load_user(path, id) {
    IOError(e) => "default"
    DBError(e) => retry(3) { load_user(path, id) }
}
```

### Trade-off accettati
- Concetto meno familiare (algebraic effects non sono mainstream)
- Più complesso da implementare nel compilatore
- Il corpo della funzione non mostra dove può fallire (gli effetti sono impliciti nel flusso)
- Rischio di "effect pollution" (troppi effetti dichiarati)
- Curva di apprendimento più alta per il programmatore

### Stato
🔲 Da implementare
