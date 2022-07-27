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

import java.util.AbstractMap.SimpleEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompositeContentClassificationMapTest {

    private CompositeContentClassificationMap compositeMap;
    @BeforeEach
    public void setUp() {
        ContentClassificationMapImpl map1 = new ContentClassificationMapImpl("map1");
        map1.put("/sometype/child/restricted", ContentClassification.INTERNAL, "from map1"); // this is the strictest classification
        map1.put("/sometype/child", ContentClassification.FINAL, "from map1");
        ContentClassificationMapImpl map2 = new ContentClassificationMapImpl("map2");
        map2.put("/sometype", ContentClassification.INTERNAL, "from map2"); // this is the strictest classification
        compositeMap = new CompositeContentClassificationMap(map1, map2);
    }

    @Test
    public void testGetContentClassificationForResourceType() {
        assertEquals(new SimpleEntry<>(ContentClassification.INTERNAL, "from map2"), compositeMap.getContentClassificationAndRemarkForResourcePath("/sometype/child", null));
        assertEquals(new SimpleEntry<>(ContentClassification.INTERNAL, "from map1"), compositeMap.getContentClassificationAndRemarkForResourcePath("/sometype/child/restricted", null));
    }

    @Test
    public void testSize() {
        assertEquals(3, compositeMap.size());
    }

    @Test
    public void testLabel() {
        assertEquals("Composite Map of map1, map2", compositeMap.getLabel());
    }
}
