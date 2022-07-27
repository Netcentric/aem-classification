[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-classification-validator)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-classification-validator)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Build Status](https://github.com/Netcentric/aem-classification/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/Netcentric/aem-classification/actions/workflows/maven.yml)
[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-classification&metric=alert_status)](https://sonarcloud.io/dashboard?id=Netcentric_aem-classification)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-classification&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Netcentric_aem-classification)


# Overview
This repository comprises of several modules around [AEM's Content Classification][1]. 

# Modules

1. [aem-classification-validator][2]: A FileVault validator for AEMs content classification
1. [aem-classification-maps][3]: Maps containing information about resource type classification. To be used with the validator
1. [aem-classification-maven-plugin][4]: A Maven plugin to generate validation maps out of repository annotations (i.e. mixin properties). This is only necessary to generate your own maps (in case the provided ones are not sufficient)
1. [aem-classification-search-index-package][5]: Oak index definitions necessary for the Maven plugin to work. This is only necessary to generate your own maps (in case the provided ones are not sufficient)


[1]: https://experienceleague.adobe.com/docs/experience-manager-65/deploying/upgrading/sustainable-upgrades.html?lang=en#content-classifications
[2]: ./aem-classification-validator
[3]: ./aem-classification-maps
[4]: ./aem-classification-maven-plugin
[5]: ./aem-classification-search-index-package
