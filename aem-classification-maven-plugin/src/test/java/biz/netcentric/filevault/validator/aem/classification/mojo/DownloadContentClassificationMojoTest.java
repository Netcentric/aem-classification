package biz.netcentric.filevault.validator.aem.classification.mojo;

/*-
* #%L
* AEM Classification Maven Plugin
* %%
* Copyright (C) 2024 Cognizant Netcentric
* %%
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
* #L%
*/

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

class DownloadContentClassificationMojoTest {

    @Test
    void testGetPathWithUnixSeparators() {
        Path path = Paths.get("my", "test", "path");
        assertEquals("my/test/path", DownloadContentClassificationMojo.getPathWithUnixSeparators(path));
    }

    @Test
    void testProductsFromProductInfo() throws IOException {
        assertEquals(List.of("Adobe Experience Manager (2025.11.23482.20251120T200914Z)", "cif (2025.10.15.00)", "forms (2025.10.17.02)"),
           DownloadContentClassificationMojo.extractProductsFromProductInfo(DownloadContentClassificationMojo.class.getResourceAsStream("/productinfo.txt")));
   }
}
