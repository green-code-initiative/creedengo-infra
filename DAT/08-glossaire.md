# 8. Glossaire & références

## 8.1 Glossaire

| Terme                  | Définition                                                                 |
|------------------------|----------------------------------------------------------------------------|
| **SonarQube**          | Plateforme d'analyse statique de code.                                     |
| **Plugin Sonar**       | JAR ajouté à `extensions/plugins/` étendant Sonar (règles, sensors…).      |
| **`Plugin` (API)**     | Point d'entrée Sonar (`org.sonar.api.Plugin`) déclarant les extensions.    |
| **RulesDefinition**    | API Sonar qui publie un *repository* de règles sur le serveur.             |
| **Repository de règles** | Espace de nommage des règles côté Sonar (ex. `creedengo-infra-docker`).  |
| **QualityProfile**     | Ensemble de règles activées appliqué à un projet.                          |
| **Sensor**             | Composant exécuté au scan, qui inspecte les fichiers et remonte des issues.|
| **IacCheck**           | Interface sonar-iac pour les règles de code d'infrastructure.              |
| **InitContext**        | Permet de s'abonner à des nœuds d'AST sonar-iac via `register(Type, ...)`. |
| **CheckContext**       | API runtime utilisée par un check pour remonter une issue.                 |
| **CheckRegistrar**     | Liste statique de classes de checks (source de vérité du plugin).          |
| **GCI**                | Préfixe « Green Code Initiative » pour les clés de règles (`GCI1025`…).    |
| **IaC**                | Infrastructure as Code (Dockerfile, Helm, Terraform, CFN, manifests YAML).|
| **creedengo way**      | Nom du profil qualité par défaut publié par le plugin.                     |
| **sonar-iac**          | Bibliothèque officielle SonarSource fournissant les parsers AST IaC.       |

## 8.2 Liens utiles

### Internes
- [`README.md`](../README.md) — présentation publique du projet
- [`RULES.md`](../RULES.md) — liste des règles supportées
- [`doc/IMPORT-NOTES.md`](../doc/IMPORT-NOTES.md) — notes d'import & TODO
- [`doc/RULES-JUSTIFICATIONS.TEMPLATE.md`](../doc/RULES-JUSTIFICATIONS.TEMPLATE.md) — gabarit de justification

### Externes
- [Green Code Initiative](https://green-code-initiative.org)
- [SonarQube Plugin API](https://docs.sonarsource.com/sonarqube/latest/extension-guide/developing-a-plugin/plugin-basics/)
- [sonar-iac (SonarSource/sonar-iac)](https://github.com/SonarSource/sonar-iac)
- [creedengo-rules-specifications](https://github.com/green-code-initiative/creedengo-rules-specifications)

## 8.3 Versions du document

| Version | Date       | Auteur       | Changement        |
|---------|------------|--------------|-------------------|
| 1.0     | 2026-05-19 | Équipe Infra | Création initiale |

