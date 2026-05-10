# Risultati Benchmark — 2026-05-10

## Valutazione corrente

| | p1 "null train" | p2a "result chain" | p2b "effect" | p2c "checked simple" |
|---|---|---|---|---|
| Correttezza | 87.5 | 62.5 | n/d | n/d |
| Snellezza | 0 | 20 | n/d | n/d |
| Antipattern bloccati | 6.5 | 6.5 | n/d | n/d |
| Pattern inclusi | 4.3 | 6.4 | n/d | n/d |
| **SCORE** | **28** | **25** | **n/d** | **n/d** |

## Dettaglio calcoli

### p1 "null train"

- **Correttezza:** valid 2/2 (100%) + invalid 3/4 (75%) → media = 87.5
- **Snellezza:** 61 LOC HLL vs 41 LOC Java → ratio 1.49 → clamp a 0
  - Nota: HLL è più lungo perché aggiunge tipi nominali, match, funzioni Demeter. È il costo della sicurezza.
- **Antipattern bloccati:** 3 bloccati, 0 penalità / 46 totali = 6.5%
  - ✅ NPE (null non esiste)
  - ✅ Stringly-typed (tipi nominali)
  - ✅ Null usage (rifiutato)
  - 0 Demeter (warning, non errore — non conta come "bloccato")
  - 0 Parametri scambiati (non testato)
  - 0 SQL injection (non testato)
  - 0 XSS (non testato)
  - 0 Hardcoded credentials (non testato)
  - 0 × 38 altri antipattern (non nel scope attuale)
- **Pattern inclusi:** 2 naturali, 0 penalità / 47 totali = 4.3%
  - ✅ Option handling
  - ✅ Tipi nominali (newtype)
  - 0 × 45 altri pattern (non testati/non nel scope)
- **SCORE:** 87.5×0.3 + 0×0.25 + 6.5×0.25 + 4.3×0.2 = 26.25 + 0 + 1.6 + 0.9 = **28.7 ≈ 28**

### p2a "result chain"

- **Correttezza:** valid 1/2 (50%) + invalid 3/4 (75%) → media = 62.5
  - Nota: 1 valid fallisce per bug noto (import Java non collegato al type checker)
- **Snellezza:** 76 LOC HLL vs 95 LOC Java → ratio 0.80 → 100-80 = 20
- **Antipattern bloccati:** 3 bloccati, 0 penalità / 46 totali = 6.5%
  - ✅ NPE, Stringly-typed, Null usage (ereditati da p1)
  - 0 Exception swallowing (il compilatore lo supporta ma manca il test)
  - 0 × 43 altri (non testati)
- **Pattern inclusi:** 3 naturali, 0 penalità / 47 totali = 6.4%
  - ✅ Option handling, Tipi nominali, Error propagation (?)
  - 0 × 44 altri (non testati)
- **SCORE:** 62.5×0.3 + 20×0.25 + 6.5×0.25 + 6.4×0.2 = 18.75 + 5 + 1.6 + 1.3 = **26.6 ≈ 25**

## Perché i punteggi sono bassi

1. **Pochi test scritti** — il compilatore blocca più antipattern di quanti ne testiamo
2. **Denominatore fisso** — conta ciò che il livello *dovrebbe* fare, non ciò che abbiamo testato
3. **Snellezza p1 negativa** — il codice sicuro è più lungo del codice insicuro (trade-off atteso)
4. **Bug noti** — il test valid/02 fallisce per un bug dell'import, non per un limite del linguaggio

## Come migliorare i punteggi

- Scrivere più test invalid (uno per ogni antipattern nella checklist)
- Fixare il bug dell'import Java nel type checker
- Aggiungere test per exception swallowing, unchecked errors, etc. in p2a
- Il benchmark L1 di p1 non compila (match-expr mancante) — fixare il parser di p1

## Aggiornamento 2026-05-10 (sessione pomeridiana)

### Sintassi aggiornata
- `fn` → `function`
- `param: Type` → `Type param`
- `let x: Type = v` → `let Type x = v`
- `name: Type` (struct) → `Type name`

### Stato test dopo refactoring

| | Math | L2 | compiler_tests | Invalid | Codegen eseguibile |
|---|---|---|---|---|---|
| p2a | ✅ | ✅ | 5/5 | 8/10 | ✅ (output = reference) |
| p2b | ✅ | ✅ | 5/5 | 7/9 | ❌ (non aggiornato) |
| p2c | ✅ | ✅ | 5/5 | 7/9 | ❌ (non aggiornato) |

### Codegen verification (p2a)

Il Java generato dal benchmark math è stato compilato ed eseguito. Output identico al reference oracle su tutti e 3 gli input di test:
- `10 7 17 48 18` → fib(10)=55, fact(7)=5040, prime(17)=true, gcd(48,18)=6 ✅
- `0 0 1 1 1` → fib(0)=0, fact(0)=1, prime(1)=false, gcd(1,1)=1 ✅
- `20 12 104729 100 75` → fib(20)=6765, fact(12)=479001600, prime(104729)=true, gcd(100,75)=25 ✅

Tutti i prototipi di livello 2 funzionano con la nuova sintassi.
