# Benchmark — Procedura di valutazione numerica dei prototipi

## Punteggio finale

```
SCORE = (correttezza * 0.3) + (snellezza * 0.25) + (antipattern_bloccati * 0.25) + (pattern_inclusi * 0.2)
```

Ogni componente è normalizzato a 0–100. Il punteggio finale è 0–100.

I pesi sono una proposta iniziale — possono essere ricalibrati dopo i primi confronti.

---

## Componente 1: Correttezza (peso 0.3)

**Cosa misura:** il compilatore fa quello che promette?

**Calcolo:**
```
correttezza = ((valid_ok / valid_total) * 50) + ((invalid_rejected / invalid_total) * 50)
```

- `valid_ok`: programmi in `tests/valid/` che compilano senza errori
- `invalid_rejected`: programmi in `tests/invalid/` che vengono rifiutati
- Punteggio 100 = tutti i valid compilano E tutti gli invalid sono rifiutati
- Punteggio 0 = nessun test passa

**Automatizzabile:** ✅ completamente

---

## Componente 2: Snellezza (peso 0.25)

**Cosa misura:** quanto codice serve per risolvere il benchmark rispetto a Java?

**Calcolo:**
```
loc_conciseness = max(0, 100 - ((loc_hll / loc_java) * 100))
cc_bonus = max(0, (cc_java - cc_hll) / cc_java * 100)
conciseness = (loc_conciseness + cc_bonus) / 2
```

Se HLL richiede meno LOC di Java E meno branch → punteggio alto.
La complessità ciclomatica contribuisce al 50% della snellezza.

### Ceremony ratio (sotto-metrica per spareggi)

Quando due prototipi hanno le stesse LOC ma verbosità diversa, si usa il ceremony ratio:
```
ceremony = (token_body_A - token_body_B) / token_body_B * 100
```

Misura i token extra (boilerplate) che un prototipo richiede rispetto all'altro per la stessa logica. Utile per discriminare sintassi diverse (es. rebinding vs mutazione in-place).

Esempio misurato (13 transizioni di stato):
- p3b (mutazione in-place): 18 token → baseline
- p3a (rebinding obbligatorio): 57 token → +216% ceremony

Il ceremony ratio si usa come criterio di spareggio quando lo score è identico.

### Complessità ciclomatica (sotto-metrica della snellezza)

```
cyclomatic_hll = conteggio di: if, match, while, for nel benchmark HLL
cyclomatic_java = conteggio di: if, while, for, switch, ?: nel benchmark Java

cyclomatic_ratio = cyclomatic_hll / cyclomatic_java
```

Misura quanti branch/decisioni servono per la stessa logica. Un valore < 1 significa che HLL richiede meno decisioni (il linguaggio gestisce i casi internamente). Un valore = 1 significa stessa complessità.

Esempio misurato (benchmark L2 exec):
- HLL: 4 branch (2 match + 2 if)
- Java: 5 branch (3 if + 1 ternary + 1 if)
- Ratio: 0.8 (HLL 20% meno complesso)

Misure aggiuntive (media pesata):
```
snellezza = (loc_ratio * 0.5) + (annotation_ratio * 0.3) + (nesting_ratio * 0.2)

loc_ratio = max(0, 100 - (loc_hll / loc_java * 100))
annotation_ratio = max(0, 100 - (type_annotations_hll / loc_hll * 200))
nesting_ratio = max(0, 100 - (max_nesting_hll * 20))
```

- `loc_hll`: righe non vuote del benchmark in HLL
- `loc_java`: righe non vuote dell'equivalente Java
- `type_annotations_hll`: numero di annotazioni di tipo esplicite
- `max_nesting_hll`: profondità massima di indentazione

**Automatizzabile:** ✅ completamente

---

## Componente 3: Antipattern bloccati (peso 0.25)

**Cosa misura:** quanti antipattern il linguaggio rende impossibili o difficili?

**Calcolo:**
```
antipattern_bloccati = (bloccati / TOTALE_ANTIPATTERN_DATABASE) * 100
```

**Il denominatore è SEMPRE il totale dal database (46 antipattern).** Non il sottoinsieme del livello. Se un prototipo blocca 5 antipattern su 46, il punteggio è 5/46 = 10.9% — indipendentemente dal livello.

**Regola dello 0:** se un antipattern non è testato, il prototipo prende 0 punti per quel caso. Non si assume che "probabilmente funzionerebbe".

**Regola del -1:** se un antipattern non è testabile per limitazioni strutturali del prototipo (es. il prototipo non ha il costrutto necessario per bloccarlo), il punteggio per quel caso è **-1**. Questo penalizza i prototipi che per design non possono risolvere certi problemi.

```
antipattern_bloccati = ((bloccati - penalità) / TOTALE_DATABASE) * 100

dove:
  bloccati = numero di test invalid rifiutati
  penalità = numero di antipattern strutturalmente non bloccabili dal prototipo
  TOTALE_DATABASE = 46
```

Esempio: p1 blocca 3 antipattern, non ne testa 5, e 38 non sono nel suo scope attuale:
```
antipattern_bloccati = (3 - 0) / 46 * 100 = 6.5%
```

