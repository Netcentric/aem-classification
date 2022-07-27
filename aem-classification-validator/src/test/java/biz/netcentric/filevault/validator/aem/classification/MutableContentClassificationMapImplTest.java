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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MutableContentClassificationMapImplTest {

    @Test
    public void testPersistAndLoad() throws IOException {
        MutableContentClassificationMap map = new MutableContentClassificationMapImpl("1.0.0");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        map.put("/sometype/someotherchild", ContentClassification.INTERNAL, null);
        map.put("/sometypewitha,comma",  ContentClassification.INTERNAL, "This is a \"Test\" with a , and an additional line\nnew line");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            map.write(output);
            try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                // now reload
                ContentClassificationMap map2 = new ContentClassificationMapImpl(input, "name");
                assertEquals(map, map2);
            }
        }
    }
}
