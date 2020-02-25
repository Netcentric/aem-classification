# Overview
Validates scripts/components for invalid usage according to [AEMs Content Classification][1]. It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

# Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][2].

Option | Mandatory | Description
--- | --- | ---
maps | yes | a comma-separated list of URLs specifying the source for a classification map. Each URL might use the protocols `file:`, for file-based classification maps, `http(s):` for classification maps in the internet or `tccl:` for classification maps being provided via the ThreadContextClassloader. The latter is especially useful with Maven as the TCCL during the execution of a goal of a Maven Plugin is the [Maven Plugin Classpath][4].
whiteListedResourceTypes | no | a comma-separated list of resource types which should not be reported no matter if they violate content classifications or not
severitiesPerClassification | no | the severity per classification (this will overwrite the default severity which is by default used for all classifications. The format is `<classification>=<severity>{,<classification>=<severity>}`, where `classification` is one of `INTERNAL`, `INTERNAL_DEPRECATED`, `FINAL` or `ABSTRACT` and `severity` is one of `DEBUG`, `INFO`, `WARN` or `ERROR`.

All validation messages are emitted with the [`defaultSeverity`][2]

## Classification Maps

The validator requires at least one validation map file in the format mentioned below (as it operates offline, i.e. without requiring a running AEM instance).
You find some predefined maps in [aem-classification-maps][9].

### Classification Map File Format

The file is a CSV serialization of the map where each line represents one item in the map and has the format

```
<path>,<classification>(,<remark>)
```

where `classification` is one of `INTERNAL`, `INTERNAL_DEPRECATED` (same restrictions as `INTERNAL` but due to being marked as deprecated), `FINAL`, `ABSTRACT`, `PUBLIC` (in order from most restricted to least restricted). The CSV format is based on [RFC-4180][7]. In addition a comment starting with `#` on the first line is supposed to contain a label for the map (like the underlying AEM version). `path` is supposed to be an absolute repository path of a specific component.

# Usage with Maven
You can use this validator with the [FileVault Package Maven Plugin][3] in version 1.1.0 or higher like this

```
<plugin>
  <groupId>org.apache.jackrabbit</groupId>
  <artifactId>filevault-package-maven-plugin</artifactId>
  <version>1.1.0</version>
  <configuration>
    <validatorsSettings>
      <netcentric-aem-classification>
        <options>
          <maps>tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-repo-annotations.map</maps><!-- references the classification.map from the ThreadContextClassLoader, might contain multiple maps (comma-separated) -->
        </options>
      </netcentric-aem-classification>
    </validatorsSettings>
  </configuration>
  <dependencies>
    <!-- the dependency for the validator plugin itself -->
    <dependency>
      <groupId>biz.netcentric.filevault.validator</groupId>
      <artifactId>aem-classification-validator</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
    <!-- the dependency containing the classification map -->
    <dependency>
      <groupId>biz.netcentric.filevault.validator.maps</groupId>
      <artifactId>aem-classification-map-repo-annotations</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
  </dependencies>
</plugin>
```

# Why?
Why is the validation and enforcement during build time crucial as Adobe already provides some run-time [Health Check][1] as well as the (run time) [Pattern Detector][6]? 

There are several reasons:

1. You should detect violations as early as possible, preferably already in your CI pipeline. The later you detect those the more effort it is to fix.
2. If you don't care about content classifications
    3. there is a high chance that you cannot easily upgrade to a newer AEM version (AMS or on-premise)
    4. it might break with every new [AEM as a Cloud Service][5] release

[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: https://jackrabbit.apache.org/filevault/validation.html
[3]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[4]: https://maven.apache.org/guides/mini/guide-maven-classloading.html
[5]: https://docs.adobe.com/content/help/en/experience-manager-cloud-service/landing/home.html
[6]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/pattern-detector.html
[7]: https://tools.ietf.org/html/rfc4180
[8]: https://github.com/Netcentric/aem-classification/aem-classification-maven-plugin
[9]: ../aem-classification-maps