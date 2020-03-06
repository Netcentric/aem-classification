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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContentClassificationMapImplTest {

    @Test
    public void testValidMap() throws IOException {
        try (InputStream input = ContentClassificationMapImplTest.class.getResourceAsStream("/valid-classification.map")) {
            ContentClassificationMapper map = new ContentClassificationMapperImpl(input, "valid-classification.map");
            ContentClassificationMapper expectedMap = new ContentClassificationMapperImpl("Simple");
            expectedMap.put("/test", ContentClassification.INTERNAL_DEPRECATED, "Deprecated");
            Assertions.assertEquals(expectedMap, map);
        }
    }
    

    @Test
    public void testPersistAndLoad() throws IOException {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("1.0.0");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        map.put("/sometype/someotherchild", ContentClassification.INTERNAL, null);
        map.put("/sometypewitha,comma",  ContentClassification.INTERNAL, "This is a \"Test\" with a , and an additional line\nnew line");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            map.write(output);
            try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
                // now reload
                ContentClassificationMapper map2 = new ContentClassificationMapperImpl(input, "name");
                assertEquals(map, map2);
            }
        }
    }

    @Test
    public void testGetContentClassificationForResourceType() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("1.0");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        map.put("/sometype/someotherchild", ContentClassification.ABSTRACT, "test");
        map.put("/libs/sometype", ContentClassification.FINAL, null);
        map.put("/libs/whitelisted", ContentClassification.INTERNAL, "internal");
        map.put("/", ContentClassification.PUBLIC, null);
        Collection<String> whitelistedResourceType = Collections.singleton("/libs/whitelisted");
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.FINAL, "someremark"), map.getContentClassificationAndRemarkForResourceType("/sometype", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.INTERNAL_CHILD, "someremark"), map.getContentClassificationAndRemarkForResourceType("/sometype/somechild", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.ABSTRACT, "test"), map.getContentClassificationAndRemarkForResourceType("/sometype/someotherchild", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.FINAL, null), map.getContentClassificationAndRemarkForResourceType("sometype", whitelistedResourceType)); // "/libs" is implicitly prepended
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourceType("whitelisted", whitelistedResourceType)); // whitelisted resource type
        // make sure that whitelisting only affects the given resource type but no children
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.INTERNAL, "internal"), map.getContentClassificationAndRemarkForResourceType("whitelisted/child", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourceType("/", whitelistedResourceType));
    }

    @Test
    public void testGetContentClassificationForResourceTypeWithoutClassification() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("somelabel");
        Assertions.assertThrows(IllegalStateException.class,() -> { map.getContentClassificationAndRemarkForResourceType("/sometype", null); });
    }

    
    @Test
    public void testGetContentClassificationForInvalidResourceType() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("somelabel");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        Assertions.assertThrows(IllegalStateException.class,() -> { map.getContentClassificationAndRemarkForResourceType("/sometype/", null); });
    }

    @Test
    public void testGetContentClassificationForEmptyResourceType() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("somelabel");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        Assertions.assertEquals(new SimpleEntry<>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourceType("", null));
    }

    @Test
    public void testPutWithARelativeResourceType() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("somelabel");
        Assertions.assertThrows(IllegalArgumentException.class,() -> { map.put("relativeresourcetype", ContentClassification.PUBLIC, null); });
    }

    @Test
    public void testInvalidMap() throws IOException {
        try (InputStream input = ContentClassificationMapImplTest.class.getResourceAsStream("/invalid-classification.map")) {
            Assertions.assertThrows(IllegalArgumentException.class,() -> { new ContentClassificationMapperImpl(input, "invalid-classification.map"); });
        }
    }

    @Test
    public void testMerge() {
        ContentClassificationMapper map = new ContentClassificationMapperImpl("base");
        map.put("/", ContentClassification.PUBLIC, null);
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        map.put("/sometype/someotherchild", ContentClassification.ABSTRACT, "test");
        map.put("/libs/sometype", ContentClassification.FINAL, null);
        map.put("/libs/whitelisted", ContentClassification.INTERNAL, "internal");
        map.put("/libs/overlaid/type/child", ContentClassification.FINAL, null);
        
        ContentClassificationMapperImpl map2 = new ContentClassificationMapperImpl("overlay");
        map2.put("/sometype", ContentClassification.INTERNAL, "overlay");
        map2.put("/libs/sometype", ContentClassification.ABSTRACT, "overlay");
        map2.put("/libs/whitelisted", ContentClassification.INTERNAL, "overlay");
        map2.put("/libs/overlaid/type", ContentClassification.INTERNAL, "overlay");
        map2.put("/libs/overlaid/type/a", ContentClassification.FINAL, "overlay");
        map2.put("/libs/overlaid/type/a/b", ContentClassification.FINAL, "overlay");
        map.merge(map2);
        
        ContentClassificationMapper expectedMergedMap = new ContentClassificationMapperImpl("base, overlay");
        expectedMergedMap.put("/", ContentClassification.PUBLIC, null);
        expectedMergedMap.put("/sometype", ContentClassification.INTERNAL, "overlay");
        expectedMergedMap.put("/libs/sometype", ContentClassification.FINAL, null);
        expectedMergedMap.put("/libs/whitelisted", ContentClassification.INTERNAL, "internal");
        expectedMergedMap.put("/libs/overlaid/type", ContentClassification.INTERNAL, "overlay");
        expectedMergedMap.put("/libs/overlaid/type/a", ContentClassification.FINAL, "overlay");
        expectedMergedMap.put("/libs/overlaid/type/a/b", ContentClassification.FINAL, "overlay");
        assertEquals(expectedMergedMap, map);
    }
}
