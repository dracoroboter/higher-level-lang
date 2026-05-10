# Lineage — Grafo di derivazione dei prototipi

## Grafo

```
p1 "null train"
├──→ p2a "result chain"
├──→ p2b "effect"
└──→ p2c "checked simple"
```

## Tabella (machine-readable)

| Proto | Padri | Nickname | Tema | Stato | Score |
|-------|-------|----------|------|-------|-------|
| p1 | — | null train | null safety + tipi nominali + Demeter | ✅ funzionante | 28 |
| p2a | p1 | result chain | eccezioni come valori (Result + ?) | ✅ funzionante | 25 |
| p2b | p1 | effect | effetti algebrici dichiarati | 🔲 solo benchmark | n/d |
| p2c | p1 | checked simple | checked exceptions migliorate | 🔲 solo benchmark | n/d |

## Come leggere la genealogia

Per ricostruire la genealogia completa di un prototipo, risali la colonna "Padri" ricorsivamente.

Esempio: `p4c` con padri `p3b, p2b`
```
p4c
├── p3b (padre 1)
│   └── p2a
│       └── p1
└── p2b (padre 2)
    └── p1
```

Significa: p4c eredita le capacità di p3b (che include p2a che include p1) + le capacità di p2b (che include p1).

## Convenzioni

- **Nome:** `p<livello><variante>` — max 3 caratteri
- **Padri:** dichiarati qui e nell'INTENT.md di ogni prototipo
- **Struttura:** DAG (grafo aciclico diretto), non albero — un prototipo può avere più padri
- **Vincitore:** quando un livello produce un vincitore, i prototipi successivi derivano da quello
- **Merge:** un prototipo può combinare idee di linee diverse (più padri)
