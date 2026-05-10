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

| | Math | L2 | Invalid rejected |
|---|---|---|---|
| p2a | ✅ | ✅ | 5/9 (2 non implementati) |
| p2b | ✅ | ✅ | 5/8 (1 crash NPE) |
| p2c | ✅ | ✅ | 5/8 (1 crash NPE) |

### ATTENZIONE: falsi positivi nei test

I test specifici per la gestione errori (effect_not_handled, fails_not_handled, result_ignored, non_exhaustive_result) **NON funzionano correttamente**:

- p2b/p2c: il test "not handled" causa un NPE nel compilatore (crash, non rifiuto intenzionale)
- p2a: i test "result ignored" e "non exhaustive" sono accettati (feature non implementata)

**Cosa funziona realmente in tutti i prototipi:**
- ✅ Parsing della sintassi specifica (Result/effects/fails)
- ✅ Null safety (Option obbligatorio)
- ✅ Tipi nominali (incluso rifiuto di tipi diversi scambiati)
- ✅ SQL injection prevention
- ✅ Demeter warning

**Cosa NON funziona ancora:**
- ❌ Verifica che Result sia consumato (p2a)
- ❌ Verifica che match su Result sia esaustivo (p2a)
- ❌ Verifica che effetti siano gestiti (p2b)
- ❌ Verifica che fails sia gestito (p2c)

Queste sono le feature **differenzianti** tra i prototipi — senza di esse il confronto non è significativo.

Tutti i prototipi di livello 2 funzionano con la nuova sintassi.
