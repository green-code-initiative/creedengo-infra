# 7. Tests, qualité de code et CI

## 7.1 Stratégie de test

| Niveau               | Outils                          | Emplacement                                  | Objectif                                              |
|----------------------|---------------------------------|----------------------------------------------|-------------------------------------------------------|
| Unitaire             | JUnit 5, AssertJ, Mockito       | `src/test/java/.../checks/*Test.java`        | Vérifier chaque règle sur des fixtures représentatives |
| Verifier sonar-iac   | `Verifier` (clone interne)      | `src/test/java/org/sonar/iac/commons/testing/`| Confronter l'AST aux annotations `// Noncompliant`    |
| Intégration plugin   | Orchestrator SonarQube          | profil Maven `its`                           | Vérifier le plugin sur une vraie instance Sonar       |
| Couverture           | Jacoco 0.8.12                   | `target/site/jacoco/`                        | Maintenir un taux de couverture > 80 %                |

## 7.2 Exécuter

```bash
./mvnw test                       # unitaires
./mvnw verify                     # + jacoco
./mvnw -P its verify              # intégration (lance une SonarQube via Orchestrator)
```

## 7.3 Convention de fixture

```
src/test/resources/checks/
└── <NomDuCheck>/
    ├── Dockerfile          # ou values.yaml, deployment.yaml, *.tf …
    └── Dockerfile.noncompliant
```

Dans la fixture, marquer les lignes attendues :

```dockerfile
FROM maven:3-eclipse-temurin-17  # Noncompliant {{Use a multi-stage build…}}
RUN mvn package
```

## 7.4 Qualité de code (SonarCloud)

Le projet s'analyse lui-même : configuration dans `sonar-project.properties`.
Le badge de qualité figure dans le `README.md`.

## 7.5 CI GitHub Actions

Pipeline principal : `.github/workflows/ci.yml` (badge dans le README).
Étapes typiques :

1. Checkout
2. Setup JDK 17
3. Cache Maven
4. `./mvnw -B verify`
5. Upload des rapports Jacoco / Surefire
6. Analyse SonarCloud

## 7.6 Release

Workflow assisté via :

```bash
./tool_release_1_prepare.sh    # bump de version, tag
./tool_release_2_branch.sh     # branche de release
```

Le JAR publié est attaché à la release GitHub et déployé sur la marketplace
SonarQube (suivi via `doc/IMPORT-NOTES.md`).

