# Compare

Script di confronto automatico tra prototipi.

## Metriche raccolte

- LOC per risolvere il benchmark
- Numero di annotazioni/tipi richiesti (boilerplate)
- Programmi errati rifiutati a compile-time (da `tests/invalid/`)
- Programmi validi compilati con successo (da `tests/valid/`)
- LOC del codice generato

## Uso

```bash
./compare.sh hll-p1 hll-p2 ...
```

Esegue la test suite su ogni prototipo e produce un report comparativo.
