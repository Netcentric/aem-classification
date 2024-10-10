package biz.netcentric.filevault.validator.aem.classification.map;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.jackrabbit.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.ContentClassificationMap;

/** 
 * A map containing content classifications for repository node paths.
 * 
 * Supports reading from an input stream which is a CSV serialization of the map where each line represents one item in the
 * map and has the format
 * 
 * <pre>
 * &#60;path&#62;,&#60;classification&#62;{,&#60;remark&#62;}
 * </pre>
 * 
 * where {@code classification} is one of {@link ContentClassification}. The CSV format is
 * based on <a href="https://tools.ietf.org/html/rfc4180">RFC-4180</a>
 * In addition a comment starting with {@code #} on the first line is supposed to contain the AEM version. 
 * @see MutableContentClassificationMapImpl
 */
public class ContentClassificationMapImpl implements ContentClassificationMap {

    protected final Map<String, ContentClassification> classificationMap; // key = absolute repository path
    protected final Map<String, String> remarkMap; // key = absolute repository path
    private String label;

    static final CSVFormat CSV_FORMAT = CSVFormat.Builder.create(CSVFormat.RFC4180).setCommentMarker('#').build();
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentClassificationMapImpl.class);

    public ContentClassificationMapImpl(String label) {
        this.classificationMap = new TreeMap<>(); // this is sorted by key
        this.remarkMap = new HashMap<>();
        this.label = label;
    }

    public ContentClassificationMapImpl(@NotNull InputStream input, String fileName) throws IOException {
        this("");
        Iterable<CSVRecord> records = CSV_FORMAT.parse(new InputStreamReader(input, StandardCharsets.US_ASCII));
        for (CSVRecord record : records) {
            if (record.getRecordNumber() == 1) {
                label = record.getComment();
            }
            if (record.size() < 2) {
                throw new IllegalArgumentException("Error in line " + record.getRecordNumber()
                        + ": Missing ',' character. At least 2 values have to be given per line!");
            }
            String resourcePath = record.get(0);
            ContentClassification classification = Enum.valueOf(ContentClassification.class, record.get(1));
            final String remark;
            // a third part may contain hints (separated from second part with ":")
            if (record.size() == 3) {
                remark = record.get(2);
            } else {
                if (record.size() > 3) {
                    LOGGER.warn("More than 3 columns in line {} in file {} given, ignoring the ones exceeding the 3rd column.", record.getRecordNumber(), fileName);
                }
                remark = null;
            }
            put(resourcePath, classification, remark);
        }
    }

    protected void put(@NotNull String resourcePath, @NotNull ContentClassification classification, @Nullable String remark) {
        // validate that only absolute resource types are given
        if (!resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("Only absolute resource paths are supported, but resource path given is '" + resourcePath + "'.");
        }
        classificationMap.put(resourcePath, classification);
        if (remark != null && !remark.isEmpty()) {
            remarkMap.put(resourcePath, remark);
        }
    }

    @Override
    @NotNull
    public Entry<ContentClassification, String> getContentClassificationAndRemarkForResourcePath(@NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourcePaths) {
        // ignore empty resourceTypes
        if (resourcePath.isEmpty()) {
            return new SimpleEntry<>(ContentClassification.PUBLIC, null);
        }
        
        // make resourceType absolute!
        if (!resourcePath.startsWith("/")) {
            // always assume "/libs" to be on the resource resolver's search path
            resourcePath = "/libs/" + resourcePath;
        }
        if (resourcePath.endsWith("/") && !resourcePath.equals("/")) {
            throw new IllegalStateException("Resource path must not end with '/' but is '" + resourcePath + "'");
        }
    
        // is the resource type whitelisted?
        if (isResourcePathWhitelisted(resourcePath, whitelistedResourcePaths)) {
            LOGGER.debug("Resource path '{}' is explicitly whitelisted and therefore has no restrictions!", resourcePath);
            return new SimpleEntry<>(ContentClassification.PUBLIC, null);
        }
        // check for direct match first
        ContentClassification classification = classificationMap.get(resourcePath);
        if (classification != null) {
            LOGGER.debug("Found exact match for classification of '{}': {}", resourcePath, classification.getLabel());
            return new SimpleEntry<>(classification, remarkMap.get(resourcePath));
        }
    
        // get longest prefix entry, which still matches
        String parentResourceType = resourcePath;
        // go to parent
        while (!parentResourceType.equals("/")) {
            parentResourceType = Text.getRelativeParent(parentResourceType, 1);
            classification = classificationMap.get(parentResourceType);
            if (classification != null) {
                LOGGER.debug("Found inexact match for classification of '{}' at '{}': {}", resourcePath, parentResourceType, classification.getChildNodeClassification().getLabel());
                return new SimpleEntry<ContentClassification, String>(classification.getChildNodeClassification(),
                        remarkMap.get(parentResourceType));
            }
        }
        throw new IllegalStateException("Could not find a classification for resource path '" + resourcePath + "'");
    }

    private boolean isResourcePathWhitelisted(@NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourceTypes) {
        if (whitelistedResourceTypes == null) {
            return false;
        }
        return whitelistedResourceTypes.stream().anyMatch(r -> r.matcher(resourcePath).matches());
    }

    @Override
    public int size() {
        return classificationMap.size();
    }

    @Override
    @NotNull
    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classificationMap, label, remarkMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ContentClassificationMapImpl))
            return false;
        ContentClassificationMapImpl other = (ContentClassificationMapImpl) obj;
        return Objects.equals(classificationMap, other.classificationMap) && Objects.equals(label, other.label)
                && Objects.equals(remarkMap, other.remarkMap);
    }

    @Override
    public String toString() {
        return "ContentClassificationMapImpl [version=" + label + ", classificationMap=" + classificationMap + ", remarkMap=" + remarkMap
                + "]";
    }

}
