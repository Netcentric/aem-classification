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
import java.net.URL;
import java.net.URLConnection;

public class ClassLoaderUrlConnection extends URLConnection {
    private final ClassLoader classLoader;

    protected ClassLoaderUrlConnection(ClassLoader classLoader, URL url) {
        super(url);
        this.classLoader = classLoader;
    }

    
    @Override
    public void connect() throws IOException {
        
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream input = classLoader.getResourceAsStream(url.getFile());
        if (input == null) {
            throw new IOException("Could not load resource '" + url.getFile() + "' from classLoader '" + classLoader + "'");
        }
        return input;
    }
}
