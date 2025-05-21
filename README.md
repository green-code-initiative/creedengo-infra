# Creedengo-Infra

_creedengo_ is a collective project aiming to reduce environmental footprint of software at the code level. The goal of the project is to provide a list of static code analyzers to highlight code structures that may have a negative ecological impact: energy and resources over-consumption, "fatware", shortening terminals' lifespan, etc.


[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![Build](https://github.com/green-code-initiative/creedengo-infra/actions/workflows/ci.yml/badge.svg)
[![Sonar Quality gate](https://img.shields.io/sonar/quality_gate/green-code-initiative_ecoCode-linter?server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/project/overview?id=green-code-initiative_ecoCode-linter)

## 🌿 SonarQube plugin

_Creedengo_Infra is an "eco-responsibility" static code analyzer for projects based on the Kubernetes ecosystem. It
can handle Yaml files. Its main purpose is to work with website source code, but it can also analyze back-end code.

This project proposes rules for the following technologies:

- Kubernetes


## 🛒 Distribution

[![sonar-plugin version](https://img.shields.io/github/v/release/green-code-initiative/creedengo-infra?label=SonarQube%20plugin)](https://github.com/green-code-initiative/creedengo-infra/releases/latest)
[![eslint-plugin version](https://img.shields.io/npm/v/@creedengo/eslint-plugin?label=ESLint%20plugin)](https://npmjs.com/package/@creedengo/eslint-plugin)

**The plugin is available from the official SonarQube marketplace! Check the
[version matrix here](https://docs.sonarsource.com/sonarqube/latest/instance-administration/plugin-version-matrix/).**

Ready to use binaries for SonarQube are also
available [from GitHub](https://github.com/green-code-initiative/creedengo-infra/releases). \
Make sure to place the binary inside `extensions/plugins/` folder of your SonarQube instance.

The standalone version of the ESLint plugin is available from [npmjs](https://npmjs.com/package/@creedengo/eslint-plugin).

## 🧩 Compatibility

| Plugins Version | SonarQube version | ESLint version |
| --------------- | ----------------- | -------------- |
| 0.1.0+            | 9.9.+ LTA to 25.3 | 7.x, 8.x, 9.x  |


## 🤝 Contribution

You have an idea or you want to help us improving this project? \
We are open to your suggestions and contributions! Open an issue or PR 🚀

Check out the [CONTRIBUTING.md](CONTRIBUTING.md) file
and follow the various guides to start contributing.