Se un prototipo ha una limitazione strutturale (es. non ha type-state quindi non PUÒ bloccare temporal coupling):
```
antipattern_bloccati = (3 - 1) / 46 * 100 = 4.3%
```

---

## Componente 4: Pattern inclusi (peso 0.2)

**Cosa misura:** quanti design pattern sono esprimibili in modo naturale (senza boilerplate)?

**Calcolo:**
```
pattern_inclusi = ((naturali - penalità) / TOTALE_PATTERN_DATABASE) * 100
```

**Il denominatore è SEMPRE il totale dal database (47 pattern).** Stesse regole:
- Non testato = 0 punti
- Strutturalmente non esprimibile = -1

Un pattern è "naturale" se esprimerlo in HLL richiede ≤ 120% delle LOC della versione ideale.

---

## Procedura di esecuzione

```bash
#!/bin/bash
# tools/compare/score.sh <prototype_dir>

PROTO=$1

# 1. Correttezza
valid_ok=$(run_valid_tests $PROTO)
invalid_rej=$(run_invalid_tests $PROTO)
correttezza=$(calc_correctness $valid_ok $invalid_rej)

# 2. Snellezza
loc_hll=$(count_loc $PROTO/benchmark/*.hll)
loc_java=$(count_loc $PROTO/benchmark/*.java)
snellezza=$(calc_snellezza $loc_hll $loc_java)

# 3. Antipattern bloccati
bloccati=$(run_invalid_tests $PROTO)  # stesso dei test invalid
antipattern=$(calc_antipattern $bloccati)

# 4. Pattern inclusi
naturali=$(count_natural_patterns $PROTO)
pattern=$(calc_pattern $naturali)

# Score finale
score=$(echo "$correttezza * 0.3 + $snellezza * 0.25 + $antipattern * 0.25 + $pattern * 0.2" | bc)

echo "=== $PROTO ==="
echo "Correttezza:  $correttezza / 100"
echo "Snellezza:    $snellezza / 100"
echo "Antipattern:  $antipattern / 100"
echo "Pattern:      $pattern / 100"
echo "SCORE FINALE: $score / 100"
```

## Output esempio

```
=== p1 ===
Correttezza:  87 / 100  (6/7 valid ok, 4/4 invalid rejected)
Snellezza:    72 / 100  (HLL 28 LOC vs Java 45 LOC)
Antipattern:  80 / 100  (4/5 bloccati)
Pattern:      60 / 100  (3/5 naturali)
SCORE FINALE: 75.9 / 100

=== p2a ===
Correttezza:  95 / 100
Snellezza:    68 / 100
Antipattern:  90 / 100
Pattern:      75 / 100
SCORE FINALE: 82.7 / 100
```

## Note

- I pesi (0.3, 0.25, 0.25, 0.2) sono configurabili e discutibili
- Il benchmark Java equivalente deve essere scritto **una volta** e vale per tutti i prototipi dello stesso livello
- I test `invalid/` sono cumulativi: livello 2 include tutti quelli di livello 1 + i nuovi
- Lo score è relativo — ha senso solo nel confronto tra prototipi dello stesso livello

## Output obbligatorio

Il report deve sempre mostrare le **componenti separate** oltre allo score finale:

```
=== p2a vs p2b vs p2c ===

| | Correttezza | Snellezza | Antipattern | Pattern | SCORE |
|---|---|---|---|---|---|
| p2a | 95 | 68 | 90 | 75 | 82.7 |
| p2b | 92 | 55 | 95 | 85 | 81.1 |
| p2c | 98 | 72 | 88 | 70 | 83.1 |
```

Le componenti separate servono per capire *perché* un prototipo vince e dove è debole.

## Risoluzione pareggi

Se due prototipi hanno score finale entro **3 punti** di differenza (es. 82.7 vs 83.1), si considerano in pareggio. In caso di pareggio, si applica in ordine:

1. **Correttezza vince** — il prototipo con correttezza più alta prevale (un linguaggio che non funziona è inutile)
2. **Antipattern bloccati vince** — a parità di correttezza, chi blocca più errori prevale (è lo scopo del progetto)
3. **Snellezza vince** — a parità dei primi due, chi è più conciso prevale (ergonomia)
4. **Giudizio qualitativo** — se ancora pari, si valuta: leggibilità soggettiva, familiarità per programmatori Java, semplicità di implementazione del compilatore

## Retirement dei prototipi vecchi

**Regola pragmatica:** quando esistono almeno 5 prototipi totali, i prototipi di livello più basso non vengono più valutati né mantenuti.

Motivazione: velocità del ciclo di test. Mantenere e testare prototipi obsoleti rallenta lo sviluppo.

Eccezioni:
- Un prototipo ritirato resta disponibile come riferimento storico (codice non cancellato)
- Se un prototipo di livello alto fallisce e serve tornare indietro, si può riattivare un antenato

**Questa regola è provvisoria** — va rivalutata in corso d'opera quando ci saranno abbastanza prototipi da capire se è troppo aggressiva o troppo conservativa.
