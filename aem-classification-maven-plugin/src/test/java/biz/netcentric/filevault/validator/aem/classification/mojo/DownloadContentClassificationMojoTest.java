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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class DownloadContentClassificationMojoTest {

    @Test
    void testGetPathWithUnixSeparators() {
        Path path = Paths.get("my", "test", "path");
        assertEquals("my/test/path", DownloadContentClassificationMojo.getPathWithUnixSeparators(path));
    }

}
