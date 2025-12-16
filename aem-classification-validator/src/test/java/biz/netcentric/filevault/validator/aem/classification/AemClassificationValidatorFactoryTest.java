package biz.netcentric.filevault.validator.aem.classification;

/*-
 * #%L
 * AEM Classification Validator
 * %%
 * Copyright (C) 2022 Cognizant Netcentric
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import biz.netcentric.filevault.validator.aem.classification.map.CompositeContentClassificationMap;
import biz.netcentric.filevault.validator.aem.classification.map.ContentClassificationMapImpl;
import biz.netcentric.filevault.validator.aem.classification.map.MutableContentClassificationMapImpl;

class AemClassificationValidatorFactoryTest {

    @Test
    void testGetSeverityPerClassification() {
        Map<ContentClassification, ValidationMessageSeverity> severityPerClassification = new HashMap<>();
        severityPerClassification.put(ContentClassification.FINAL, ValidationMessageSeverity.WARN);
        severityPerClassification.put(ContentClassification.INTERNAL, ValidationMessageSeverity.ERROR);
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification("INTERNAL=ERROR,FINAL=WARN"));
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification("INTERNAL = ERROR , FINAL = WARN"));
    }

    @Test
    void testGetSeverityPerClassificationWithNullParameter() {
        Map<ContentClassification, ValidationMessageSeverity> severityPerClassification = new HashMap<>();
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification(null));
    }

    @Test
    void testGetSeverityPerClassificationWithEmptyParameter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification(""); });
    }

    @Test
    void testGetSeverityPerClassificationWithInvalidParameter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification("FINAL:WARN"); });
    }

    @Test
    void testGetSeverityPerClassificationWithInvalidParameter2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification("INVALID:WARN"); });
    }

    @Test
    void testGetSeverityPerClassificationWithInvalidParameter3() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification(" =ERROR,FINAL=WARN"); });
    }

    @Test
    void testValidateResourcePathPatternWithValidPatterns() {
        AemClassificationValidatorFactory.validateResourcePathPattern("/libs");
        AemClassificationValidatorFactory.validateResourcePathPattern("/libs/some/path");
        AemClassificationValidatorFactory.validateResourcePathPattern(".*/test");
    }

    @Test
    void testValidateResourcePathPatternWithInvalidPatterns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("relative/path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("/"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("[^/].*"));
    }

    @Test
    void testCreateValidator() {
        AemClassificationValidatorFactory factory = new AemClassificationValidatorFactory();
        Map<String, String> options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map");
        // deprecated option for whitelisting
        options.put("whitelistedResourcePathsPatterns", "/resourceType1/.*,/resourceType2,\n/resourceType3");
        options.put("ignoreViolationsInPropertiesMatchingPathPatterns", "/apps/mysite/components/reference,\n/apps/mysite/components/old-components/.*");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG,\nINTERNAL_DEPRECATED=INFO");
        ValidatorSettings settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        MutableContentClassificationMap map = new MutableContentClassificationMapImpl("Simple");
        map.put("/test", ContentClassification.INTERNAL_DEPRECATED, "Deprecated");
        Collection<String> whiteListedResourceTypes = new LinkedList<>();
        whiteListedResourceTypes.add("/resourceType1/.*");
        whiteListedResourceTypes.add("/resourceType2");
        whiteListedResourceTypes.add("/resourceType3");
        Collection<String> ignoreViolationsInPropertiesMatchingPathPatterns = new LinkedList<>();
        ignoreViolationsInPropertiesMatchingPathPatterns.add("/apps/mysite/components/reference");
        ignoreViolationsInPropertiesMatchingPathPatterns.add("/apps/mysite/components/old-components/.*");
        Map<ContentClassification, ValidationMessageSeverity> severitiesPerClassification = new HashMap<>();
        severitiesPerClassification.put(ContentClassification.INTERNAL, ValidationMessageSeverity.DEBUG);
        severitiesPerClassification.put(ContentClassification.INTERNAL_DEPRECATED, ValidationMessageSeverity.INFO);
        AemClassificationValidator expectedValidator = new AemClassificationValidator(ValidationMessageSeverity.WARN, new CompositeContentClassificationMap(map), whiteListedResourceTypes, ignoreViolationsInPropertiesMatchingPathPatterns, severitiesPerClassification);
        Assertions.assertEquals(expectedValidator, factory.createValidator(mock(ValidationContext.class), settings));

        options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map");
        // new option for whitelisting
        options.put("whitelistedResourcePathPatterns", "/resourceType1/.*,/resourceType2,\n/resourceType3");
        options.put("ignoreViolationsInPropertiesMatchingPathPatterns", "/apps/mysite/components/reference,\n/apps/mysite/components/old-components/.*");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG,\nINTERNAL_DEPRECATED=INFO");
        settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        Assertions.assertEquals(expectedValidator, factory.createValidator(mock(ValidationContext.class), settings));

        // test with multiple validation maps including whitespaces in the maps string
        options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map,tccl:empty-map-1.map,\n  \t tccl:empty-map-2.map");
        options.put("whitelistedResourcePathPatterns", "/resourceType1/.*,/resourceType2,\n/resourceType3");
        options.put("ignoreViolationsInPropertiesMatchingPathPatterns", "/apps/mysite/components/reference,\n/apps/mysite/components/old-components/.*");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG,\nINTERNAL_DEPRECATED=INFO");
        settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        ContentClassificationMap emptyMap = new ContentClassificationMapImpl("");
        expectedValidator = new AemClassificationValidator(ValidationMessageSeverity.WARN, new CompositeContentClassificationMap(map, emptyMap, emptyMap), whiteListedResourceTypes, ignoreViolationsInPropertiesMatchingPathPatterns, severitiesPerClassification);
        Assertions.assertEquals(expectedValidator, factory.createValidator(mock(ValidationContext.class), settings));
    }

    private static final class ValidatorSettingsImpl implements ValidatorSettings {

        private final boolean isDisabled;
        private final ValidationMessageSeverity defaultSeverity;
        private Map<String, String> options;

        ValidatorSettingsImpl(boolean isDisabled, ValidationMessageSeverity defaultSeverity, Map<String, String> options) {
            super();
            this.isDisabled = isDisabled;
            this.defaultSeverity = defaultSeverity;
            this.options = options;
        }

        @Override
        @NotNull
        public ValidationMessageSeverity getDefaultSeverity() {
            return defaultSeverity;
        }

        @Override
        @NotNull
        public Map<String, String> getOptions() {
            return options;
        }

        @Override
        public
        boolean isDisabled() {
            return isDisabled;
        }

    }
}
