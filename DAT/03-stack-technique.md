# 3. Stack technique

## 3.1 Langages & runtimes

| Élément              | Version            | Source                |
|----------------------|--------------------|-----------------------|
| Java                 | 17                 | `pom.xml` (`java.version`) |
| Maven                | 3.9+ (via `mvnw`)  | wrapper inclus        |
| Packaging Maven      | `sonar-plugin`     | `pom.xml`             |

## 3.2 Dépendances principales

| Dépendance                              | Version                  | Rôle                                                  |
|-----------------------------------------|--------------------------|-------------------------------------------------------|
| `sonar-plugin-api`                      | 12.0.0.2960              | API du plugin SonarQube                               |
| `sonar-iac` (commons / docker / kubernetes / terraform / cloudformation / yaml) | 1.24.0.7839 | Parsers AST IaC réutilisés par les sensors            |
| `sonar-analyzer-commons`                | 2.17.0.3322              | `RuleMetadataLoader`, `BuiltInQualityProfileJsonLoader` |
| `creedengo-rules-specifications`        | `main-SNAPSHOT`          | Métadonnées HTML+JSON des règles (multi-classifiers)  |
| `snakeyaml`                             | 2.4                      | Parsing YAML (utilitaires)                            |
| `sonar-packaging-maven-plugin`          | 1.23.0.740               | Empaquetage du JAR plugin                             |

### Test
| Dépendance         | Version  | Rôle                       |
|--------------------|----------|----------------------------|
| JUnit Jupiter      | 5.12.2   | Framework de tests         |
| Mockito            | 5.17.0   | Mocks                      |
| AssertJ            | 3.27.3   | Assertions fluides         |
| Jacoco             | 0.8.12   | Couverture de code         |
| Orchestrator Sonar | 25.12.0  | Tests d'intégration        |

## 3.3 Compatibilité SonarQube

| Plugin Version | SonarQube       |
|----------------|-----------------|
| 0.1.0+         | 9.9 LTA → 25.3  |

## 3.4 Outils annexes

- **Docker / Docker Compose** : stack locale SonarQube + PostgreSQL pour tester
  le plugin (`docker-compose.yml`).
- **Makefile** : raccourcis `make build`, `make docker-init`, etc.
- **SonarCloud** : qualité du projet lui-même (`sonar-project.properties`).
- **GitHub Actions** : CI (`.github/workflows/ci.yml`).

## 3.5 Prérequis machine de développement

| Outil          | Version minimale | Remarque                          |
|----------------|------------------|-----------------------------------|
| JDK            | 17               | requis par `maven.compiler.target` |
| Docker Engine  | 24+              | pour la stack locale Sonar         |
| Docker Compose | v2               | invoqué via `docker compose`       |
| Git            | 2.30+            |                                    |

