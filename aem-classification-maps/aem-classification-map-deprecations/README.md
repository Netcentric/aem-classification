# Overview
This validator map contains deprecated resource types which are not yet [annotated in the repository][1] but still mentioned in some release notes like

1. <https://docs.adobe.com/content/help/en/experience-manager-65/release-notes/deprecated-removed-features.html>
1. <https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/changelogs.html>

# Details
The classification map is available at Maven Central with the following coordinates:

Group ID | Artifact ID | Version 
--- | --- | ---
`biz.netcentric.filevault.validator.maps` | `aem-classification-map-deprecations` | `1.0.0-SNAPHOT`

The following files are provided in that artifact:

URL | Description 
--- | ---
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/coral2deprecations.map` | Contains all deprecated Granite UI components still based on the [deprecated Coral UI 2](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/legacy/coral2/migration.html)
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/graniteuideprecations.map` | Contains all deprecated Granite UI components according to <https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/changelogs.html>


[1]: ../aem-classification-map-repo-annotations/README.md
