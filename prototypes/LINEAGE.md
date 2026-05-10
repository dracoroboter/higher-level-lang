# Lineage — Grafo di derivazione dei prototipi

## Grafo

```
p1 "null train" 🏚️ ritirato
├──→ p2a "result chain" 🏚️ ritirato
├──→ p2b "effect" 🏚️ ritirato
│    ├──→ p3a "state strict"
│    ├──→ p3b "state light" ← vincitore L3
│    └──→ p3c "state runtime" ❌ scartato
└──→ p2c "checked simple" ← vincitore L2
         │
         └──→ p4a "module" (padri: p3b + p2c)
```

## Tabella (machine-readable)

| Proto | Padri | Nickname | Tema | Stato | Score |
|-------|-------|----------|------|-------|-------|
| p1 | — | null train | null safety + tipi nominali + Demeter | 🏚️ ritirato | 28 |
| p2a | p1 | result chain | eccezioni come valori (Result + ?) | 🏚️ ritirato | 44 |
| p2b | p1 | effect | effetti algebrici dichiarati | 🏚️ ritirato | 43 |
| p2c | p1 | checked simple | checked exceptions migliorate | ✅ vincitore L2 | 40 |
| p3a | p2b | state strict | type-state con ownership obbligatoria | ✅ funzionante | 52 |
| p3b | p2b | state light | type-state con ownership solo su state | ✅ vincitore L3 | 49 |
| p3c | p2b | state runtime | type-state con verifica a runtime | ❌ scartato | — |
| p4a | p3b, p2c | module | module system + injection + DAG deps | ✅ funzionante | 47 |

## Come leggere la genealogia

Per ricostruire la genealogia completa di un prototipo, risali la colonna "Padri" ricorsivamente.

Esempio: `p4c` con padri `p3b, p2a`
```
p4c
├── p3b (padre 1)
│   └── p2b
│       └── p1
└── p2a (padre 2)
    └── p1
```

Significa: p4c eredita le capacità di p3b (che include p2b che include p1) + le capacità di p2a (che include p1).

## Convenzioni

- **Nome:** `p<livello><variante>` — max 3 caratteri
- **Padri:** dichiarati qui e nell'INTENT.md di ogni prototipo
- **Struttura:** DAG (grafo aciclico diretto), non albero — un prototipo può avere più padri
- **Vincitore:** quando un livello produce un vincitore, i prototipi successivi derivano da quello
- **Merge:** un prototipo può combinare idee di linee diverse (più padri)
