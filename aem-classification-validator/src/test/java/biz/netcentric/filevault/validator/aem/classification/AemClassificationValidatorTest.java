package biz.netcentric.filevault.validator.aem.classification;

/*-
 * #%L
 * AEM Classification Validator
 * %%
 * Copyright (C) 2020 Netcentric - A Cognizant Digital Business
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.vault.util.Text;
import org.apache.jackrabbit.vault.validation.ValidationViolation;
import org.apache.jackrabbit.vault.validation.impl.util.ValidatorSettingsImpl;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.apache.jackrabbit.vault.validation.spi.impl.DocumentViewParserValidator;
import org.apache.jackrabbit.vault.validation.spi.impl.DocumentViewParserValidatorFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import biz.netcentric.filevault.validator.aem.classification.map.MutableContentClassificationMapImpl;

class AemClassificationValidatorTest {
    private MutableContentClassificationMap classificationMap;

    private AemClassificationValidator validator;

    private static final Path SIMPLEFILE_HTL_PATH = Paths.get("/apps/example-htl.html");
    private static final Path SIMPLEFILE_JSP_PATH = Paths.get("/apps/example.jsp");
    private static final Path OVERLAY_DOCVIEW_PATH = Paths.get("/apps/.content.xml");
    private static final Path EXAMPLE_DOCVIEW_PATH = Paths.get("/apps/example/.content.xml");

    @BeforeEach
    void setUp() {
        classificationMap = new MutableContentClassificationMapImpl("1.0.0");
        classificationMap.put("/libs/abstract", ContentClassification.ABSTRACT, "abstractremark");
        classificationMap.put("/libs/final", ContentClassification.FINAL, "finalremark");
        classificationMap.put("/libs/internal", ContentClassification.INTERNAL, "internalremark");
        classificationMap.put("/libs/public", ContentClassification.PUBLIC, "publicremark");
        classificationMap.put("/", ContentClassification.PUBLIC, "");
        validator = new AemClassificationValidator(ValidationMessageSeverity.ERROR, classificationMap, Collections.emptyList(), Collections.emptyMap());
    }

    @Test
    void testHtlIncludePattern() {
        // use single quotes in expression string literals
        assertFalse(AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\"${ @ path='path/to/resource'}\"></article>").find());
        assertFalse(AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\"${ @ path='path/to/resource',removeSelectors}\"></article>").find());
        Matcher matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\"${'resource' @ resourceType='resourceType'}\">");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\"${ @ path='path/to/resource',removeSelectors, resourceType  =  'resourceType'}\"></article>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\"${ @ path='path/to/resource',removeSelectors, resourceType= 'resourceType', someOtherOption}\">");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        
        // use double quotes in expression string literals
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\'${\"resource\" @ resourceType=\"resourceType\"}'>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\'${ @ path=\"path/to/resource\",removeSelectors, resourceType  =  \"resourceType\"}\'></article>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=\'${ @ path=\"path/to/resource\",removeSelectors, resourceType= \"resourceType\", someOtherOption}\'>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        
        // use attributes without quotes
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=${\'resource\'@resourceType=\'resourceType\'}>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
        matcher = AemClassificationValidator.HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE.matcher("<article data-sly-resource=${ @ path=\"path/to/resource\",removeSelectors,resourceType=\"resourceType\"}></article>");
        assertTrue(matcher.find());
        assertEquals("resourceType", matcher.group(1));
    }

    @Test
    void testReferencingViolationsInDocviewXml()
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, FileSystemException {
        ContentUsage usage = ContentUsage.REFERENCE;
        assertJcrDocViewValidationMessages(validator, EXAMPLE_DOCVIEW_PATH, "/referencing.xml",
                new ClassificationViolation("/apps/example/test1", 4, 82,  usage, "/libs/abstract", ContentClassification.ABSTRACT, "abstractremark"),
                new ClassificationViolation("/apps/example/test1/test11", 5, 94, usage, "/libs/abstract/test", ContentClassification.ABSTRACT, "abstractremark"),
                new ClassificationViolation("/apps/example/test1/test12", 6, 88, usage, "abstract/test", ContentClassification.ABSTRACT, "abstractremark"),
                //new ClassificationViolation("/apps/example/test2", 8, 84,  usage, "/libs/final", ContentClassification.FINAL, "finalremark"),
                new ClassificationViolation("/apps/example/test2/test21", 9, 91, usage, "/libs/final/test", ContentClassification.INTERNAL_CHILD, "finalremark"),
                new ClassificationViolation("/apps/example/test3", 11, 82, usage, "/libs/internal", ContentClassification.INTERNAL, "internalremark"),
                new ClassificationViolation("/apps/example/test3/test31", 12, 94, usage, "/libs/internal/test", ContentClassification.INTERNAL, "internalremark")
                //new ClassificationViolation("/apps/example/test4", 14, 1, usage, "/libs/public", ContentClassification.PUBLIC, "publicremark"),
                //new ClassificationViolation("/apps/example/test4/test41", 15, 1, usage, "/libs/public/test", ContentClassification.PUBLIC, "publicremark")
         );
    }

    @Test
    void testReferencingWithTrailingSlashesInDocviewXml()
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, FileSystemException {
        Assertions.assertEquals(Collections.singletonList(
                new ValidationViolation("myId", ValidationMessageSeverity.ERROR, "Resource path must not end with '/' but is 'core/wcm/components/teaser/v1/teaser/'", EXAMPLE_DOCVIEW_PATH, Paths.get(""), "/apps/example", 8, 107, null)
                ), validateJcrDocView(validator, "myId", "/invalid-resource-type.xml", EXAMPLE_DOCVIEW_PATH));
    }

    @Test
    void testInheritingViolationsInDocviewXml()
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, FileSystemException {
        
        ContentUsage usage = ContentUsage.INHERIT;
        assertJcrDocViewValidationMessages(validator, EXAMPLE_DOCVIEW_PATH, "/inheriting.xml",
                //new ClassificationViolation("/apps/example/test1", 4, 82,  usage, "/libs/abstract", ContentClassification.ABSTRACT, "abstractremark"),
                //new ClassificationViolation("/apps/example/test1/test11", 5, 94, usage, "/libs/abstract/test", ContentClassification.ABSTRACT, "abstractremark"),
                //new ClassificationViolation("/apps/example/test1/test12", 6, 88, usage, "abstract/test", ContentClassification.ABSTRACT, "abstractremark"),
                new ClassificationViolation("/apps/example/test2", 8, 84,  usage, "/libs/final", ContentClassification.FINAL, "finalremark"),
                new ClassificationViolation("/apps/example/test2/test21", 9, 96, usage, "/libs/final/test", ContentClassification.INTERNAL_CHILD, "finalremark"),
                new ClassificationViolation("/apps/example/test3", 11, 87, usage, "/libs/internal", ContentClassification.INTERNAL, "internalremark"),
                new ClassificationViolation("/apps/example/test3/test31", 12, 99, usage, "/libs/internal/test", ContentClassification.INTERNAL, "internalremark")
                //new ClassificationViolation("/apps/example/test4", 14, 1, usage, "/libs/public", ContentClassification.PUBLIC, "publicremark"),
                //new ClassificationViolation("/apps/example/test4/test41", 15, 1, usage, "/libs/public/test", ContentClassification.PUBLIC, "publicremark")
           );
    }

    @Test
    void testOverlayingViolationsInDocviewXml()
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, FileSystemException {
        ContentUsage usage = ContentUsage.OVERLAY;
        assertJcrDocViewValidationMessages(validator, OVERLAY_DOCVIEW_PATH, "/overlaying.xml",
                new ClassificationViolation("/apps/final", 8, 46, usage, "/libs/final", ContentClassification.FINAL, "finalremark"),
                new ClassificationViolation("/apps/final/test21", 9, 53, usage, "/libs/final/test21", ContentClassification.INTERNAL_CHILD, "finalremark"),
                new ClassificationViolation("/apps/internal", 11, 49, usage, "/libs/internal", ContentClassification.INTERNAL, "internalremark"),
                new ClassificationViolation("/apps/internal/test31", 12, 53, usage, "/libs/internal/test31", ContentClassification.INTERNAL, "internalremark")
           );
    }

    @Test
    void testOverlayingViolationsInSimpleFiles()
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, FileSystemException {
        assertNull(validator.validate("/apps/abstract"));
        assertNull(validator.validate("/apps/abstract/test11"));
        assertNull(validator.validate("/apps/abstract/test12"));
        assertEquals(Collections.singleton(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.OVERLAY, "/libs/final", ContentClassification.FINAL, "finalremark")), validator.validate("/apps/final"));
        assertEquals(Collections.singleton(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.OVERLAY, "/libs/final/test21", ContentClassification.INTERNAL_CHILD, "finalremark")), validator.validate("/apps/final/test21"));
        assertEquals(Collections.singleton(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.OVERLAY, "/libs/internal", ContentClassification.INTERNAL, "internalremark")), validator.validate("/apps/internal"));
        assertEquals(Collections.singleton(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.OVERLAY, "/libs/internal/test21",ContentClassification.INTERNAL,  "internalremark")), validator.validate("/apps/internal/test21"));
        assertNull(validator.validate("/apps/public"));
        assertNull(validator.validate("/apps/public/test41"));
    }

    @Test
    void testReferencingViolationsInHtlAndJsp() throws IOException {
        // check content of JSPs and HTLs
        assertTrue(validator.shouldValidateJcrData(Paths.get("/apps/mytest/component/componentA/componentA.html")));
        assertTrue(validator.shouldValidateJcrData(Paths.get("/apps/mytest/component/componentA/componentA.jsp")));
        assertFalse(validator.shouldValidateJcrData(Paths.get("/apps/mytest/component/componentA/componentA.js")));

        // check HTL which references protected resource type
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("htl-example.html")) {
            Collection<ValidationMessage> messages = validator.validateJcrData(input, SIMPLEFILE_HTL_PATH, new HashMap<String, Integer>());
            // and check violations
            assertEquals(Collections.singletonList(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.REFERENCE, "/libs/abstract/test",  ContentClassification.ABSTRACT, "abstractremark")), messages);
        }
        
        // check JSP which references protected resource type
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("example.jsp")) {
            Collection<ValidationMessage> messages = validator.validateJcrData(input, SIMPLEFILE_JSP_PATH, new HashMap<String, Integer>());
            // and check violations
            assertEquals(Collections.singletonList(getSimpleFileViolationMessage(ValidationMessageSeverity.ERROR, ContentUsage.REFERENCE, "/libs/abstract/test",  ContentClassification.ABSTRACT, "abstractremark")), messages);
        }
    }
    static final class ClassificationViolation {
        private final String nodePath;
        private final String name;
        private final int line;
        private final int column;
        private final ContentUsage usage;
        private final String targetResourceType; 
        private final ContentClassification classification;
        private final String remark;

        ClassificationViolation(String nodePath, int line, int column, ContentUsage usage, String targetResourceType, ContentClassification classification,
                String remark) {
            super();
            this.nodePath = nodePath;
            this.line = line;
            this.column = column;
            this.usage = usage;
            this.targetResourceType = targetResourceType;
            this.classification = classification;
            this.remark = remark;
            this.name = Text.getName(nodePath);
        }
    }

    static void assertJcrDocViewValidationMessages(AemClassificationValidator validator, Path path, String name, ClassificationViolation... violations) throws IOException, ParserConfigurationException, SAXException {
        Collection<ValidationMessage> actualMessages = validateJcrDocView(validator, "myid", name, path);
        Collection<ValidationViolation> expectedMessages = Arrays.stream(violations).map(a -> 
            ValidationViolation.wrapMessage("myid", 
                    getDocviewViolationMessage(ValidationMessageSeverity.ERROR, a.name, a.usage, a.targetResourceType, a.classification, a.remark),
                    path, Paths.get(""), a.nodePath, a.line, a.column)).collect(Collectors.toList());
        
        assertEquals(expectedMessages, actualMessages);
    }

    private static Collection<ValidationMessage> validateJcrDocView(AemClassificationValidator validator, String validatorId, String name, Path filePath) throws IOException, ParserConfigurationException, SAXException {
        try (InputStream input = AemClassificationValidatorTest.class.getResourceAsStream(name)) {
            ValidatorSettings settings = new ValidatorSettingsImpl();
            DocumentViewParserValidator docViewValidator = (DocumentViewParserValidator) new DocumentViewParserValidatorFactory().createValidator(null, settings);
            docViewValidator.setDocumentViewXmlValidators(Collections.singletonMap(validatorId, validator));
            Map<String, Integer> nodePathsAndLineNumbers = new HashMap<>();
            Collection<ValidationMessage> allMessages = docViewValidator.validateJcrData(input, filePath, nodePathsAndLineNumbers);
            return allMessages.stream().filter(a -> a.getSeverity().ordinal() >= ValidationMessageSeverity.ERROR.ordinal()).collect(Collectors.toList());
        }
    }

    /** Pattern to be used with {@link String#format(String, Object...)} */
    static final String DOCVIEW_VIOLATION_MESSAGE_STRING = "Element with name \"%s\" %s resource '%s' which is marked as '%s'. It therefore violates the content classification!";
    static final String SIMPLEFILE_VIOLATION_MESSAGE_STRING = "This file %s resource '%s' which is marked as '%s'. It therefore violates the content classification!";
    static @NotNull ValidationMessage getDocviewViolationMessage(ValidationMessageSeverity severity, String label, ContentUsage usage, String targetResourceType, ContentClassification classification, String remark) {
        String message = AemClassificationValidator.extendMessageWithRemark(String.format(DOCVIEW_VIOLATION_MESSAGE_STRING, label, usage.getLabel(), targetResourceType, classification.getLabel()), remark);
        return new ValidationMessage(severity, message);
    }

    static @NotNull ValidationMessage  getSimpleFileViolationMessage(ValidationMessageSeverity severity, ContentUsage usage, String resourceType, ContentClassification classification, String remark) {
        return new ValidationMessage(severity, AemClassificationValidator.extendMessageWithRemark(String.format(SIMPLEFILE_VIOLATION_MESSAGE_STRING, usage.getLabel(), resourceType, classification.getLabel()), remark));
    }
}
