# 2. Architecture

> 🎨 Les diagrammes ASCII de ce chapitre ont leur équivalent **LikeC4** dans
> [`DAT/diagrams/`](diagrams/README.md) (vues `contexte`, `containers`,
> `components`, `ruleLifecycle`, `scanFlow`, et les séquences `addRule`,
> `serverStartup`, `scanExecution`).

## 2.1 Vue d'ensemble

`creedengo-infra` est un **plugin SonarQube** packagé en JAR (packaging Maven
`sonar-plugin`). Une fois déposé dans `extensions/plugins/` d'une instance
SonarQube, il enregistre :

- des **dépôts de règles** (un par langage IaC),
- des **profils qualité « creedengo way »** (un par langage IaC),
- des **sensors** d'analyse exécutés à l'analyse (`sonar-scanner`).

## 2.2 Schéma d'architecture — composants

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                              SonarQube Server                                  │
│                                                                                │
│   ┌──────────────────────────────────────────────────────────────────────────┐ │
│   │                  Plugin JAR : creedengo-infra-plugin                     │ │
│   │                                                                          │ │
│   │   InfraPlugin  (Plugin)                                                  │ │
│   │       │                                                                  │ │
│   │       ├─► RulesDefinition   (× 5 langages)  ──► repository créé          │ │
│   │       │      InfraDockerRulesDefinition                                  │ │
│   │       │      InfraKubernetesRulesDefinition                              │ │
│   │       │      InfraTerraformRulesDefinition                               │ │
│   │       │      InfraCloudFormationRulesDefinition                          │ │
│   │       │      InfraYamlRulesDefinition                                    │ │
│   │       │                                                                  │ │
│   │       ├─► BuiltInQualityProfile (× 5)        ──► profil "creedengo way"  │ │
│   │       │      InfraDockerQualityProfile                                   │ │
│   │       │      InfraKubernetesQualityProfile                               │ │
│   │       │      InfraTerraformQualityProfile                                │ │
│   │       │      InfraCloudFormationQualityProfile                           │ │
│   │       │      InfraYamlQualityProfile                                     │ │
│   │       │                                                                  │ │
│   │       └─► Sensor (× 5 + cross-file)         ──► exécutés au scan         │ │
│   │              InfraDockerSensor       ─────────┐                          │ │
│   │              InfraKubernetesSensor            │                          │ │
│   │              InfraTerraformSensor             │   tous étendent          │ │
│   │              InfraCloudFormationSensor        ├─► AbstractInfraIacSensor │ │
│   │              InfraYamlSensor          ────────┘                          │ │
│   │              DockerignoreSensor          (cross-file)                    │ │
│   │              K8sWorkloadCrossRefSensor   (cross-file)                    │ │
│   │              HelmValuesEcoSensor         (cross-file)                    │ │
│   └──────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
                                       ▲
                                       │ analyse
                                       │
                            ┌──────────┴────────────┐
                            │  sonar-scanner (CLI)  │
                            │  côté projet client   │
                            └──────────┬────────────┘
                                       │
                                       ▼
                       ┌─────────────────────────────────┐
                       │   Code IaC à analyser :         │
                       │   Dockerfile, *.yaml, *.tf, ... │
                       └─────────────────────────────────┘
```

## 2.3 Flux d'analyse (runtime)

1. `sonar-scanner` parcourt le projet client et identifie les fichiers IaC
   (langages Sonar `docker`, `kubernetes`, `terraform`, `cloudformation`,
   `yaml`).
2. SonarQube instancie chaque `Infra<Lang>Sensor` enregistré par `InfraPlugin`.
3. `AbstractInfraIacSensor#execute(SensorContext)` :
   - construit la **liste des checks actifs** (`buildActiveChecks`) en filtrant
     sur le profil qualité courant via `context.activeRules()`,
   - itère sur tous les `InputFile` du langage,
   - pour chacun : lit le contenu, **re-parse** avec le parser sonar-iac
     adapté (`DockerParser`, `YamlParser`, `HclParser`…),
   - exécute chaque `IacCheck` en lui injectant un `SensorCheckContext` (pont
     entre l'API `InitContext` / `CheckContext` de sonar-iac et l'API
     `SensorContext` de SonarQube),
   - chaque `ctx.reportIssue(...)` est traduit en `NewIssue` posté sur le
     dépôt de règles dédié (`creedengo-infra-<lang>`).

