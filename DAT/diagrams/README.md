# Diagrammes LikeC4 — creedengo-infra

Ce dossier contient le modèle d'architecture du plugin **creedengo-infra**
exprimé en [LikeC4](https://likec4.dev), un DSL textuel pour le modèle
[C4](https://c4model.com) (Contexte / Container / Composant) + vues
dynamiques (séquences).

## Fichiers

| Fichier              | Rôle                                                                |
|----------------------|---------------------------------------------------------------------|
| `specification.c4`   | Méta-modèle : types d'éléments (actor, system, container…) et relations. |
| `model.c4`           | Modèle : acteurs, systèmes externes, le plugin et ses composants internes. |
| `views.c4`           | Vues statiques : contexte, containers, composants, cycle de vie d'une règle, flot de scan. |
| `dynamic-views.c4`   | Vues dynamiques (séquences) : ajout d'une règle, démarrage Sonar, exécution d'un scan. |

## Vues fournies

### Statiques (C4)
- **`contexte`** — Niveau 1 : `creedengo-infra` dans son écosystème
  (développeur IaC, contributeur, DevOps, SonarQube, sonar-scanner, sonar-iac,
  creedengo-rules-specifications).
- **`containers`** — Niveau 2 : JAR plugin + ressources embarquées
  (profils JSON), placé dans `extensions/plugins/`.
- **`components`** — Niveau 3 : architecture interne du JAR
  (`InfraPlugin`, `Infra<Lang>RulesDefinition`, `Infra<Lang>QualityProfile`,
  `Infra<Lang>CheckRegistrar`, `AbstractInfraIacSensor`,
  `Infra<Lang>Sensor`, sensors transverses, checks, utils).
- **`ruleLifecycle`** — Vue focalisée : les composants traversés pour qu'une
  règle GCIxxxx devienne **active** et soit **exécutée**.
- **`scanFlow`** — Vue focalisée : pipeline de scan
  (scanner → sensor → AST → check → issue).

### Dynamiques (séquences)
- **`addRule`** — Étapes effectuées par un contributeur pour ajouter une règle.
- **`serverStartup`** — Enregistrement des extensions au démarrage SonarQube.
- **`scanExecution`** — Exécution d'un scan, du `sonar-scanner` à l'issue Sonar.

## Visualisation locale

LikeC4 fournit un mode preview interactif (HTML + recherche + diagrammes
auto-layoutés Mermaid/Dagre) :

```bash
# Lancer un serveur local (npx tire la version a jour)
npx -y likec4 serve DAT/diagrams

# Exporter en HTML statique
npx -y likec4 build DAT/diagrams --output DAT/diagrams/dist

# Exporter une vue en PNG/SVG (via Playwright)
npx -y likec4 export png  DAT/diagrams --output DAT/diagrams/out
npx -y likec4 export svg  DAT/diagrams --output DAT/diagrams/out
```

> Prérequis : Node.js 18+. La première exécution télécharge la CLI.

## Extension VS Code / JetBrains

- VS Code : extension **LikeC4** (auto-complétion, preview live).
- JetBrains : plugin **LikeC4** (Marketplace, support du fichier `.c4`).

## Conventions

- Les noms d'éléments en `camelCase`.
- Le système racine analysé est `creedengoInfra` (le plugin lui-même).
- Tous les autres systèmes (`sonarqube`, `scanner`, `iacRepo`, `rulesSpecs`,
  `sonarIac`) sont **externes** et apparaissent grisés/`slate` dans les vues.
- Les acteurs ont une seule responsabilité : `developer`, `contributor`,
  `devops`.

