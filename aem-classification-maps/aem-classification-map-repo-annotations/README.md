# Overview
This validator map contains content classifications derived from the `jcr:mixinTypes` (documented at [Content Classifications][1]) and the properties `cq:deprecated` `cq:deprecatedReason`.

It has been generated with the [Maven Plugin][2].

# Details
The classification map is available [at Maven Central][3] with the following coordinates:

Group ID | Artifact ID
--- | --- 
`biz.netcentric.filevault.validator.maps` | `aem-classification-map-repo-annotations`


The version is consisting out of 4 parts.
The first 3 parts specify the version of the underlying AEM version

- AEM (on premise/AMS), up to version 6.5.x, i.e. `6.5.3` for AEM 6.5 SP3
- AEM as a Cloud Service with `<YYYY>.<MM>.<BUILD>`, i.e. `2020.01.0`.

Please check [at Maven Central][3] which version is closest to the AEM version you use and pick that one for validation.

The following files are provided in that artifact:

URL| Description
--- | ---
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-repo-annotations.map` | Contains all classifications and in addition also deprecations from the repository of AEM.

[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: ../../aem-classification-maven-plugin/README.md
[3]: https://search.maven.org/search?q=g:biz.netcentric.filevault.validator.maps%20AND%20a:aem-classification-map-repo-annotations
