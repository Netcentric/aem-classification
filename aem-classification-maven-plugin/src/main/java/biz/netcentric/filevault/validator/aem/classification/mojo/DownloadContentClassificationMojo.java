package biz.netcentric.filevault.validator.aem.classification.mojo;

/*-
 * #%L
 * AEM Classification Maven Plugin
 * %%
 * Copyright (C) 2020 Netcentric - A Cognizant Digital Business
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.felix.utils.json.JSONParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.ContentUsage;
import biz.netcentric.filevault.validator.aem.classification.MutableContentClassificationMap;
import biz.netcentric.filevault.validator.aem.classification.map.MutableContentClassificationMapImpl;

/**
 *  Downloads the classification data from a remote JCR (only works with AEM 6.4 or newer) via HTTP endpoints,
 *  serializes it into a map file and optionally wraps that within a JAR file.
 *  <p>
 *  That JAR file still needs to be manually uploaded to a Maven repository to leverage this classification map from the aem-classification-validator.
 *  <p>
 *  Uses the JCR search to find the current classification and also deprecation infos from properties {@code cq:deprecated} and {@code cq:deprecatedReason}.
 */
@Mojo(requiresProject=false, name = "download-content-classification")
public class DownloadContentClassificationMojo extends AbstractMojo {

    /**
     * the base URL where AEM is deployed
     */
    @Parameter(property="baseUrl", defaultValue="http://localhost:4502")
    URI baseUrl;
    
    /**
     * the user name to access the {@link baseUrl}
     */
    @Parameter(property="username", defaultValue = "admin")
    String username;
    
    /**
     * the password of the user to access the {@link baseUrl}
     */
    @Parameter(property="password", defaultValue = "admin")
    String password;

    /**
     * If the classification map should be wrapped in a JAR file this
     * needs to be set to the filepath the map should have within the JAR.
     */
    @Parameter(property="relativeFileNameInJar", required = false)
    Path relativeFileNameInJar;

    /** The path of the classification map file (and potentially wrapper jar) without extension. If not set it is written to the default temporary directory of the file system with a random file name. */
    @Parameter(property="outputFile", required = false)
    Path outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            Collection<String> products = getProducts(httpClient);
            
            log.warn("Make sure that the relevant search index definitions are deployed on AEM at " + baseUrl + ". Otherwise this goal will fail!");
            log.info("Start retrieving the classification and deprecation data from " + baseUrl);
            
            MutableContentClassificationMap map = new MutableContentClassificationMapImpl(products.stream().collect(Collectors.joining(", ")));
            // always make sure that the root node is PUBLIC (even though this might not be part of the classification map extracted from a repo)
            map.put("/", ContentClassification.PUBLIC, null);
            // 1. retrieve classifications from mixins and store in map
            for (ContentClassification classification : ContentClassification.values()) {
                if (classification.isLabelMixin() == false) {
                    continue;
                }
                retrieveClassificationForMixin(httpClient, classification, map);
            }
            
            // 2. update map with deprecation entries
            retrieveDeprecatedResourceTypes(httpClient, map);
            
            // 3. persist the map
            final Path classificationMapFile;
            if (outputFile == null) {
                classificationMapFile = Files.createTempFile("contentclassification", ".map");
            } else {
                classificationMapFile = outputFile.resolveSibling(outputFile.getFileName() + ".map");
            }
            try (OutputStream fileOutputStream = Files.newOutputStream(classificationMapFile)) {
                map.write(fileOutputStream);
            }
            log.info("Written classification map to " + classificationMapFile + " containing " + map.size() + " entries.");
            
            // 4. optionally wrap in a JAR
            if (relativeFileNameInJar != null) {
                Path jarFile = createJarWrapper(classificationMapFile, relativeFileNameInJar);
                log.info("Written wrapper jar to " + jarFile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException("Could not retrieve classification metadata: " + e.getMessage(), e);
        } catch(IOException|IllegalStateException e) {
            throw new MojoFailureException("Could not retrieve classification metadata: " + e.getMessage(), e);
        }
    }

    Collection<String> getProducts(HttpClient httpClient) throws IOException, InterruptedException {
        // http://localhost:4502/system/console/status-productinfo does not provide proper JSON, therefore parse TXT
        try (InputStream input = downloadFromAem(httpClient, "/system/console/status-productinfo.txt")) {
            return extractProductsFromProductInfo(input);
        }
    }

