# 6. Ajouter une nouvelle règle — Guide pas-à-pas

Ce chapitre est le **cœur opérationnel** du DAT : il décrit, de bout en bout,
comment créer une nouvelle règle creedengo-infra, du **Check** Java jusqu'à
sa **distribution et son exécution effective** par SonarQube.

Pour la version visuelle de ce chapitre, voir les vues LikeC4 :
- statique : `DAT/diagrams/views.c4` → vue **`ruleLifecycle`**
- dynamique : `DAT/diagrams/dynamic-views.c4` → séquences **`addRule`**,
  **`serverStartup`** et **`scanExecution`**.

---

## 6.1 La chaîne complète : pourquoi *six* maillons ?

Pour qu'une règle GCIxxxx finisse réellement par produire une issue sur un
projet client, **six maillons** doivent être correctement câblés. Si un seul
manque, la règle est invisible OU non exécutée OU non activée. C'est la
cause #1 des fausses pistes lors de l'ajout d'une règle.

```
       ┌────────────────────┐
       │ 1. Check.java      │  Logique métier (IacCheck + @Rule(key=GCI…))
       └─────────┬──────────┘
                 │ référencé par
                 ▼
       ┌────────────────────┐
       │ 2. CheckRegistrar  │  Liste statique ANNOTATED_RULE_CLASSES
       └─────────┬──────────┘
                 │ lu par
        ┌────────┴────────────────────────────┐
        ▼                                     ▼
┌──────────────────────┐              ┌──────────────────────┐
│ 3. RulesDefinition   │              │ 5. Infra<Lang>Sensor │
│  publie la règle     │              │  exécute le Check    │
│  dans le repo Sonar  │              │  au scan             │
└──────────┬───────────┘              └──────────┬───────────┘
           │ enregistré via                       │ enregistré via
           ▼                                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. InfraPlugin.define(Context)                              │
│   context.addExtension(InfraDockerRulesDefinition.class);   │
│   context.addExtension(InfraDockerQualityProfile.class);    │
│   context.addExtension(InfraDockerSensor.class);            │
└─────────────────────────────────────────────────────────────┘
                                ▲
                                │
       ┌────────────────────────┴────────────┐
       │ 4. QualityProfile JSON              │
       │  creedengo_way_profile_<lang>.json  │
       │  active la ruleKey dans "creedengo  │
       │  way"                               │
       └─────────────────────────────────────┘
```

| # | Maillon                       | Si manquant → conséquence                                       |
|---|-------------------------------|------------------------------------------------------------------|
| 1 | `Check.java` + `@Rule`        | Pas de logique. Pas de clé connue.                              |
| 2 | `CheckRegistrar`              | Règle invisible côté `RulesDefinition` *et* `Sensor`.            |
| 3 | `RulesDefinition`             | Règle non publiée dans `creedengo-infra-<lang>` → UI Sonar vide. |
| 4 | Profil JSON `creedengo way`   | Règle existe mais **désactivée** → 0 issue par défaut.           |
| 5 | `Infra<Lang>Sensor`           | Règle visible et activée, mais **jamais exécutée** au scan.      |
| 6 | `InfraPlugin#define`          | Aucune extension chargée par Sonar (cas extrême : plugin neuf).  |

> ✅ **Bonne nouvelle** : les maillons 3, 5 et 6 sont déjà **génériques**
> dans le projet (un par langage). Ajouter une règle nécessite donc seulement
> de **toucher au maillon 1, 2 et 4** (et de fournir les métadonnées). Les
> maillons 3/5/6 « se câblent tout seuls » grâce à l'introspection de la
> liste `ANNOTATED_RULE_CLASSES`.

## 6.2 Vue d'ensemble du flux de contribution