```
┌──────────────┐   parse    ┌───────────────┐   visit   ┌─────────────────────┐
│ InputFile    │──────────► │ Tree (sonar-iac)│────────►│ IacCheck            │
│ (Dockerfile) │            │  AST + ranges │           │ (ex: MultistageCheck)│
└──────────────┘            └───────────────┘           └─────────┬───────────┘
                                                                  │ reportIssue
                                                                  ▼
                                                       ┌──────────────────────┐
                                                       │ SensorCheckContext   │
                                                       │  → SensorContext     │
                                                       │  → NewIssue (Sonar)  │
                                                       └──────────────────────┘
```

## 2.4 Pourquoi un Sensor dédié ?

`sonar-iac` 1.24/1.46 instancie ses propres analyseurs (`KubernetesAnalyzer`,
`DockerSensor`, …) avec une liste de visiteurs **codée en dur** à partir des
`CheckList` internes. **Aucun SPI public** ne permet à un plugin tiers d'y
contribuer.

Solution retenue (cf. javadoc de [`InfraPlugin`](../src/main/java/org/greencodeinitiative/creedengo/infra/InfraPlugin.java)) :

- on ajoute notre propre `Sensor` par langage,
- on **re-parse** le fichier via le parser public de sonar-iac,
- on remonte les issues sur un **repository dédié** (`creedengo-infra-<lang>`)
  qui ne peut donc pas entrer en collision avec les règles natives sonar-iac.

Le coût de la double passe d'analyse est négligeable car les fichiers IaC sont
petits.

## 2.5 Composants détaillés

### 2.5.1 `InfraPlugin`
Point d'entrée Sonar (`org.sonar.api.Plugin`). Déclare toutes les extensions
via `context.addExtension(...)`.

### 2.5.2 `Infra<Lang>RulesDefinition`
Crée le `NewRepository(<creedengo-infra-lang>, <lang>)` et charge les métadonnées
des règles (HTML + JSON) via `RuleMetadataLoader` depuis le classifier
`creedengo-rules-specifications`.

### 2.5.3 `Infra<Lang>CheckRegistrar`
**Liste statique** des classes de check actives pour un langage donné.
C'est la **source de vérité** : à la fois :
- la `RulesDefinition` (pour les méta), 
- le `Sensor` (pour l'exécution),
- les profils (indirectement, via les `ruleKey` JSON).

```java
public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
    MultistageCheck.class,
    LightweightImagesCheck.class,
    SetNonRootUserCheck.class,
    NoBuildToolsInRuntimeCheck.class
);
```

### 2.5.4 `Infra<Lang>QualityProfile`
Charge la liste des `ruleKeys` à activer depuis
`src/main/resources/org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_<lang>.json`
et les active dans le profil « creedengo way ».

### 2.5.5 `AbstractInfraIacSensor`
Squelette commun à tous les sensors par langage. Responsabilités :
- déclarer le langage et le repository (`describe`),
- filtrer les checks actifs dans le profil,
- parser le fichier (méthode abstraite `parse(String)`),
- piloter la visite via `SensorCheckContext`,
- transformer les `reportIssue(...)` en `NewIssue` SonarQube.

### 2.5.6 Checks (`org.greencodeinitiative.creedengo.infra.checks`)
Chaque check implémente `org.sonar.iac.common.api.checks.IacCheck` et porte
une annotation `@Rule(key = "GCI…")`.
La logique métier d'une règle s'exprime via `init.register(NodeType.class, …)`
sur un type d'AST sonar-iac (`Body`, `DockerImage`, `RunInstruction`,
`MappingTree` YAML, blocs HCL…).

### 2.5.7 Sensors transverses
- `DockerignoreSensor` : audit des `.dockerignore` voisins.
- `K8sWorkloadCrossRefSensor` : croise plusieurs manifests Kubernetes (Service↔Deployment, etc.).
- `HelmValuesEcoSensor` : analyse des `values.yaml` Helm sans recours à l'AST Go-template.

## 2.6 Vue logique (couches)

```
┌────────────────────────────────────────────────────────────────────┐
│  Couche d'intégration SonarQube                                    │
│   InfraPlugin · Infra<Lang>RulesDefinition · QualityProfile        │
├────────────────────────────────────────────────────────────────────┤
│  Couche d'orchestration de scan                                    │
│   AbstractInfraIacSensor · Infra<Lang>Sensor · SensorCheckContext  │
├────────────────────────────────────────────────────────────────────┤
│  Couche métier (règles éco-responsables)                           │
│   checks/*.java   (MultistageCheck, ReplicasGreaterThanNeeded, …)  │
├────────────────────────────────────────────────────────────────────┤
│  Couche utilitaire                                                 │
│   DockerCheckUtils · KubernetesCheckUtils · TerraformCheckUtils …  │
├────────────────────────────────────────────────────────────────────┤
│  Dépendance externe : sonar-iac (parsing AST, types Tree)          │
└────────────────────────────────────────────────────────────────────┘
```

