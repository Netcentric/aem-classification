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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.ContentClassificationMap;

/** 
 * Composite classification map backed by multiple {@link ContentClassificationMap}s.
 * It returns the strictest {@link ContentClassification} returned by any of the underlying maps (irrespective of
 * prefix length matching the given resource path) for {@link #getContentClassificationAndRemarkForResourcePath(String, Collection)}.
 */
public class CompositeContentClassificationMap implements ContentClassificationMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeContentClassificationMap.class);
    private final Collection<ContentClassificationMap> maps;

    public CompositeContentClassificationMap(@NotNull ContentClassificationMap... maps) {
        this(Arrays.asList(maps));
    }

    public CompositeContentClassificationMap(@NotNull Collection<ContentClassificationMap> maps) {
        if (maps.size() == 0) {
            throw new IllegalStateException("A composite map must consist of at least one map");
        }
        this.maps = new LinkedList<>(maps);
    }

    @Override
    public @NotNull Entry<ContentClassification, String> getContentClassificationAndRemarkForResourcePath(
            @NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourcePaths) {
        Entry<ContentClassification, String> resultingEntry = null;
        String mapLabel = null;
        for (ContentClassificationMap map : maps) {
            Entry<ContentClassification, String> entry = map.getContentClassificationAndRemarkForResourcePath(resourcePath, whitelistedResourcePaths);
            if (resultingEntry == null || entry.getKey().ordinal() <  resultingEntry.getKey().ordinal()) {
                resultingEntry = entry;
                mapLabel = map.getLabel();
            }
        }
        LOGGER.debug("Found strictest classification {} in map with label {}", resultingEntry.getKey(), mapLabel);
        return resultingEntry;
    }

    @Override
    public int size() {
        return maps.stream().mapToInt(ContentClassificationMap::size).sum();
    }

    @Override
    public @NotNull String getLabel() {
        return maps.stream().map(ContentClassificationMap::getLabel).collect(Collectors.joining(", ", "Composite Map of ", ""));
    }

    @Override
    public int hashCode() {
        return Objects.hash(maps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CompositeContentClassificationMap))
            return false;
        CompositeContentClassificationMap other = (CompositeContentClassificationMap) obj;
        return Objects.equals(maps, other.maps);
    }

    @Override
    public String toString() {
        return "CompositeContentClassificationMap [maps=" + maps + "]";
    }

}
