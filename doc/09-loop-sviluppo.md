# Loop di sviluppo dei prototipi

## Il ciclo

```
INTENT → BENCHMARK JAVA → IMPLEMENT → SCORE → COMPARE → CHOOSE → DERIVE → (loop)
```

### 1. INTENT

Scrivere `INTENT.md` per il nuovo prototipo:
- **Deriva da:** quale(i) prototipo(i) padre
- **Ipotesi generale:** cosa cambia rispetto al padre, in una frase
- **Problemi risolti:** tabella antipattern → meccanismo (include quelli ereditati)
- **Trade-off accettati:** cosa si perde o si complica

L'INTENT si scrive PRIMA del codice. È il contratto: il prototipo deve fare esattamente questo, né più né meno.

### 2. BENCHMARK JAVA

Scrivere (o estendere) il benchmark Java di riferimento per il livello:
- Il benchmark Java è scritto **con gli antipattern** che il livello vuole eliminare
- Cresce ad ogni livello: livello 2 include i problemi del livello 1 + i nuovi
- È il reference oracle: produce l'output corretto che i prototipi HLL devono replicare
- Include input problematici (edge case, valori invalidi, campi assenti)

Il benchmark Java definisce **cosa deve migliorare** il prototipo. Se il Java non ha il problema, il prototipo non ha nulla da dimostrare.

### 2. IMPLEMENT

Implementare il prototipo:
1. Creare directory (`prototypes/pXy/`)
2. Copiare codice del padre (se pragmatico)
3. Modificare grammatica ANTLR
4. Aggiornare AST, AstBuilder, TypeChecker
5. Scrivere test (`tests/invalid/` per ogni antipattern dichiarato)
6. Scrivere benchmark nella sintassi del prototipo
7. Verificare che i test passano per il motivo giusto (non crash!)

### 3. SCORE

Calcolare il punteggio con la formula:
```
SCORE = (correttezza × 0.3) + (snellezza × 0.25) + (antipattern × 0.25) + (pattern × 0.2)
```

- **Correttezza:** % test valid che compilano + % test invalid rifiutati
- **Snellezza:** LOC HLL vs LOC Java equivalente
- **Antipattern:** test rifiutati / 46 (totale database)
- **Pattern:** pattern esprimibili / 47 (totale database)

Dettagli in `tools/compare/BENCHMARK.md`.

### 4. COMPARE

Confrontare i prototipi allo stesso livello:
- Stessi test, stesso benchmark Java di riferimento
- Tabella con le 4 componenti + score finale
- Se differenza < 3 punti → pareggio → regola di spareggio (correttezza > antipattern > snellezza)

### 5. CHOOSE

Scegliere il vincitore (o i vincitori) del livello:
- Il vincitore diventa la base per il livello successivo
- Se due prototipi hanno idee complementari, possono essere entrambi padri di un merge

### 6. DERIVE

Definire il tema del livello successivo:
- Quale nuovo problema affrontare? (es. type-state, module system, concorrenza)
- Quali varianti testare? (almeno 2-3 ipotesi diverse)
- Scrivere gli INTENT dei nuovi prototipi
- Loop a 1.

---

## Regole

### Ereditarietà funzionale
Un prototipo figlio deve risolvere **almeno** gli stessi problemi del padre. Può risolverli con implementazione diversa, ma non può perdere capacità.

### DAG, non albero
Un prototipo può avere più padri (merge di idee da linee diverse). Il nome non codifica la genealogia — i padri sono dichiarati nell'INTENT.

### Retirement
Dopo 5+ prototipi totali, i livelli bassi non vengono più testati. Restano come riferimento storico.

### Corrispondenza INTENT ↔ Implementazione
Per ogni antipattern dichiarato nell'INTENT:
- Deve esistere un test che lo verifica
- Il test deve essere rifiutato per il motivo giusto (non crash)
- Se manca il test → 0 punti per quell'antipattern
- Se il test crasha → -1 punto

### Benchmark condiviso
Tutti i prototipi allo stesso livello compilano lo stesso benchmark. Il benchmark Java è il reference oracle — l'output deve essere identico sugli stessi input.

---

## Esempio concreto: da livello 2 a livello 3

```
Livello 2 completato:
  p2a "result chain" → score 35
  p2b "effect"       → score 36  ← vincitore
  p2c "checked simple" → score 30

Decisione: p2b vince. Tema livello 3: type-state (risorse con protocollo).

Nuovi prototipi:
  p3a "state strict"  (padre: p2b) — ownership obbligatoria su tipi con state
  p3b "state light"   (padre: p2b) — ownership solo su state, resto libero
  p3c "state+result"  (padri: p2b, p2a) — merge: effects + Result per interop

Ciclo: INTENT → IMPLEMENT → SCORE → COMPARE → CHOOSE → ...
```
