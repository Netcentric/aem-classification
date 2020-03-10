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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A map containing content classifications for repository node paths.
 * 
 * Supports writing to a file and reading from a file. The file is a CSV serialization of the map where each line represents one item in the
 * map and has the format
 * 
 * <pre>
 * &#60;path&#62;,&#60;classification&#62{,&#60;remark&#62;}
 * </pre>
 * 
 * where {@code classification} is one of {@link ContentClassification}. The CSV format is
 * based on <a href="https://tools.ietf.org/html/rfc4180">RFC-4180</a>
 * In addition a comment starting with {@code #} on the first line is supposed to contain the AEM version. 
 */
public class ContentClassificationMapperImpl implements ContentClassificationMapper {

    private final Map<String, ContentClassification> classificationMap; // key = absolute repository path
    private final Map<String, String> remarkMap; // key = absolute repository path
    private String label;

    private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.withCommentMarker('#');
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentClassificationMapperImpl.class);

    public ContentClassificationMapperImpl(@NotNull String label) {
        this.classificationMap = new TreeMap<>(); // this is sorted by key
        this.remarkMap = new HashMap<>();
        this.label = label;
    }

    public ContentClassificationMapperImpl(@NotNull InputStream input, String fileName) throws IOException {
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
            String resourceType = record.get(0);
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
            put(resourceType, classification, remark);
        }
    }

    @Override
    public @NotNull Entry<ContentClassification, String> getContentClassificationAndRemarkForResourcePath(@NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourcePaths) {
        // ignore empty resourceTypes
        if (StringUtils.isBlank(resourcePath)) {
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
    public void put(@NotNull String resourcePath, @NotNull ContentClassification classification, @Nullable String remark) {
        // validate that only absolute resource types are given
        if (!resourcePath.startsWith("/")) {
            throw new IllegalArgumentException("Only absolute resource paths are supported, but resource path given is '" + resourcePath + "'.");
        }
        classificationMap.put(resourcePath, classification);
        if (StringUtils.isNotEmpty(remark)) {
            remarkMap.put(resourcePath, remark);
        }
    }

    @Override
    public int size() {
        return classificationMap.size();
    }

    @Override
    public @NotNull String getLabel() {
        return label;
    }

    @Override
    public void write(@NotNull OutputStream output) throws IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(new CloseShieldOutputStream(output), StandardCharsets.US_ASCII), CSV_FORMAT)) {
            csvPrinter.printComment(label);
            for (Entry<String, ContentClassification> entry : classificationMap.entrySet()) {
                Collection<String> values = new LinkedList<>();
                values.add(entry.getKey()); // resource type
                values.add(entry.getValue().toString());
                String remark = remarkMap.get(entry.getKey());
                if (StringUtils.isNotEmpty(remark)) {
                    values.add(remark);
                }
                csvPrinter.printRecord(values);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classificationMap == null) ? 0 : classificationMap.hashCode());
        result = prime * result + ((remarkMap == null) ? 0 : remarkMap.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
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
        ContentClassificationMapperImpl other = (ContentClassificationMapperImpl) obj;
        if (classificationMap == null) {
            if (other.classificationMap != null)
                return false;
        } else if (!classificationMap.equals(other.classificationMap))
            return false;
        if (remarkMap == null) {
            if (other.remarkMap != null)
                return false;
        } else if (!remarkMap.equals(other.remarkMap))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ContentClassificationMapImpl [version=" + label + ", classificationMap=" + classificationMap + ", remarkMap=" + remarkMap
                + "]";
    }

    @Override
    public void merge(ContentClassificationMapper otherMap) {
        Map<String, ContentClassification> entryMapToMerge = new HashMap<>();
        if (!(otherMap instanceof ContentClassificationMapper)) {
            throw new IllegalArgumentException("Can only call merge with a ContentClassificationMapper object as argument, but it is " + otherMap);
        }
        ContentClassificationMapperImpl otherMapImpl = ContentClassificationMapperImpl.class.cast(otherMap);
        // only merge at the end (once all items are processed)
        for (Map.Entry<String, ContentClassification> newEntry : otherMapImpl.classificationMap.entrySet()) {
            // what to do with inexact matches?
            // actually a previous entry on a parent node can be more restrictive
            ContentClassification oldClassification = getContentClassificationAndRemarkForResourcePath(newEntry.getKey(), null).getKey();
            if (oldClassification == null || oldClassification.ordinal() > newEntry.getValue().ordinal()) {
                entryMapToMerge.put(newEntry.getKey(), newEntry.getValue());
                removeChildEntriesWithLowerClassification(newEntry.getKey(), newEntry.getValue());
                String newRemark = otherMapImpl.remarkMap.get(newEntry.getKey());
                if (StringUtils.isNotEmpty(newRemark)) {
                    remarkMap.put(newEntry.getKey(), newRemark);
                }
            } else {
                LOGGER.debug("Could not merge classification '{}' for resource '{}' due to other classification '{}' with higher precedence",  newEntry.getValue(), newEntry.getKey(), oldClassification);
            }
        }
        classificationMap.putAll(entryMapToMerge);
        label = Stream.of(label, otherMapImpl.label).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.joining(", "));
    }

    private void removeChildEntriesWithLowerClassification(@NotNull String path, ContentClassification classification) {
        Iterator<Entry<String, ContentClassification>> i =  classificationMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, ContentClassification> entry = i.next();
            if (entry.getKey().startsWith(path)) {
                if (entry.getValue().ordinal() > classification.ordinal()) {
                    remarkMap.remove(entry.getKey());
                    i.remove();
                    LOGGER.debug("Remove entry '{}' with classification '{}' as this has a lower precedence as the new parent entry '{}' with  {}", entry.getKey(), entry.getValue(), path, classification);
                }
            }
        }
            
    }
}
