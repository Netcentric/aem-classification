
package biz.netcentric.filevault.validator.aem.classification;

/*-
 * #%L
 * AEM Classification Validator
 * %%
 * Copyright (C) 2020 Netcentric - A Cognizant Digital Business
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.Validator;
import org.apache.jackrabbit.vault.validation.spi.ValidatorFactory;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.netcentric.filevault.validator.aem.classification.classpathurl.URLFactory;

@MetaInfServices
public class AemClassificationValidatorFactory implements ValidatorFactory {

    /** URL of the classifier map, could start with file: for filesystem based maps, or start with tccl: for thread-context classloader
     * resource based maps. Also all other known protocols are supported */
    private static final String OPTION_MAPS = "maps";
    /** optional list of comma-separated resource path patterns (should be absolute) */
    private static final String OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS = "whitelistedResourcePathPatterns";
    private static final String OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS_OLD = "whitelistedResourcePathsPatterns";

    private static final Object OPTION_SEVERITIES_PER_CLASSIFICATION = "severitiesPerClassification";

    private static final Logger LOGGER = LoggerFactory.getLogger(AemClassificationValidatorFactory.class);

    @Override
    public Validator createValidator(@NotNull ValidationContext context, @NotNull ValidatorSettings settings) {
        String mapUrls = settings.getOptions().get(OPTION_MAPS);
        // either load map from classpath, from filesystem or from generic url
        if (StringUtils.isBlank(mapUrls)) {
            throw new IllegalArgumentException("Mandatory option " + OPTION_MAPS + " missing!");
        }
        String optionWhitelistedResourcePaths = null;
        if (settings.getOptions().containsKey(OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS_OLD)) {
            LOGGER.warn("Deprecated option '{}' detected, please switch to the new key '{}' instead", OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS_OLD, OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS);
            optionWhitelistedResourcePaths = settings.getOptions().get(OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS_OLD);
        }
        if (settings.getOptions().containsKey(OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS)) {
            optionWhitelistedResourcePaths = settings.getOptions().get(OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS);
        }

        Collection<String> whitelistedResourcePaths = 
                Optional.ofNullable(optionWhitelistedResourcePaths).map(op -> Arrays.asList(op.split(","))).orElse(Collections.emptyList());

        try {
            whitelistedResourcePaths.stream().forEach(AemClassificationValidatorFactory::validateResourcePathPattern);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("At least one value given in option " + OPTION_WHITELISTED_RESOURCE_PATH_PATTERNS + " is invalid", e);
        }
        try {
            ContentClassificationMapper map = null;
            for (String mapUrl : mapUrls.split(",")) {
                try (InputStream input = URLFactory.createURL(mapUrl).openStream()) {
                    if (map == null) {
                        map = new ContentClassificationMapperImpl(input, mapUrl);
                    } else {
                        LOGGER.debug("Merge another map {}", mapUrl);
                        // merge another map
                        map.merge(new ContentClassificationMapperImpl(input, mapUrl));
                    }
                }
            }
            if (map == null) {
                throw new IllegalArgumentException("At least one valid map must be given!");
            }
            return new AemClassificationValidator(settings.getDefaultSeverity(), map, whitelistedResourcePaths,
                    getSeverityPerClassification(settings.getOptions().get(OPTION_SEVERITIES_PER_CLASSIFICATION)));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read from  " + mapUrls, e);
        }
    }

    @Override
    public boolean shouldValidateSubpackages() {
        return false;
    }

    @Override
    public @NotNull String getId() {
        return "netcentric-aem-classification";
    }

    @Override
    public int getServiceRanking() {
        return 0;
    }

    static Map<ContentClassification, ValidationMessageSeverity> getSeverityPerClassification(@Nullable String option) {
        final Map<ContentClassification, ValidationMessageSeverity> severitiesPerClassification;
        return Optional.ofNullable(option)
                .map(op -> Arrays.asList(op.split(",")))
                .map(AemClassificationValidatorFactory::parseSeverityClassification)
                .orElse(Collections.emptyMap());
    }

    private static Map<ContentClassification, ValidationMessageSeverity> parseSeverityClassification(List<String> severities) {
        Map<ContentClassification, ValidationMessageSeverity> result = severities.stream()
                .map(severity -> severity.split("="))
                .filter(arr -> arr.length == 2 && !StringUtils.isEmpty(arr[0]) && !StringUtils.isEmpty(arr[1]))
                .collect(Collectors.toMap(s -> ContentClassification.valueOf(s[0].trim()),
                        s -> ValidationMessageSeverity.valueOf(s[1].trim())));

        if (result.size() != severities.size()) {
            throw new IllegalArgumentException(
                    "severitiesPerClassification must be given as comma-separated 'classification=severity` pairs, but is '" + severities
                            + "'");
        }

        return result;
    }

    static void validateResourcePathPattern(String resourcePathPattern) throws IllegalArgumentException {
        Matcher matcher = Pattern.compile(resourcePathPattern).matcher("/");
        matcher.matches();
        if (!matcher.hitEnd()) {
            // pattern does not allow a starting "/"
            throw new IllegalArgumentException("The given resource path pattern " + resourcePathPattern + " will never match as it does not allow a path to start with '/' or matches all paths!");
        }
    }
}
