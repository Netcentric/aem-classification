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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.vault.util.DocViewNode;
import org.apache.jackrabbit.vault.validation.spi.DocumentViewXmlValidator;
import org.apache.jackrabbit.vault.validation.spi.GenericJcrDataValidator;
import org.apache.jackrabbit.vault.validation.spi.NodePathValidator;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AemClassificationValidator implements DocumentViewXmlValidator, GenericJcrDataValidator, NodePathValidator {

    /**
     * Example HTL code which should be matched by the RegEx:
     * <br>
     * <code>&lt;article data-sly-resource="${'path/to/resource' @ resourceType='&lt;some resource type&gt;'}"&gt;</code>
     * <br>
     * The first subgroup must contain the value of the resource type.
     * This pattern only works if the resource type is given as literal!
     * 
     * @see <a href="https://helpx.adobe.com/experience-manager/htl/using/block-statements.html#resource">data-sly-resource</a>
     */
    static final Pattern HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE = Pattern.compile("data-sly-resource\\s*=[^@]*.*?resourceType\\s*=\\s*(?:\"|\')([^'\"]*)(?:\"|\')");
    
    /**
     * Example JSP code which should be matched by the RegEx:
     * <br>
     * {@code <cq:include path="trail" resourceType="<some resource type>" ... /> } or
     * {@code <sling:include ... resourceType="<some resource type>" ... /> }
     * <br>
     * The first subgroup must contain the value of the resource type.
     * This pattern only works if the resource type is given as literal!
     * 
     * @see <a href="https://experienceleague.adobe.com/docs/experience-manager-65/developing/platform/taglib.html?lang=en">CQ/Sling Tag Library</a>
     */
    private static final Pattern JSP_INCLUDE_OVERWRITING_RESOURCE_TYPE = Pattern.compile("(?:<cq:|<sling:)include resourceType\\s*=\\s*(?:\"|\')([^'\"]*)(?:\"|\')");

    private static final PathMatcher HTL_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.html");
    private static final PathMatcher JSP_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.jsp");

    private static final String SLING_RESOURCE_TYPE_PROPERTY_NAME = NameFactoryImpl.getInstance().create(JcrResourceConstants.SLING_NAMESPACE_URI,SlingConstants.PROPERTY_RESOURCE_TYPE).toString();
    private static final String SLING_RESOURCE_SUPER_TYPE_PROPERTY_NAME = NameFactoryImpl.getInstance().create(JcrResourceConstants.SLING_NAMESPACE_URI,SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE).toString();

    public static final String MESSAGE_SUBJECT_NODE = "Element with name \"%s\"";
    public static final String MESSAGE_SUBJECT_FILE = "This file";

    public static final Pattern JCR_EXPANDED_FORM_PATTERN = Pattern.compile("\\{([^\\}]*)\\}(.*)$");
    /** Pattern to be used with {@link String#format(String, Object...)} */
    static final String VIOLATION_MESSAGE_STRING = "%s %s resource '%s' which is marked as '%s'. It therefore violates the content classification!";

    private final ContentClassificationMap classificationMap;
    private final Collection<String> whitelistedResourcePaths;
    private final Collection<Pattern> whitelistedResourcePathPatterns;
    private final Map<ContentClassification, ValidationMessageSeverity> severityPerClassification;

    private @NotNull ValidationMessageSeverity defaultSeverity;
    private final Collection<String> overlaidNodePaths;

    public AemClassificationValidator(@NotNull ValidationMessageSeverity defaultSeverity, @NotNull ContentClassificationMap classificationMap, @NotNull Collection<String> whitelistedResourcePaths, @NotNull Map<ContentClassification, ValidationMessageSeverity> severityPerClassification) {
        super();
        this.defaultSeverity = defaultSeverity;
        this.classificationMap = classificationMap;
        this.whitelistedResourcePaths = whitelistedResourcePaths;
        this.whitelistedResourcePathPatterns = whitelistedResourcePaths.stream().map(Pattern::compile).collect(Collectors.toList());
        this.severityPerClassification = severityPerClassification;
        this.overlaidNodePaths = new LinkedList<>();
    }

    public Collection<ValidationMessage> done() {
        return Collections.singleton(new ValidationMessage(ValidationMessageSeverity.INFO,"Successfully checked against classification maps: " + classificationMap.getLabel() + " (" + classificationMap.size() + " entries)"));
    }

    @Override
    public Collection<ValidationMessage> validate(@NotNull String path) {
        if (!overlaidNodePaths.contains(path)) {
            // check overlay usage in addition for non-docview files
            ValidationMessage message = validateClassification(path,  ContentUsage.OVERLAY, MESSAGE_SUBJECT_FILE);
            if (message != null) {
               return Collections.singleton(message);
            }
        }
        return null;
    }

    @Override
    public boolean shouldValidateJcrData(@NotNull Path filePath) {
        return (isHtlFile(filePath) || isJspFile(filePath));
    }

    @Override
    public Collection<ValidationMessage> validateJcrData(@NotNull InputStream input, @NotNull Path filePath, @NotNull Map<String, Integer> nodePathsAndLineNumbers) throws IOException {
        final Pattern regex;
        if (isHtlFile(filePath)) {
            regex = HTL_INCLUDE_OVERWRITING_RESOURCE_TYPE;
        } else if (isJspFile(filePath)) {
            regex = JSP_INCLUDE_OVERWRITING_RESOURCE_TYPE;
        } else {
            throw new IllegalStateException("The given file is neither JSP nor HTL (" + filePath + ")");
        }
        Collection<ValidationMessage> messages = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = regex.matcher(line);
                while (matcher.find()) {
                    String resourceType = matcher.group(1);
                    ValidationMessage message = validateClassification(resourceType, ContentUsage.REFERENCE, MESSAGE_SUBJECT_FILE);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
        }
        return messages;
    }

    /**
     * JCR expanded form is not very user friendly therefore try to format as more readable string
     * @return the given argument in a more readable format
     */
    static String jcrExpandedFormNameToReadableFormat(String jcrExpandedFormName) {
        Matcher matcher = JCR_EXPANDED_FORM_PATTERN.matcher(jcrExpandedFormName);
        if (matcher.matches()) {
            if (matcher.group(1).isEmpty()) {
                return matcher.group(2);
            } else {
                return matcher.group(2) + " (Namespace URI: " + matcher.group(1) + ")";
            }
        } else {
            return jcrExpandedFormName;
        }
    }

    @Override
    public Collection<ValidationMessage> validate(@NotNull DocViewNode node, @NotNull String nodePath, @NotNull Path filePath, boolean isRoot) {
        Collection<ValidationMessage> messages = new LinkedList<>();
        String subject = String.format(MESSAGE_SUBJECT_NODE, jcrExpandedFormNameToReadableFormat(node.label));

        // attributes resourceType ...
        String usedResource = node.getValue(SLING_RESOURCE_TYPE_PROPERTY_NAME);
        ValidationMessage message = validateClassification(usedResource, ContentUsage.REFERENCE, subject);
        if (message != null) {
           messages.add(message);
        }

        // ... and resourceSuperType are considered
        String superResource = node.getValue(SLING_RESOURCE_SUPER_TYPE_PROPERTY_NAME);
        message = validateClassification(superResource, ContentUsage.INHERIT, subject);
        if (message != null) {
           messages.add(message);
        }

        // check overlays in addition
        message = validateClassification(nodePath, ContentUsage.OVERLAY, subject);
        if (message != null) {
           messages.add(message);
           overlaidNodePaths.add(nodePath);
        }
        
        // TODO: check usage of clientlib dependencies/embeds
        return messages;
    }

    private @Nullable ValidationMessage validateClassification(String resourcePath, ContentUsage usage, String messageSubject) {
        if (resourcePath == null) {
            // skip validation
            return null;
        }
        if (resourcePath.endsWith("/") && !resourcePath.equals("/")) {
            // add subject and usage to message
            return new ValidationMessage(defaultSeverity, "Resource path must not end with '/' but is '" + resourcePath + "'");
        }
        
        if (usage == ContentUsage.OVERLAY) {
            if (!resourcePath.startsWith("/apps/")) {
                return null; // this is not an overlay at all, therefore no violation
            }
            // is this an overlay?
            resourcePath = "/libs/" + resourcePath.substring("/apps/".length());
        }
        Entry<ContentClassification,String> classificationAndRemark = classificationMap.getContentClassificationAndRemarkForResourcePath(resourcePath, whitelistedResourcePathPatterns);
        ContentClassification classification = classificationAndRemark.getKey();
        if (!classificationAndRemark.getKey().isAllowed(usage)) {
            return new ValidationMessage(getSeverityForClassification(classification), extendMessageWithRemark(String.format(VIOLATION_MESSAGE_STRING, messageSubject, usage.getLabel(), resourcePath, classification.getLabel()), classificationAndRemark.getValue()));
        }
        return null;
    }

    private static boolean isHtlFile(Path file) {
        return HTL_PATH_MATCHER.matches(file);
    }

    private static boolean isJspFile(Path file) {
        return JSP_PATH_MATCHER.matches(file);
    }

    static @NotNull String extendMessageWithRemark(@NotNull String message, String remark) {
        if (StringUtils.isNotBlank(remark)) {
            return message + " Remark: " + remark;
        }
        return message;
    }
    
    @NotNull ValidationMessageSeverity getSeverityForClassification(ContentClassification classification) {
        ValidationMessageSeverity severity = severityPerClassification.get(classification);
        return severity != null ? severity : defaultSeverity;
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classificationMap == null) ? 0 : classificationMap.hashCode());
        result = prime * result + ((defaultSeverity == null) ? 0 : defaultSeverity.hashCode());
        result = prime * result + ((whitelistedResourcePaths == null) ? 0 : whitelistedResourcePaths.hashCode());
        result = prime * result + ((severityPerClassification == null) ? 0 : severityPerClassification.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AemClassificationValidator other = (AemClassificationValidator) obj;
        if (classificationMap == null) {
            if (other.classificationMap != null)
                return false;
        } else if (!classificationMap.equals(other.classificationMap))
            return false;
        if (defaultSeverity != other.defaultSeverity)
            return false;
        if (whitelistedResourcePaths == null) {
            if (other.whitelistedResourcePaths != null)
                return false;
        } else if (!whitelistedResourcePaths.equals(other.whitelistedResourcePaths))
            return false;
        if (severityPerClassification == null) {
            if (other.severityPerClassification != null)
                return false;
        } else if (!severityPerClassification.equals(other.severityPerClassification))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AemClassificationValidator [" + (classificationMap != null ? "classificationMap=" + classificationMap + ", " : "")
                + (whitelistedResourcePaths != null ? "resourceTypeWhitelist=" + whitelistedResourcePaths + ", " : "")
                + (severityPerClassification != null ? "severityPerClassification=" + severityPerClassification + ", " : "")
                + (defaultSeverity != null ? "defaultSeverity=" + defaultSeverity : "") + "]";
    }
}