```
┌──────────────────┐      ┌────────────────────┐      ┌──────────────────────┐
│ 1. Spécifier     │  ──► │ 2. Implémenter     │  ──► │ 3. Tester            │
│   *.md à la      │      │   <Name>Check.java │      │   <Name>CheckTest    │
│   racine + JSON  │      │   @Rule(key=GCI…)  │      │   + fixtures         │
│   metadata       │      │                    │      │                      │
└──────────────────┘      └────────────────────┘      └──────────────────────┘
                                                                 │
                                                                 ▼
┌────────────────────────┐      ┌─────────────────────────┐      ┌────────────────┐
│ 4. Enregistrer dans    │ ──► │ 5. Activer dans le      │ ──►  │ 6. Packager &  │
│  Infra<Lang>           │      │  profil JSON            │      │  installer     │
│  CheckRegistrar        │      │  creedengo_way_*.json   │      │  le plugin     │
└────────────────────────┘      └─────────────────────────┘      └────────────────┘
```

Chaque langage IaC (Docker, Kubernetes, Terraform, CloudFormation, YAML)
expose la même chaîne, simplement avec son propre triplet
`RulesDefinition / CheckRegistrar / Sensor`.

## 6.3 Choisir la clé de règle

- Format : `GCI<NNNN>` (Green Code Initiative).
- Réserver une nouvelle clé inutilisée (consulter `RULES.md` et les
  `creedengo_way_profile_*.json` existants).
- Documenter une spec courte à la racine : `mon-nouvelle-regle.md` (s'inspirer
  des `*.md` existants — voir `doc/RULES-JUSTIFICATIONS.TEMPLATE.md`).

## 6.4 Étape 1 — Implémenter le `Check`

Créer la classe sous
`src/main/java/org/greencodeinitiative/creedengo/infra/checks/`.

### 6.4.1 Squelette générique

```java
package org.greencodeinitiative.creedengo.infra.checks;

import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;

@Rule(key = "GCI1099")            // ← clé de règle (cf. § 6.3)
public class MyEcoCheck implements IacCheck {

  private static final String MESSAGE =
      "Description courte de l'issue, à l'impératif (verbe d'action).";

  @Override
  public void initialize(@Nonnull InitContext init) {
    // S'abonner à un nœud d'AST sonar-iac :
    init.register(SomeTreeType.class, (ctx, node) -> {
      if (matchesBadPattern(node)) {
        ctx.reportIssue(node /* HasTextRange */, MESSAGE);
      }
    });
  }

  private boolean matchesBadPattern(SomeTreeType node) {
    // Logique métier
    return false;
  }
}
```

### 6.4.2 Trois points critiques à respecter

1. **`@Rule(key = "GCIxxxx")`** est obligatoire et doit être unique : c'est
   cette clé qui sera lue par `AbstractInfraIacSensor#ruleKeyOf(...)` pour
   décider si la règle est active dans le profil courant. Sans cette
   annotation, le sensor **ignore silencieusement** la classe.
2. **Implémenter `IacCheck`** (et non `org.sonar.check.Check` ou autre) :
   c'est le contrat attendu par le `SensorCheckContext` qui pilote la
   visite d'AST.
3. **Constructeur sans argument** : `AbstractInfraIacSensor` instancie le
   check via `checkClass.getDeclaredConstructor().newInstance()`. Tout
   constructeur paramétré fera planter le sensor en silence (log warn,
   règle désactivée pour ce fichier).

### 6.4.3 Choisir le bon type d'AST selon le langage

| Langage         | Imports utiles                                              | Nœuds AST fréquents                                |
|-----------------|-------------------------------------------------------------|----------------------------------------------------|
| Docker          | `org.sonar.iac.docker.tree.api.*`                           | `Body`, `DockerImage`, `RunInstruction`, `FromInstruction` |
| Kubernetes/YAML | `org.sonar.iac.common.yaml.tree.*`                          | `FileTree`, `MappingTree`, `TupleTree`, `ScalarTree` |
| Terraform (HCL) | `org.sonar.iac.terraform.api.tree.*`                        | `FileTree`, `BlockTree`, `AttributeTree`            |
| CloudFormation  | `org.sonar.iac.common.yaml.tree.*` (CFN est du YAML)        | comme Kubernetes                                   |
| YAML / CI       | idem Kubernetes                                             | + utilitaires `CiYamlCheckUtils`                    |

