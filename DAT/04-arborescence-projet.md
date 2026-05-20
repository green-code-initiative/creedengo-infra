# 4. Arborescence du projet

## 4.1 Vue d'ensemble

```
creedengo-infra/
├── pom.xml                      # Build Maven (packaging sonar-plugin)
├── mvnw / mvnw.cmd              # Wrapper Maven
├── Makefile                     # Raccourcis build / docker
├── Dockerfile                   # Image SonarQube + plugin pré-installé
├── docker-compose.yml           # Stack locale (SonarQube + Postgres)
├── sonar-project.properties     # Analyse SonarCloud du repo lui-même
├── tool_*.sh                    # Scripts utilitaires (build, start, stop, release)
├── *.md                         # Spécifications « source » des règles (1 fichier par règle)
├── doc/                         # Documentation interne
│   ├── IMPORT-NOTES.md
│   └── RULES-JUSTIFICATIONS.TEMPLATE.md
├── DAT/                         # ← Ce dossier (Dossier d'Architecture Technique)
├── tools/
│   └── eco-iac-scan.sh          # Script d'aide au scan IaC
└── src/
    ├── main/
    │   ├── java/org/greencodeinitiative/creedengo/infra/
    │   │   ├── InfraPlugin.java                       # Entry-point Sonar
    │   │   ├── AbstractInfraIacSensor.java            # Sensor abstrait
    │   │   ├── Infra<Lang>RulesDefinition.java        # × 5 langages
    │   │   ├── Infra<Lang>QualityProfile.java         # × 5 langages
    │   │   ├── Infra<Lang>CheckRegistrar.java         # × 5 langages
    │   │   ├── Infra<Lang>Sensor.java                 # × 5 langages
    │   │   ├── DockerignoreSensor.java                # Sensor cross-file
    │   │   ├── K8sWorkloadCrossRefSensor.java         # Sensor cross-file
    │   │   ├── HelmValuesEcoSensor.java               # Sensor cross-file
    │   │   └── checks/                                # Implémentation des règles
    │   │       ├── MultistageCheck.java               # @Rule(key="GCI1025")
    │   │       ├── LightweightImagesCheck.java
    │   │       ├── …                                  # 1 classe = 1 règle
    │   │       └── *CheckUtils.java                   # Utilitaires partagés
    │   └── resources/
    │       └── org/greencodeinitiative/creedengo/profiles/
    │           ├── creedengo_way_profile_docker.json
    │           ├── creedengo_way_profile_kubernetes.json
    │           ├── creedengo_way_profile_terraform.json
    │           ├── creedengo_way_profile_cloudformation.json
    │           └── creedengo_way_profile_yaml.json
    └── test/
        ├── java/org/greencodeinitiative/creedengo/infra/checks/
        │   └── *CheckTest.java                        # 1 test par check
        └── resources/
            └── checks/<NomDuCheck>/                   # Fixtures IaC d'entrée
```

## 4.2 Conventions de nommage

| Élément            | Convention                                              | Exemple                      |
|--------------------|---------------------------------------------------------|------------------------------|
| Classe de check    | `<Suffixe>Check.java` dans `checks/`                    | `MultistageCheck`            |
| Clé de règle       | `GCI<NNNN>` (Green Code Initiative)                     | `GCI1025`                    |
| Repository Sonar   | `creedengo-infra-<lang>`                                | `creedengo-infra-docker`     |
| Profil qualité     | `creedengo way` (un par langage)                        | n/a                          |
| Test unitaire      | `<NomDuCheck>Test.java`                                 | `MultistageCheckTest`        |
| Fixture de test    | `src/test/resources/checks/<NomDuCheck>/<fichier>`      | `…/MultistageCheck/Dockerfile`|
| Spec de règle      | `<slug>.md` à la racine du repo                         | `multistage.md`              |

## 4.3 Source de vérité

Le **registrar** (`Infra<Lang>CheckRegistrar`) est la **liste autoritaire** des
règles actives :

- s'il y est listé → la règle est connue de la `RulesDefinition` *et* exécutée
  par le `Sensor`,
- s'il n'y est pas → la règle est invisible côté plugin (même si la classe
  existe).

⚠️ Il faut donc **maintenir cohérent** : registrar ⇄ profil JSON ⇄ classifier
de métadonnées (`creedengo-rules-specifications`).

