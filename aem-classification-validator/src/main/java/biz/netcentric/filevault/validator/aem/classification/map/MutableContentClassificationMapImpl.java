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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.MutableContentClassificationMap;

/** 
 * A mutable {@link ContentClassificationMapImpl} which supports serialization.
 */
public class MutableContentClassificationMapImpl extends ContentClassificationMapImpl implements MutableContentClassificationMap {

    public MutableContentClassificationMapImpl(@NotNull String label) {
        super(label);
    }

    @Override
    public void put(@NotNull String resourcePath, @NotNull ContentClassification classification, @Nullable String remark) {
        super.put(resourcePath, classification, remark);
    }

    @Override
    public void write(@NotNull OutputStream output) throws IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(output, StandardCharsets.US_ASCII), CSV_FORMAT)) {
            csvPrinter.printComment(getLabel());
            for (Entry<String, ContentClassification> entry : classificationMap.entrySet()) {
                Collection<String> values = new LinkedList<>();
                values.add(entry.getKey()); // resource type
                values.add(entry.getValue().toString());
                String remark = remarkMap.get(entry.getKey());
                if (remark != null && !remark.isEmpty()) {
                    values.add(remark);
                }
                csvPrinter.printRecord(values);
            }
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        // allow equality check amongst this class and its superclass
        return true;
    }

}