> 💡 Inspiration immédiate : ouvrir un check existant proche du langage cible
> (`MultistageCheck` pour Docker, `ReplicasGreaterThanNeededCheck` pour
> Kubernetes, `TfRightSizeInstanceTypesCheck` pour Terraform…).

### 6.4.4 Utilitaires partagés

- `DockerCheckUtils` — concaténation/inspection des `RunInstruction`.
- `KubernetesCheckUtils` — extraction `kind`, `metadata`, `spec`…
- `TerraformCheckUtils` — accès `resource "<type>" "<name>"`.
- `CiYamlCheckUtils` — détection des plateformes CI (GitHub Actions, GitLab…).

Réutiliser systématiquement pour rester homogène.

## 6.5 Étape 2 — Fournir les métadonnées de règle

Deux options selon que la règle est portée par le repo central
`creedengo-rules-specifications` ou par ce repo.

**Option A — Métadonnées dans `creedengo-rules-specifications` (cas standard)** :
- ouvrir une PR sur ce repo en ajoutant `GCI1099.html` + `GCI1099.json`
  dans le classifier `<lang>`,
- `Infra<Lang>RulesDefinition` les charge automatiquement via
  `RuleMetadataLoader` depuis `org/green-code-initiative/rules/<lang>`.

**Option B — Métadonnées locales** (jusqu'à la publication centrale) :
- placer `GCI1099.html` + `GCI1099.json` sous le `RESOURCE_BASE_PATH`
  utilisé par la `RulesDefinition` correspondante.

Champs JSON minimaux :
```json
{
  "title": "Use a Docker multi-stage build…",
  "type": "CODE_SMELL",
  "status": "ready",
  "remediation": { "func": "Constant\/Issue", "constantCost": "5min" },
  "tags": ["creedengo", "eco-design", "docker"],
  "defaultSeverity": "Minor"
}
```

> 🔎 **Comment c'est chargé concrètement ?**
> `InfraDockerRulesDefinition#define` construit un `RuleMetadataLoader`
> pointant sur `org/green-code-initiative/rules/docker` puis appelle
> `loader.addRulesByAnnotatedClass(repository, InfraDockerCheckRegistrar.checkClasses())`.
> Le loader prend la clé `@Rule(key=…)` de chaque classe, cherche
> `GCI1099.json` et `GCI1099.html` dans ce chemin, et en déduit
> les champs `title`, `severity`, `tags`, etc.

## 6.6 Étape 3 — Écrire les tests unitaires

### 6.6.1 Fixture

Créer `src/test/resources/checks/<NomDuCheck>/<fichier-IaC>` avec des
annotations `# Noncompliant` ou `// Noncompliant` sur les lignes attendues
(format hérité du `Verifier` de sonar-iac).

### 6.6.2 Test

```java
class MyEcoCheckTest {

  @Test
  void shouldReport() {
    Verifier.verify(
        Path.of("src/test/resources/checks/MyEcoCheck/Dockerfile"),
        new MyEcoCheck()
    );
  }
}
```

> Les helpers (`Verifier`, `CollectingContext`…) sont fournis dans
> `src/test/java/org/sonar/iac/commons/testing/`.

Exécuter :

```bash
./mvnw test -Dtest=MyEcoCheckTest
```

## 6.7 Étape 4 — Enregistrer la règle dans le `CheckRegistrar`

C'est **le maillon central** : `Infra<Lang>CheckRegistrar` est la **source
de vérité** lue par :
- la `RulesDefinition` → publie la règle dans le repository Sonar,
- le `Sensor` → exécute la règle au scan.

Ouvrir `Infra<Lang>CheckRegistrar.java` (`<Lang>` ∈ Docker, Kubernetes,
Terraform, CloudFormation, Yaml) et **ajouter la classe** dans la liste
`ANNOTATED_RULE_CLASSES` :

```java
public final class InfraDockerCheckRegistrar {

  public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
      org.greencodeinitiative.creedengo.infra.checks.MultistageCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.LightweightImagesCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.SetNonRootUserCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.NoBuildToolsInRuntimeCheck.class,
      // ➜ Ajouter ici :
      org.greencodeinitiative.creedengo.infra.checks.MyEcoCheck.class
  );

  public static List<Class<?>> checkClasses() {
    return ANNOTATED_RULE_CLASSES;
  }
}
```

### 6.7.1 Que se passe-t-il automatiquement ensuite ?

Une fois la classe ajoutée à la liste, **vous n'avez rien d'autre à coder** :

| Conséquence                                              | Mécanisme                                                                         |
|----------------------------------------------------------|------------------------------------------------------------------------------------|
| La règle apparaît dans le repository `creedengo-infra-docker` | `InfraDockerRulesDefinition#define` itère sur `checkClasses()` via `RuleMetadataLoader.addRulesByAnnotatedClass(...)`. |
| La règle est instanciée et exécutée par le sensor        | `AbstractInfraIacSensor#buildActiveChecks` itère sur les mêmes classes, lit `@Rule(key=…)`, filtre via `SensorContext.activeRules()`, instancie via réflexion. |
| Les issues atterrissent sur la bonne `RuleKey`           | `SensorCheckContext` reçoit le couple `(repositoryKey, ruleKey)` et appelle `sensorContext.newIssue().forRule(...)`. |

> 🚦 **Aucune modification** de `InfraDockerSensor`, `AbstractInfraIacSensor`
> ou `InfraPlugin` n'est nécessaire pour ajouter une règle « standard ».
> C'est le bénéfice principal de l'architecture : un seul point d'enregistrement.

## 6.8 Étape 5 — Activer la règle dans le profil « creedengo way »

C'est l'étape **la plus oubliée**. La règle peut être parfaitement publiée
côté serveur mais rester désactivée sur les projets clients par défaut.

Éditer le profil JSON correspondant
(`src/main/resources/org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_<lang>.json`) :

```json
{
  "name": "Creedengo",
  "ruleKeys": [
    "GCI1025",
    "GCI1026",
    "…",
    "GCI1099"   // ← nouvelle règle
  ]
}
```

### 6.8.1 Pourquoi cette étape est obligatoire

`Infra<Lang>QualityProfile#define` charge ce JSON via
`BuiltInQualityProfileJsonLoader.load(profile, REPOSITORY_KEY, PATH)`.
Le loader ne croit *que* ce fichier : il n'active pas par défaut toutes les
règles connues du repository. Donc une règle absente du JSON existera dans
SonarQube (rule explorer) mais sera **inactive** dans « creedengo way ».

Côté `Sensor`, l'étape de filtrage `context.activeRules().find(key)` renverra
`null` → la règle sera ignorée silencieusement au scan.

## 6.9 Étape 6 — Aucune modif côté Sensor et Plugin (cas standard)

L'`Infra<Lang>Sensor` est **générique** : il itère sur les classes renvoyées
par `Infra<Lang>CheckRegistrar.checkClasses()` puis filtre par règle active
(`SensorContext.activeRules()`). Aucune modification n'est nécessaire en
ajoutant une règle « standard ».

`InfraPlugin#define` enregistre les classes `Sensor`, `RulesDefinition`,
`QualityProfile` *à la classe*, pas à la règle. **Pas de modification**.

### 6.9.1 Exception — règle nécessitant un nouveau Sensor

Si la nouvelle règle nécessite un sensor cross-file (par exemple, lecture de
fichiers `values.yaml` séparés ou corrélation entre plusieurs manifests),
il faut alors :

1. créer un nouveau `Sensor` (à la manière de `HelmValuesEcoSensor` ou
   `K8sWorkloadCrossRefSensor`) qui implémente `org.sonar.api.batch.sensor.Sensor`,
2. l'enregistrer dans `InfraPlugin#define` via `context.addExtension(...)`,
3. dans son `describe(SensorDescriptor)`, appeler
   `descriptor.createIssuesForRuleRepository("creedengo-infra-<lang>")` pour
   pouvoir reporter sur la bonne `RuleKey`,
4. la règle elle-même n'est plus forcément un `IacCheck` traditionnel : elle
   peut vivre directement dans le sensor (logique inline), ou rester un
   `IacCheck` instancié manuellement et invoqué par le sensor.

Voir `HelmValuesEcoSensor.java` pour un patron de cross-file sensor.

## 6.10 Récapitulatif des fichiers à toucher

| Type d'ajout                | Fichiers à modifier / créer                                                                              |
|-----------------------------|----------------------------------------------------------------------------------------------------------|
| Règle « standard »          | 1. `checks/MyEcoCheck.java` (nouveau)<br>2. `Infra<Lang>CheckRegistrar.java` (liste)<br>3. `creedengo_way_profile_<lang>.json` (clé)<br>4. `src/test/...CheckTest.java` (nouveau)<br>5. Fixtures `src/test/resources/checks/MyEcoCheck/`<br>6. `mynewslug.md` à la racine (spec) |
| Métadonnées locales         | + `GCI1099.html` / `.json` sous le `RESOURCE_BASE_PATH`                                                  |
| Règle cross-file            | + nouveau `XxxSensor.java` + `InfraPlugin#define` (`addExtension`)                                       |

## 6.11 Vérifier l'intégration de bout en bout

```bash
# 1. Build + tests
./mvnw clean package

# 2. Déployer le JAR
cp target/creedengo-infra-plugin-*.jar \
   /opt/sonarqube/extensions/plugins/
docker compose restart sonar       # ou: make stop && make start

# 3. Vérifier dans l'UI SonarQube :
#    - Rules → repository "creedengo-infra-<lang>" → règle GCI1099 visible
#    - Quality Profiles → "creedengo way" (lang) → règle activée
# 4. Lancer un scan sur un projet IaC contenant le motif fautif
sonar-scanner -Dsonar.projectKey=demo -Dsonar.sources=.
#    → issue GCI1099 visible sur le projet "demo"
```

### 6.11.1 Diagnostic en cas de règle « invisible »

| Symptôme                                                  | Cause probable                                                                  | Vérification                                                        |
|-----------------------------------------------------------|---------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Règle absente de l'UI *Rules*                             | Classe non listée dans le `CheckRegistrar` OU métadonnées manquantes            | `grep MyEcoCheck.class src/main/java/.../Infra*CheckRegistrar.java` |
| Règle visible mais désactivée dans « creedengo way »      | Clé absente de `creedengo_way_profile_<lang>.json`                              | `grep GCI1099 src/main/resources/.../creedengo_way_profile_*.json`  |
| Règle active mais 0 issue                                 | `@Rule(key=…)` mal écrite, ou check ne s'abonne pas au bon type AST             | Logs Sonar `Could not instantiate creedengo-infra check …`          |
| Erreur `IllegalArgumentException` au scan                 | `reportIssue` avec un `TextRange` sortant du fichier                            | Logs Sonar (niveau DEBUG)                                            |
| JAR pas chargé du tout                                    | Mauvais répertoire `extensions/plugins/` OU conflit de version SonarQube        | `tail -f logs/sonar.log` au démarrage                                |

## 6.12 Checklist PR

- [ ] Classe `<Name>Check` créée avec `@Rule(key = "GCI…")`
- [ ] Test unitaire + fixtures fournis et passants
- [ ] Ajout dans `Infra<Lang>CheckRegistrar.ANNOTATED_RULE_CLASSES`
- [ ] Ajout de la clé dans `creedengo_way_profile_<lang>.json`
- [ ] Métadonnées HTML+JSON disponibles (locales ou via `creedengo-rules-specifications`)
- [ ] Fichier `*.md` de spécification à la racine
- [ ] `./mvnw verify` OK
- [ ] Validation manuelle dans l'UI SonarQube
- [ ] (Si cross-file) Sensor dédié déclaré dans `InfraPlugin#define`

