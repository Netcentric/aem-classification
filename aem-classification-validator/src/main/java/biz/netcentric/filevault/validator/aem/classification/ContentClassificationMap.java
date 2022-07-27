package biz.netcentric.filevault.validator.aem.classification;

/*-
 * #%L
 * AEM Classification Validator
 * %%
 * Copyright (C) 2022 Netcentric Cognizant
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import java.util.Collection;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A classification map consists out of classifications per resource type (i.e. repository paths)
 */
public interface ContentClassificationMap {

    /**
     * Returns the classification for the given resource type.
     * In case the given {@code resourcePath} matches any of the {@code whitelistedResourcePaths} it returns PUBLIC.
     * @param resourcePath the absolute resource path
     * @param whitelistedResourcePaths the whitelisted resource paths as regular expression patterns. 
     * @return the classification and the optional remark belonging to the given resource type
     */
    @NotNull Entry<ContentClassification, String> getContentClassificationAndRemarkForResourcePath(@NotNull String resourcePath, @Nullable Collection<Pattern> whitelistedResourcePaths);

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

}
