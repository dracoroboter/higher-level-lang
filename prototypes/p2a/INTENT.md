# p2a "result chain"

## Dichiarazione di intenti

### Deriva da
- **p1** "null train" — eredita: blocco NPE, tipi nominali obbligatori, warning Demeter

### Ipotesi generale
Gli errori sono **valori**, non un flusso di controllo separato. Una funzione che può fallire lo dichiara nel tipo di ritorno (`Result<T, E>`). Il chiamante vede l'errore nel tipo e deve gestirlo o propagarlo esplicitamente. Non esistono eccezioni implicite, catch vuoti, o errori ignorati.

L'operatore `?` permette la propagazione concisa senza boilerplate, ma resta esplicita e tracciabile.

### Problemi risolti
| Antipattern | Come lo blocca |
|---|---|
| (da p1) NPE | Option obbligatorio |
| (da p1) Stringly-typed | Tipi nominali |
| (da p1) Demeter | Warning su catene profonde |
| Exception swallowing | Non esistono catch — devi gestire ogni caso |
| Unchecked exceptions | Tutti gli errori sono nel tipo di ritorno |
| throws Exception generico | Ogni errore è tipizzato specificamente |
| Return null su errore | Non esiste null; l'errore è nel Result |
| Errori non gestiti | Il compilatore rifiuta un Result non consumato |

### Meccanismo
```hll
fn read_config(path: FilePath) -> Result<Config, IOError> { ... }

fn load_user(path: FilePath) -> Result<UserName, AppError> {
    let cfg = read_config(path)?        // propaga IOError come AppError
    let conn = connect(cfg)?            // propaga DBError come AppError
    Ok(conn.query("...").get("name"))
}
```

### Trade-off accettati
- Ogni funzione fallibile ha `Result` nel tipo di ritorno (più verboso)
- Serve un tipo "union" per comporre errori diversi (`AppError = IOError | DBError`)
- L'operatore `?` nasconde la propagazione — meno visibile di un match esplicito
- Non c'è distinzione nativa tra errori recuperabili e fatali (serve convenzione)

### Stato
🔲 Da implementare
