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
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A classification map consists out of classifications per resource type (i.e. repository paths)
 */
public interface ContentClassificationMapper {

    /**
     * Returns the classification for the given resource type.
     * In case the given {@code resourcePath} matches any of the {@code whitelistedResourcePaths} it returns PUBLIC.
     * @param resourcePath the absolute resource path
     * @param whitelistedResourcePaths the whitelisted resource paths as regular expression patterns. 
     * @return the classification and the optional remark belonging to the given resource type
     */
    @NotNull Entry<ContentClassification, String> getContentClassificationAndRemarkForResourcePath(@NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourcePaths);

    /**
     * Writes the map to a given output stream.
     * Leaves the output stream open.
     * 
     * @param outputStream the stream to write to
     * @throws IOException in case of any exception during writing
     */
    void write(@NotNull OutputStream outputStream) throws IOException;

    /**
     * Adds a new entry to the classification map.
     * @param resourcePath the absolute resource path
     * @param classification the classification
     * @param remark the optional remark (may be null)
     */
    void put(@NotNull String resourcePath, @NotNull ContentClassification classification, @Nullable String remark);

    /**
     * Returns the number of entries in this map.
     * @return the number of entries
     */
    int size();
    
    /**
     * Returns label of this map (e.g. the AEM version).
     * @return the label
     */
    @NotNull String getLabel();

    void merge(ContentClassificationMapper otherMap);
}
