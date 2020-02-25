package biz.netcentric.filevault.validator.aem.classification.classpathurl;

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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class URLFactoryTest {

    @Test
    public void testTcclUrl() throws MalformedURLException, IOException {
        try (InputStream input = URLFactory.createURL("tccl:test.file").openStream()) {
            Assertions.assertEquals("test", IOUtils.toString(input, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testFileUrl() throws MalformedURLException, IOException {
        Path tmpPath;
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("test.file")) {
            // copy to tmp file
            tmpPath = Files.createTempFile(null, null);
            Files.copy(input, tmpPath, StandardCopyOption.REPLACE_EXISTING);
            
        }
        try (InputStream input = URLFactory.createURL(tmpPath.toUri().toURL().toString()).openStream()) {
            Assertions.assertEquals("test", IOUtils.toString(input, StandardCharsets.UTF_8));
        } finally {
            Files.delete(tmpPath);
        }
    }
}
