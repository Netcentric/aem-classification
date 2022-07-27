[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-classification-validator)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-classification-validator)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Build Status](https://github.com/Netcentric/aem-classification/actions/workflows/maven.yml/badge.svg?branch=master)](https://github.com/Netcentric/aem-classification/actions/workflows/maven.yml)
[![SonarCloud Status](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-classification&metric=alert_status)](https://sonarcloud.io/dashboard?id=Netcentric_aem-classification)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-classification&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Netcentric_aem-classification)

# Overview

Validates scripts/components for invalid usage according to [AEMs Content Classification][1]. It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

# Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][2].

Option | Mandatory | Description
--- | --- | ---
maps | yes | a comma-separated list of URLs specifying the source for a classification map. Each URL might use the protocols `file:`, for file-based classification maps, `http(s):` for classification maps in the internet or `tccl:` for classification maps being provided via the ThreadContextClassloader. The latter is especially useful with Maven as the TCCL during the execution of a goal of a Maven Plugin is the [Maven Plugin Classpath][4].
whitelistedResourcePathPatterns | no | a comma-separated list of regular expressions matching an absolute resource path which should not be reported (no matter if its usage violates content classifications or not). The path is referring to the referenced/inherited/overlaid resource path (not the path containing the reference/supertype/overlay). 
severitiesPerClassification | no | the severity per classification (this will overwrite the default severity which otherwise used for all classifications). The format is `<classification>=<severity>{,<classification>=<severity>}`, where `classification` is one of `INTERNAL`, `INTERNAL_DEPRECATED_ANNOTATION`, `INTERNAL_DEPRECATED`, `FINAL` or `ABSTRACT` and `severity` is one of `DEBUG`, `INFO`, `WARN` or `ERROR`.

All validation messages are emitted with the [`defaultSeverity`][2]

## Classification Maps

The validator requires at least one validation map file in the format mentioned below (as it operates offline, i.e. without requiring a running AEM instance).
You find some predefined maps in [aem-classification-maps][9].

### Classification Map File Format

The file is a CSV serialization of the map where each line represents one item in the map and has the format

```
<path>,<classification>(,<remark>)
```

where `classification` is one of 

1. `INTERNAL`
2. `INTERNAL_DEPRECATED_ANNOTATION`, same restrictions as `INTERNAL` but due to being marked as deprecated via some annotation e.g. `cq:deprecated` property
3. `INTERNAL_DEPRECATED`, same restrictions as `INTERNAL` but due to being marked as deprecated in some external sources like release notes
4. `FINAL`
5. `ABSTRACT`
6. `PUBLIC` 

(in order from most restricted to least restricted). 
The explanation for those can be found in the [Adobe documentation][1].
The CSV format is based on [RFC 4180][7]. In addition a comment starting with `#` on the first line is supposed to contain a label for the map (like the underlying AEM version). `path` is supposed to be an absolute path of a specific resource.

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
          <!-- references the classification.map from the ThreadContextClassLoader, might contain multiple maps (comma-separated) -->
          <maps>tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/coral2deprecations.map,tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-deprecations/graniteuideprecations.map,tccl:biz/netcentric/filevault/validator/maps/aem-classification-map-repo-annotations.map</maps>
        </options>
      </netcentric-aem-classification>
    </validatorsSettings>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>biz.netcentric.filevault.validator</groupId>
      <artifactId>aem-classification-validator</artifactId>
      <version>1.0.0</version>
    </dependency>
    <!-- the dependency containing the actual classification map -->
    <dependency>
      <groupId>biz.netcentric.filevault.validator.maps</groupId>
      <artifactId>aem-classification-map-repo-annotations</artifactId>
      <version>6.5.3.0</version>
    </dependency>
    <dependency>
      <groupId>biz.netcentric.filevault.validator.maps</groupId>
      <artifactId>aem-classification-map-deprecations</artifactId>
      <version>6.5.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```

# Why?

Why is the validation and enforcement during build time crucial as Adobe already provides some run-time [Health Check][1] as well as the (run time) [Pattern Detector][6]? 

There are several reasons:

1. You should detect violations as early as possible, preferably already in your CI pipeline. The later you detect those the more effort it is to fix.
2. If you don't care about content classifications
   1. there is a high chance that you cannot easily upgrade to a newer AEM version (AMS or on-premise)
   2. it might break with every new [AEM as a Cloud Service][5] release

[1]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/sustainable-upgrades.html#content-classifications
[2]: https://jackrabbit.apache.org/filevault/validation.html
[3]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[4]: https://maven.apache.org/guides/mini/guide-maven-classloading.html
[5]: https://docs.adobe.com/content/help/en/experience-manager-cloud-service/landing/home.html
[6]: https://docs.adobe.com/content/help/en/experience-manager-65/deploying/upgrading/pattern-detector.html
[7]: https://tools.ietf.org/html/rfc4180
[8]: https://github.com/Netcentric/aem-classification/aem-classification-maven-plugin
[9]: ../aem-classification-maps	
