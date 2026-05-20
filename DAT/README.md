# Dossier d'Architecture Technique (DAT)

Ce dossier regroupe la documentation d'architecture technique du projet **creedengo-infra**.

## Sommaire

| # | Document | Description |
|---|----------|-------------|
| 1 | [01-presentation.md](01-presentation.md) | Présentation générale, objectifs et périmètre fonctionnel |
| 2 | [02-architecture.md](02-architecture.md) | Architecture logique, schémas et composants |
| 3 | [03-stack-technique.md](03-stack-technique.md) | Stack technique, dépendances, prérequis |
| 4 | [04-arborescence-projet.md](04-arborescence-projet.md) | Arborescence du projet et conventions |
| 5 | [05-demarrage-projet.md](05-demarrage-projet.md) | Build, démarrage local & déploiement du plugin |
| 6 | [06-ajout-nouvelle-regle.md](06-ajout-nouvelle-regle.md) | **Guide pas-à-pas : ajouter une nouvelle règle** (Check → Sensor → Registrar → Plugin) |
| 7 | [07-tests-qualite.md](07-tests-qualite.md) | Stratégie de test, qualité de code, CI |
| 8 | [08-glossaire.md](08-glossaire.md) | Glossaire et références |
| 📐 | [diagrams/](diagrams/) | **Modèle d'architecture en LikeC4** (`.c4`) — vues C4 statiques + séquences dynamiques |

## Diagrammes LikeC4

Les diagrammes d'architecture sont écrits en [LikeC4](https://likec4.dev)
dans le dossier [`diagrams/`](diagrams/README.md). Cinq vues statiques
(`contexte`, `containers`, `components`, `ruleLifecycle`, `scanFlow`) et trois
vues dynamiques (`addRule`, `serverStartup`, `scanExecution`) y sont fournies.

Visualisation locale :

```bash
npx -y likec4 serve DAT/diagrams       # preview interactif (HTML)
npx -y likec4 build DAT/diagrams       # export HTML statique
npx -y likec4 export png DAT/diagrams  # export PNG/SVG des vues
```

## Version

| Version | Date       | Auteur       | Remarques            |
|---------|------------|--------------|----------------------|
| 1.0     | 2026-05-19 | Équipe Infra | Création initiale    |

