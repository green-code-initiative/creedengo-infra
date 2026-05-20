# 1. Présentation générale

## 1.1 Contexte

**creedengo-infra** est un plugin SonarQube développé sous l'égide de la
[Green Code Initiative](https://green-code-initiative.org). Il fait partie de
la famille des plugins *creedengo* (anciennement *ecoCode*) dont la vocation
est de fournir des **analyseurs statiques de code à vocation éco-responsable**.

Là où `creedengo-java`, `creedengo-python`, etc. ciblent le code applicatif,
`creedengo-infra` se concentre sur le **code d'infrastructure (IaC : Infrastructure
as Code)** et identifie les motifs qui dégradent l'empreinte environnementale :
sur-allocation de ressources, images conteneur lourdes, absence d'autoscaling,
réplicas surdimensionnés, etc.

## 1.2 Objectifs

- Détecter les **mauvaises pratiques d'IaC ayant un impact environnemental**.
- Fournir un **plugin SonarQube** publiant des règles dans des dépôts de règles
  dédiés (un par langage IaC).
- Activer ces règles via un **profil qualité « creedengo way »** intégré.
- S'intégrer aux pipelines CI/CD standards (SonarScanner, GitHub Actions, etc.).

## 1.3 Périmètre fonctionnel

Le plugin couvre les langages d'IaC suivants, chacun avec son propre dépôt de
règles `creedengo-infra-<lang>` :

| Langage IaC      | Repository SonarQube           | Exemples de fichiers analysés                   |
|------------------|--------------------------------|-------------------------------------------------|
| Docker           | `creedengo-infra-docker`       | `Dockerfile`, `Containerfile`                   |
| Kubernetes / Helm| `creedengo-infra-kubernetes`   | manifests `*.yaml`, charts Helm                 |
| Terraform        | `creedengo-infra-terraform`    | `*.tf`                                          |
| CloudFormation   | `creedengo-infra-cloudformation`| templates CFN (`*.yaml` / `*.json`)            |
| YAML / CI        | `creedengo-infra-yaml`         | `.github/workflows/*.yml`, `.gitlab-ci.yml`     |

## 1.4 Acteurs et utilisateurs

| Acteur                | Rôle                                                                 |
|-----------------------|----------------------------------------------------------------------|
| Développeur IaC       | Reçoit les remontées de règles via l'IDE / la PR.                    |
| Équipe DevOps / SRE   | Configure le profil qualité « creedengo way » sur l'instance Sonar.  |
| Architecte logiciel   | Définit la liste des règles obligatoires et les seuils de qualité.   |
| Contributeur plugin   | Ajoute / maintient les règles (voir [06-ajout-nouvelle-regle.md](06-ajout-nouvelle-regle.md)).|

## 1.5 Contraintes techniques majeures

- **Compatibilité SonarQube** : 9.9 LTA → 25.3 (cf. README).
- **Java 17** requis pour la compilation.
- **sonar-iac 1.24+** est utilisé comme bibliothèque de parsing IaC.
- Le plugin **ne peut pas étendre nativement** la `CheckList` de sonar-iac
  (pas de SPI public côté SonarSource) → solution : un *Sensor* dédié par
  langage qui **re-parse** les fichiers et exécute les checks creedengo
  (voir [02-architecture.md](02-architecture.md)).

