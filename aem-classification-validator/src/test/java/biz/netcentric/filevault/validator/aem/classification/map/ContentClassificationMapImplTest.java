package biz.netcentric.filevault.validator.aem.classification.map;

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
import java.util.regex.Pattern;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.ContentClassificationMap;
import biz.netcentric.filevault.validator.aem.classification.MutableContentClassificationMap;
import biz.netcentric.filevault.validator.aem.classification.map.ContentClassificationMapImpl;
import biz.netcentric.filevault.validator.aem.classification.map.MutableContentClassificationMapImpl;

public class ContentClassificationMapImplTest {

    @Test
    public void testValidMap() throws IOException {
        try (InputStream input = ContentClassificationMapImplTest.class.getResourceAsStream("/valid-classification.map")) {
            ContentClassificationMap map = new ContentClassificationMapImpl(input, "valid-classification.map");
            MutableContentClassificationMap expectedMap = new MutableContentClassificationMapImpl("Simple");
            expectedMap.put("/test", ContentClassification.INTERNAL_DEPRECATED, "Deprecated");
            Assertions.assertEquals(expectedMap, map);
        }
    }

    @Test
    public void testGetContentClassificationForResourceType() {
        ContentClassificationMapImpl map = new ContentClassificationMapImpl("1.0");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        map.put("/sometype/someotherchild", ContentClassification.ABSTRACT, "test");
        map.put("/libs/sometype", ContentClassification.FINAL, null);
        map.put("/libs/whitelisted", ContentClassification.INTERNAL, "internal");
        map.put("/", ContentClassification.PUBLIC, null);
        Collection<Pattern> whitelistedResourceType = Collections.singleton(Pattern.compile("/libs/whitelisted"));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.FINAL, "someremark"), map.getContentClassificationAndRemarkForResourcePath("/sometype", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.INTERNAL_CHILD, "someremark"), map.getContentClassificationAndRemarkForResourcePath("/sometype/somechild", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.ABSTRACT, "test"), map.getContentClassificationAndRemarkForResourcePath("/sometype/someotherchild", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.FINAL, null), map.getContentClassificationAndRemarkForResourcePath("sometype", whitelistedResourceType)); // "/libs" is implicitly prepended
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourcePath("whitelisted", whitelistedResourceType)); // whitelisted resource type
        // make sure that whitelisting only affects the given resource type but no children (really?)
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.INTERNAL, "internal"), map.getContentClassificationAndRemarkForResourcePath("whitelisted/child", whitelistedResourceType));
        assertEquals(new SimpleEntry<ContentClassification, String>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourcePath("/", whitelistedResourceType));
    }

    @Test
    public void testGetContentClassificationForResourceTypeWithoutClassification() {
        ContentClassificationMapImpl map = new ContentClassificationMapImpl("somelabel");
        Assertions.assertThrows(IllegalStateException.class,() -> { map.getContentClassificationAndRemarkForResourcePath("/sometype", null); });
    }

    
    @Test
    public void testGetContentClassificationForInvalidResourceType() {
        ContentClassificationMapImpl map = new ContentClassificationMapImpl("somelabel");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        Assertions.assertThrows(IllegalStateException.class,() -> { map.getContentClassificationAndRemarkForResourcePath("/sometype/", null); });
    }

    @Test
    public void testGetContentClassificationForEmptyResourceType() {
        ContentClassificationMapImpl map = new ContentClassificationMapImpl("somelabel");
        map.put("/sometype", ContentClassification.FINAL, "someremark");
        Assertions.assertEquals(new SimpleEntry<>(ContentClassification.PUBLIC, null), map.getContentClassificationAndRemarkForResourcePath("", null));
    }

    @Test
    public void testPutWithARelativeResourceType() {
        ContentClassificationMapImpl map = new ContentClassificationMapImpl("somelabel");
        Assertions.assertThrows(IllegalArgumentException.class,() -> { map.put("relativeresourcetype", ContentClassification.PUBLIC, null); });
    }

    @Test
    public void testInvalidMap() throws IOException {
        try (InputStream input = ContentClassificationMapImplTest.class.getResourceAsStream("/invalid-classification.map")) {
            Assertions.assertThrows(IllegalArgumentException.class,() -> { new ContentClassificationMapImpl(input, "invalid-classification.map"); });
        }
    }

}
