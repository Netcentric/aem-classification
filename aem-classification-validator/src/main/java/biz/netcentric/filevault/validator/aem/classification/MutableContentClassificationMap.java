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

import java.io.IOException;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mutable {@link ContentClassificationMap} which can also be serialized.
 * This is only used from aem-classification-maven-plugin currently.
 */
public interface MutableContentClassificationMap extends ContentClassificationMap {


    /**
     * Writes the map to a given output stream.
     * Closes the stream upon completion.
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

}