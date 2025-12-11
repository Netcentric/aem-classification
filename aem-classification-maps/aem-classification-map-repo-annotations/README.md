# Overview
This classification map contains content classifications derived from the `jcr:mixinTypes` (documented at [Content Classifications][1]) and the properties `cq:deprecated` `cq:deprecatedReason`.

It has been generated with the [Maven Plugin][2].

# Details
The classification map is available [at Maven Central][3] with the following coordinates:

Group ID | Artifact ID
--- | --- 
`biz.netcentric.filevault.validator.maps` | `aem-classification-map-repo-annotations`


The version depends on whether you use Classic or Cloud
- AEM Classic (on premise/AMS), up to version `6.5.x.y`, i.e. `6.5.13.0` for AEM 6.5 SP13. Always consists out of 4 parts. The third party indicated the service pack.
- AEM 6.5 LTS with technical version `6.6.x.y`. Always consists out of 4 parts. The third party indicated the service pack.
- AEM as a Cloud Service with `<YYYY>.<MM>.<BUILD>`, i.e. `2025.11.23482`. Always consists out of 3 parts.

Please check [at Maven Central][3] which version is closest to the AEM version you use and pick that one for validation.

The following files are provided in that artifact:

URL| Description
--- | ---
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-repo-annotations.map` | Contains all classifications and in addition also deprecations from the repository of AEM.

## Included Add-Ons

Since `2025.11.23482` and `6.6.1.0` there is also annotations included from the following add-ons in the most recent version (at the time of creation).

1. [Forms](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/forms/setup-configure-migrate/setup-local-development-environment#add-forms-archive)
2. [CIF](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/content-and-commerce/cif-storefront/storefront/developing/develop#accessing-add-on)

# Known incorrect classifications

All known incorrect classifications in the repo are listed in the table below. Please report others via [Issues](https://github.com/Netcentric/aem-classification/issues).

Path | Wrong Classification | Usage Examples | Wrong in version |  Reported (and tracked in)
--- | --- | --- | --- | ---
`/libs/cq/workflow/components/pages/model` | `granite:InternalArea` | Workflow Models | 6.5.x, 6.6.x, All AEMaaCS versions | https://daycare.day.com/content/home/netcentric/netcentric_de/aemasacloudservice/208727.html (CQ-4291242) 
`/libs/cq/dtm-reactor/components/conf/page` | `granite:InternalArea` (via parent) | Launch Cloud Configurations use that artificial resource type | 6.5.x, 6.6.x, All AEMaaCS versions | -

[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: ../../aem-classification-maven-plugin/README.md
[3]: https://central.sonatype.com/artifact/biz.netcentric.filevault.validator.maps/aem-classification-map-repo-annotations/versions
