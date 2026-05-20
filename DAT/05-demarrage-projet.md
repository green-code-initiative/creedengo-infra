# 5. Démarrage du projet

## 5.1 Cloner et préparer

```bash
git clone https://github.com/green-code-initiative/creedengo-infra.git
cd creedengo-infra
# (Optionnel) configurer Java 17 :
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
```

## 5.2 Compiler le plugin

Trois équivalents au choix :

```bash
# Via Make
make build                # = mvn clean package -DskipTests

# Via Maven Wrapper
./mvnw clean package      # avec tests
./mvnw clean package -DskipTests

# Via le script utilitaire (utilisé par le Dockerfile)
./tool_build.sh
```

Artefact produit : `target/creedengo-infra-plugin-<version>.jar`.

## 5.3 Exécuter les tests

```bash
./mvnw test                       # tests unitaires
./mvnw verify                     # + couverture Jacoco
./mvnw -P its verify              # tests d'intégration (Orchestrator)
```

Rapports :
- Surefire : `target/surefire-reports/`
- Jacoco   : `target/site/jacoco/index.html`

## 5.4 Stack SonarQube locale (Docker Compose)

Le `docker-compose.yml` lance :
- `sonar` : SonarQube 26.5 Community + plugin monté en bind-mount,
- `db`    : PostgreSQL 16.

Cycle de vie via le Makefile :

```bash
make docker-init     # docker compose up --build -d
make docker-logs     # docker compose logs -f
make start           # docker compose start
make stop            # docker compose stop
make docker-clean    # docker compose down --volumes
```

Accès SonarQube : http://localhost:9000 (admin/admin par défaut).

> ⚠️ Le bind-mount cible
> `./sonar-plugin/target/creedengo-infra-plugin-2.1.1-SNAPSHOT.jar`. Si la
> version produite localement diffère, ajuster soit le `pom.xml`, soit le
> `docker-compose.yml`.

## 5.5 Installer le plugin sur une SonarQube existante

1. Compiler : `./mvnw clean package -DskipTests`.
2. Copier le JAR :
   ```bash
   cp target/creedengo-infra-plugin-*.jar \
      /opt/sonarqube/extensions/plugins/
   ```
3. Redémarrer SonarQube.
4. Dans l'UI : *Quality Profiles* → vérifier la présence de **« creedengo way »**
   pour chacun des langages (docker, kubernetes, terraform, cloudformation, yaml).
5. Définir « creedengo way » comme profil par défaut sur les projets cibles.

## 5.6 Lancer une analyse sur un projet IaC

Côté projet client (où se trouvent les `Dockerfile`, `*.tf`, manifests k8s…) :

```bash
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<token> \
  -Dsonar.projectKey=mon-projet-iac \
  -Dsonar.sources=.
```

Les issues remontent dans les dépôts `creedengo-infra-<lang>`.

## 5.7 Scripts utilitaires (`tool_*.sh`)

| Script                       | Rôle                                                 |
|------------------------------|------------------------------------------------------|
| `tool_build.sh`              | `./mvnw clean package -DskipTests`                   |
| `tool_compile.sh`            | Compile seulement (sans packaging).                  |
| `tool_docker-init.sh`        | `docker compose up --build -d`                       |
| `tool_docker-clean.sh`       | Nettoyage de la stack Docker.                        |
| `tool_docker-logs.sh`        | Suit les logs SonarQube.                             |
| `tool_start.sh` / `tool_start_withtoken.sh` | Démarrage avec / sans token Sonar.    |
| `tool_stop.sh`               | Stoppe la stack.                                     |
| `tool_release_1_prepare.sh`  | Prépare une release (versionnage).                   |
| `tool_release_2_branch.sh`   | Crée la branche de release.                          |

