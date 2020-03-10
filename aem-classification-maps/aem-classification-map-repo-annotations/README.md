# Overview
This validator map contains content classifications derived from the `jcr:mixinTypes` (documented at [Content Classifications][1]) and the properties `cq:deprecated` `cq:deprecatedReason`.

It has been generated with the [Maven Plugin][2].

# Details
The classification map is available at Maven Central with the following coordinates:

Group ID | Artifact ID | Version 
--- | --- | ---
`biz.netcentric.filevault.validator.maps` | `aem-classification-map-repo-annotations` | `6.5.3.0-SNAPHOT`

The version is consisting out of 4 sections. 
The first 3 sections give the 
- AEM on Prem versioning scheme, i.e. 6.5.3 for AEM 6.5 SP3
- AEM as a CloudService versioning scheme, i.e. 2020.01.0

The following files are provided in that artifact:

URL| Description
--- | ---
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-repo-annotations.map` | Contains all classifications and in addition also deprecations from the repository of AEM 6.5.3.

[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: ../../aem-classification-maven-plugin/README.md