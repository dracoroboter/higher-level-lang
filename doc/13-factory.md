# Factory Method in HLL: già nativo

## Il pattern

Il Factory Method (GoF) esiste per disaccoppiare la creazione di un oggetto dal suo tipo concreto. In Java/C++ serve perché:
1. Nascondere il tipo concreto (ritornare un'interfaccia)
2. Validazione alla creazione (smart constructor)
3. Nomi descrittivi (più costruttori con significati diversi)

## Come HLL lo rende nativo (senza costrutto aggiuntivo)

### Caso 1: Nascondere il tipo concreto

**Java (serve Factory):**
```java
interface Logger { void log(String msg); }
class FileLogger implements Logger { ... }
class ConsoleLogger implements Logger { ... }

Logger createLogger(String env) {
    if (env.equals("prod")) return new FileLogger();
    return new ConsoleLogger();
}
```

**HLL (nativo con service/provide):**
```hll
export service Logger {
    function log(String msg) -> Unit
}

provide Logger {  // quale provide è attivo dipende dal module graph
    function log(String msg) -> Unit { ... }
}
```

Il chiamante dipende da `Logger` (interfaccia). L'implementazione è iniettata dal module system. Non serve un factory — il `provide` È il factory.

### Caso 2: Validazione alla creazione (smart constructor)

**Java (serve Factory):**
```java
public class Email {
    private String value;
    private Email(String v) { this.value = v; }
    public static Email of(String raw) {
        if (!isValid(raw)) throw new IllegalArgumentException();
        return new Email(raw);
    }
}
```

**HLL (nativo con type + where):**
```hll
type Email = String where validate.email()
```

La validazione è nel tipo stesso. Non puoi creare un `Email` invalido — il compilatore lo impedisce. Non serve un factory.

### Caso 3: Nomi descrittivi (named constructors)

**Java (serve Factory):**
```java
Color.fromRGB(255, 0, 0);
Color.fromHex("#FF0000");
Color.fromName("red");
```

**HLL (funzioni normali):**
```hll
function fromRGB(Int r, Int g, Int b) -> Color { Color(r, g, b) }
function fromHex(String hex) -> Color { ... }
function fromName(String name) -> Color { ... }
```

Una funzione che ritorna un tipo nominale È un named constructor. Non serve un costrutto speciale.

## Fonti

- Gamma, E. et al. "Design Patterns" (1994) — Factory Method originale
- Bloch, J. "Effective Java" (2001), Item 1: "Consider static factory methods instead of constructors"
- Rust Book: "Associated Functions" — `impl Type { fn new() -> Self }` rende il pattern superfluo
- Kotlin: companion object + sealed class eliminano il bisogno di factory espliciti
- gcanti, "Functional design: smart constructors" (2021) — validazione nel tipo

## Conclusione

Il Factory Method è **già nativo** in HLL senza aggiungere costrutti:
- `service` + `provide` = factory per polimorfismo (caso 1)
- `type X = T where validate()` = smart constructor (caso 2)
- Funzioni che ritornano un tipo = named constructors (caso 3)

## Casi potenzialmente non coperti (da investigare)

- **Abstract Factory** (famiglia di oggetti correlati) — coperto da module con più service?
- **Factory con stato** (factory che ricorda configurazione) — coperto da provide con needs?
- **Factory con registrazione dinamica** (plugin system) — potrebbe richiedere un meccanismo nuovo
