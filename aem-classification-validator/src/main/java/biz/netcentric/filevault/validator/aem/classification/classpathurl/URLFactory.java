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

import java.net.MalformedURLException;
import java.net.URL;

public class URLFactory {
    public static final String TCCL_PROTOCOL_PREFIX = "tccl:";
    
    private URLFactory() {
        
    }

    public static URL createURL(String spec) throws MalformedURLException {
        final URL url;
        // which URLHandler to take
        if (spec.startsWith(TCCL_PROTOCOL_PREFIX)) {
            // use custom UrlStreamHandler
            url = new URL(null, spec, new ThreadContextClassLoaderURLStreamHandler());
        } else {
            // use default UrlStreamHandler
            url = new URL(spec);
        }
        return url;
    }
}