    static Collection<String> extractProductsFromProductInfo(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            Collection<String> products = new LinkedList<>();
            boolean isRelevantLine = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals("Installed Products")) {
                    // the rest is relevant
                    isRelevantLine = true;
                } else if(isRelevantLine){
                    String product = line.trim();
                    if (!product.isEmpty()) {
                        products.add(product);
                    }
                }
            }
            return products;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void retrieveClassificationForMixin(HttpClient httpClient, ContentClassification classification, MutableContentClassificationMap map) throws IOException, InterruptedException {
        // Uses the crxde search to find the current classification
        // (http://localhost:8080/crx/de/query.jsp?_dc=1536334082630&_charset_=utf-8&type=JCR_SQL2&stmt=SELECT%20*%20FROM%20%5Bgranite%3AInternalArea%5D%0A&showResults=true)
        // the index is crucial for that though (property index limited to properties jcr:primaryType and jcr:mixinTypes)
        // for AEM 6.4 we talk about roughly 300 entries
        String query = "SELECT * FROM [" + classification.getLabel() + "]";
        StringBuilder urlParameters = new StringBuilder();
        urlParameters.append("_dc=").append(new Date().getTime()).append("&_charset_=utf-8&type=JCR-SQL2&stmt=").append(URLEncoder.encode(query, "ASCII")).append("&showResults=true");
        try (InputStream input = downloadFromAem(httpClient, "/crx/de/query.jsp?" + urlParameters)) {
            JSONParser parser = new JSONParser(input);
            Map<String, Object> response = parser.getParsed();
            getLog().debug("Received JSON response " + response);
            /*
             JSON Format
            "results": [{ "path": "<resource type>" }],
            "total": 102,
            "success": true,
            "time": 27
            */
            // check status first
            if (!Boolean.TRUE.equals(response.get("success"))) {
                throw new IllegalStateException("JSON response did not indicate success");
            }
            Object results = response.get("results");
            if (!(results instanceof List)) {
                throw new IllegalStateException("JSON response did not have an array of results");
            }
            for (Map<String, Object> result : (List<Map<String, Object>>)results) {
                String resourceType = (String)result.get("path");
                if (resourceType != null) {
                    map.put(resourceType, classification, null);
                }
            }
            getLog().info("Retrieved " + ((List)results).size() + " entries for classification " +classification.getLabel());
        }
        
    }

    @SuppressWarnings("unchecked")
    void retrieveDeprecatedResourceTypes(HttpClient httpClient, MutableContentClassificationMap map) throws IOException, InterruptedException {
        // uses query builder api to retrieve all deprecation metadata
        String query = "1_property=cq:deprecated&1_property.operation=exists&p.limit=-1&p.hits=selective&p.properties=" + URLEncoder.encode("jcr:mixinTypes jcr:path cq:deprecated cq:deprecatedReason", "ASCII");
        EnumSet<ContentUsage> allContentUsages = EnumSet.allOf(ContentUsage.class);
        try (InputStream input = downloadFromAem(httpClient, "/bin/querybuilder.json?" + query)) {
            JSONParser parser = new JSONParser(input);
            Map<String, Object> response = parser.getParsed();
            getLog().debug("Received JSON response " + response);
            Object results = response.get("hits");
            if (!(results instanceof List)) {
                throw new IllegalStateException("JSON response did not have an array of hits");
            }
            for (Map<String, Object> result : (List<Map<String, Object>>)results) {
                String resourceType = (String)result.get("jcr:path");
                // override classification in case any usage is allowed!
                ContentClassification classification = map.getContentClassificationAndRemarkForResourcePath(resourceType, null).getKey();
                if (classification.isAllowed(allContentUsages)) {
                    classification = ContentClassification.INTERNAL_DEPRECATED_ANNOTATION;
                }
                // override classification in case this is still marked as public
                String deprecatedSince = (String) result.get("cq:deprecated");
                String deprecatedReason = (String) result.get("cq:deprecatedReason");
                String deprecationRemark = "Deprecated since " + deprecatedSince + ": " + deprecatedReason;
                map.put(resourceType, classification, deprecationRemark);
            }
            getLog().info("Retrieved " + ((List)results).size() + " entries for deprecations.");
        }
    }

    @SuppressWarnings("java:S2647") // basic auth is ok in this context
    private InputStream downloadFromAem(HttpClient httpClient, String path) throws IOException, InterruptedException {
        String credentials = username+":"+password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        URI uri = baseUrl.resolve(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                // preemptive-auth only natively supported once authentication in cache (after first successful request), https://stackoverflow.com/a/58612586
                .header("Authorization", "Basic "+encoded)
                .build();
        getLog().debug("Connecting to " + uri + "...");
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return response.body();
    }

    Path createJarWrapper(Path sourceFile, Path relativeFileNameInJar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        final Path jarFile;
        if (outputFile == null) {
            jarFile = Files.createTempFile("contentclassification", ".jar");
        } else {
            jarFile = outputFile.resolveSibling(outputFile.getFileName() + ".jar");
        }
        
        try (JarOutputStream target = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            JarEntry entry = new JarEntry(getPathWithUnixSeparators(relativeFileNameInJar));
            entry.setTime(Files.getLastModifiedTime(sourceFile).toMillis());
            target.putNextEntry(entry);
            try (InputStream input = Files.newInputStream(sourceFile)) {
                input.transferTo(target);
            }
            target.closeEntry();
        }
        return jarFile;
    }

    static String getPathWithUnixSeparators(Path path) {
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }
}
