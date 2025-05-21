## Hello! We are pleased to see you here 👋

Please read
common [CONTRIBUTING.md](https://github.com/green-code-initiative/creedengo-common/blob/main/doc/CONTRIBUTING.md)
in `creedengo-common` repository first.\
Also check the [starter pack](https://github.com/green-code-initiative/creedengo-common/blob/main/doc/starter-pack.md) to
have the basic information before starting.

## Structure

The Infra plugin is divided into 3 modules:

- `sonar-plugin` contains the SonarQube plugin implementation
- `test-project` contains a project to test the rules on SonarQube

## Requirements

### For the SonarQube plugin

- Java JDK 11+
- Maven 3.8 or later

SonarQube provides a documentation to
learn [plugin basics](https://docs.sonarqube.org/latest/extension-guide/developing-a-plugin/plugin-basics/).

## Installation

1. Clone the Git repository
2. Run `yarn install` inside **eslint-plugin** directory
3. Synchronize dependencies using Maven inside **sonar-plugin** directory
4. You are good to go! 🚀

## Create a rule

### Before starting

Before even starting to implement the rule, it must have been discussed, mesured and documented in the Creedengo
referential. Please follow
the [dedicated documentation](https://github.com/green-code-initiative/creedengo-rules-specifications/blob/main/README.md#creedengo-rules-specification-repository)
to find out how to proceed.

### In the SonarQube plugin

Now that the rule has been implemented in the Creedengo referential and its implementation written in the ESLint plugin,
all that remains is to reference it in the SonarQube plugin. Here are the simple steps:

1. Create a Java class in `src/main/java/fr/greencodeinitiative/creedengo/javascript/checks/` with
   the declaration of the SonarQube key and the ESLint key (check other classes to have an example)
2. Reference created in method `getAllChecks()` of Java class `CheckList` (please use alphabetical order)
3. Add the SonarQube key of the rule in the appropriate profile Json file.

### Test the rule

> ℹ You don't need to add a test in the Sonar plugin, the class will be tested automatically.

### Add an integration test

It is also necessary to add an example of non-compliant code to the test project in the
repository. This allows to check that the plgin works correctly with a real
SonarQube instance. A README is present in `test-project` directory to help running a local analysis.

### Generate rule documentation


## What else?

You should be ready to open a PR on GitHub with all your code!

Check one last time that you have respected the following points:

- [ ] My rule implementation covers the use case without false-positive
- [ ] My code is explicit or well documented
- [ ] I have provided unit tests
- [ ] I have added a non-compliant example code inside the test project
- [ ] The coverage of my code is greather than 80%
- [ ] I have modified the CHANGELOG with my change

This is the end of this guide, thank you for reading this far and contributing to the project 🙏.
