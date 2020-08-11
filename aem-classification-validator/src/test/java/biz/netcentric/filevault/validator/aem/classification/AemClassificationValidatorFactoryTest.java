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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AemClassificationValidatorFactoryTest {

    @Test
    public void testGetSeverityPerClassification() {
        Map<ContentClassification, ValidationMessageSeverity> severityPerClassification = new HashMap<>();
        severityPerClassification.put(ContentClassification.FINAL, ValidationMessageSeverity.WARN);
        severityPerClassification.put(ContentClassification.INTERNAL, ValidationMessageSeverity.ERROR);
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification("INTERNAL=ERROR,FINAL=WARN"));
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification("INTERNAL = ERROR , FINAL = WARN"));
    }

    @Test
    public void testGetSeverityPerClassificationWithNullParameter() {
        Map<ContentClassification, ValidationMessageSeverity> severityPerClassification = new HashMap<>();
        assertEquals(severityPerClassification, AemClassificationValidatorFactory.getSeverityPerClassification(null));
    }

    @Test
    public void testGetSeverityPerClassificationWithEmptyParameter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification(""); });
    }

    @Test
    public void testGetSeverityPerClassificationWithInvalidParameter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification("FINAL:WARN"); });
    }

    @Test
    public void testGetSeverityPerClassificationWithInvalidParameter2() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification("INVALID:WARN"); });
    }

    @Test
    public void testGetSeverityPerClassificationWithInvalidParameter3() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> { AemClassificationValidatorFactory.getSeverityPerClassification(" =ERROR,FINAL=WARN"); });
    }

    @Test
    public void testValidateResourcePathPatternWithValidPatterns() {
        AemClassificationValidatorFactory.validateResourcePathPattern("/libs");
        AemClassificationValidatorFactory.validateResourcePathPattern("/libs/some/path");
        AemClassificationValidatorFactory.validateResourcePathPattern(".*/test");
    }

    @Test
    public void testValidateResourcePathPatternWithInvalidPatterns() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("relative/path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("/"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> AemClassificationValidatorFactory.validateResourcePathPattern("[^/].*"));
    }

    @Test
    public void testCreateValidator() {
        AemClassificationValidatorFactory factory = new AemClassificationValidatorFactory();
        Map<String, String> options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map");
        // deprecated option for whitelisting
        options.put("whitelistedResourcePathsPatterns", "/resourceType1/.*,/resourceType2");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG");
        ValidatorSettings settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        ContentClassificationMapper map = new ContentClassificationMapperImpl("Simple");
        map.put("/test", ContentClassification.INTERNAL_DEPRECATED, "Deprecated");
        Collection<String> whiteListedResourceTypes = new LinkedList<>();
        whiteListedResourceTypes.add("/resourceType1/.*");
        whiteListedResourceTypes.add("/resourceType2");
        Map<ContentClassification, ValidationMessageSeverity> severitiesPerClassification = new HashMap<>();
        severitiesPerClassification.put(ContentClassification.INTERNAL, ValidationMessageSeverity.DEBUG);
        AemClassificationValidator expectedValidator = new AemClassificationValidator(ValidationMessageSeverity.WARN, map, whiteListedResourceTypes, severitiesPerClassification);
        Assertions.assertEquals(expectedValidator, factory.createValidator(null, settings));
        
        options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map");
        // new option for whitelisting
        options.put("whitelistedResourcePathPatterns", "/resourceType1/.*,/resourceType2");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG");
        settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        Assertions.assertEquals(expectedValidator, factory.createValidator(null, settings));

        // test with multiple validation maps including whitespaces in the maps string
        options = new HashMap<>();
        options.put("maps", "tccl:valid-classification.map,tccl:empty-map-1.map,\n  \t tccl:empty-map-2.map");
        options.put("whitelistedResourcePathPatterns", "/resourceType1/.*,/resourceType2");
        options.put("severitiesPerClassification", "INTERNAL=DEBUG");
        settings = new ValidatorSettingsImpl(false, ValidationMessageSeverity.WARN, options);
        Assertions.assertEquals(expectedValidator, factory.createValidator(null, settings));
    }
    
    private static final class ValidatorSettingsImpl implements ValidatorSettings {
        
        private final boolean isDisabled;
        private final ValidationMessageSeverity defaultSeverity;
        private Map<String, String> options;
        
        public ValidatorSettingsImpl(boolean isDisabled, ValidationMessageSeverity defaultSeverity, Map<String, String> options) {
            super();
            this.isDisabled = isDisabled;
            this.defaultSeverity = defaultSeverity;
            this.options = options;
        }

        @Override
        public @NotNull ValidationMessageSeverity getDefaultSeverity() {
            return defaultSeverity;
        }

        @Override
        public @NotNull Map<String, String> getOptions() {
            return options;
        }

        @Override
        public boolean isDisabled() {
            return isDisabled;
        }

    }
}
