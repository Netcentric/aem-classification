![](https://github.com/Netcentric/aem-classification/workflows/Java%20CI/badge.svg)[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)

# Overview
This repository comprises of several modules around [AEMs Content Classification][1]. 

# Modules

1. [aem-classification-validator][2]: A FileVault validator for AEMs content classification
1. [aem-classification-maps][3]: Maps containing information about resource type classification. To be used with the validator
1. [aem-classification-maven-plugin][4]: A Maven plugin to generate validation maps out of repository annotations (i.e. mixin properties)
1. [aem-classification-search-index-package][5]: Oak index definitions necessary for the Maven plugin to work.


[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: ./aem-classification-validator
[3]: ./aem-classification-maps
[4]: ./aem-classification-maven-plugin
[5]: ./aem-classification-search-index-package
