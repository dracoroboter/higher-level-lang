# Antipattern bloccati per assenza del costrutto

Questi antipattern sono impossibili in HLL perché il linguaggio non ha il meccanismo che li rende possibili. Non esiste un test `.hll` perché il codice non è nemmeno esprimibile.

Per ogni entry: il file Java dimostra l'antipattern, la colonna "Costrutto assente" spiega perché HLL lo blocca.

| # | Antipattern | Costrutto assente in HLL | Perché è impossibile | Java reference |
|---|---|---|---|---|
| 1 | Deep Inheritance | `class extends` | HLL non ha ereditarietà. Solo composizione + service/provide. Nessuna gerarchia da creare. | `blocked/DeepInheritance.java` |
| 2 | Yo-Yo Problem | `class extends` | Senza gerarchie, non c'è navigazione su/giù tra classi. | `blocked/YoYoProblem.java` |
| 3 | Callback Hell | callback asincroni | HLL non ha callback, lambda asincroni, o Promise. Il flusso è sequenziale con `fails` per gli errori. | `blocked/CallbackHell.java` |
| 4 | Poltergeist | classi istanziabili senza stato | HLL ha solo struct (dati) e service (interfacce). Non puoi creare una classe "manager" che fa solo forwarding. | `blocked/Poltergeist.java` |
| 5 | Thread Unsafe Singleton | `static`, global state | HLL non ha variabili statiche né global state. I service sono iniettati, non istanziati globalmente. | `blocked/ThreadUnsafeSingleton.java` |
