# Overview
This classification map contains deprecated resource types which are not (yet) [annotated in the repository][1] but still mentioned in some release notes like

1. <https://docs.adobe.com/content/help/en/experience-manager-65/release-notes/deprecated-removed-features.html>
1. <https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/changelogs.html>

Feel free to provide additional deprecations via PRs for arbitrary AEM versions.

# Details
The classification map is available at [Maven Central][2] with the following coordinates:

Group ID | Artifact ID
--- | ---
`biz.netcentric.filevault.validator.maps` | `aem-classification-map-deprecations`

Please check which versions are [available][2] and combine multiple versions to also detect deprecations from previous AEM versions. The maps are versioned with the same schema as the underlying AEM version from which the deprecation was derived.

This map only contains deprecations which are not yet reflected in the repository via `cq:deprecated` properties. Those can be found instead in the map maintained at [aem-classification-map-repo-annotations][1].

The following files are provided in that artifact:

URL | Description 
--- | ---
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/coral2deprecations.map` | Contains all deprecated Granite UI components still based on the [deprecated Coral UI 2](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/legacy/coral2/migration.html)
`tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/graniteuideprecations.map` | Contains all deprecated Granite UI components according to <https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/changelogs.html>


[1]: ../aem-classification-map-repo-annotations/README.md
[2]: https://search.maven.org/search?q=g:biz.netcentric.filevault.validator.maps%20AND%20a:aem-classification-map-deprecations
