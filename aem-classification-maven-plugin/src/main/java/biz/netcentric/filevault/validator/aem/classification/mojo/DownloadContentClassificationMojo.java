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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.json.JSONParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import biz.netcentric.filevault.validator.aem.classification.ContentClassification;
import biz.netcentric.filevault.validator.aem.classification.ContentClassificationMapper;
import biz.netcentric.filevault.validator.aem.classification.ContentClassificationMapperImpl;
import biz.netcentric.filevault.validator.aem.classification.ContentUsage;

/**
 *  Downloads the classification data from a remote JCR repository (only works with AEM 6.4 or newer),
 *  serializes it into a map file and optionally wraps that within a JAR file.
 *  <p>
 *  That JAR file still needs to be manually uploaded to a Maven repository to leverage this classification map from the plugin.
 *  <p>
 *  Uses the JCR search to find the current classification and also deprecation infos from properties "cq:deprecated" and "cq:deprecatedReason"
 *  The search index needs to be setup for that though (property index limited to properties jcr:primaryType and jcr:mixinTypes for node types granite:FinalArea, granite:PublicArea, granite:InternalArea, granite:AbstractArea and another property index for properties cq:deprecated for any node type)
 */
@Mojo(requiresProject=false, name = "download-content-classification")
public class DownloadContentClassificationMojo extends AbstractMojo {

    /**
     * the base URL where AEM is deployed
     */
    @Parameter(property="baseUrl", defaultValue="http://localhost:4502")
    URL baseUrl;
    
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
    File relativeFileNameInJar;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        try {
            String aemVersion = getAemVersion();
            
            log.warn("Make sure that the relevant search index definitions are deployed on AEM at " + baseUrl + ". Otherwise this goal will fail!");
            log.info("Start retrieving the classification and deprecation data from " + baseUrl);
            
            ContentClassificationMapper map = new ContentClassificationMapperImpl("AEM " + aemVersion);
            // always make sure that the root node is PUBLIC (even though this might not be part of the classification map extracted from a repo)
            map.put("/", ContentClassification.PUBLIC, null);
            // 1. retrieve classifications from mixins and store in map
            for (ContentClassification classification : ContentClassification.values()) {
                if (classification.isLabelMixin() == false) {
                    continue;
                }
                retrieveClassificationForMixin(classification, map);
            }
            
            // 2. update map with deprecation entries
            retrieveDeprecatedResourceTypes(map);
            
            // 3. persist the map
            File outputFile = File.createTempFile("contentclassification", ".map");
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                map.write(fileOutputStream);
            }
            log.info("Written classification map to " + outputFile + " containing " + map.size() + " entries.");
            
            // 4. optionally wrap in a JAR
            if (relativeFileNameInJar != null) {
                File jarFile = createJarWrapper(outputFile, relativeFileNameInJar);
                log.info("Written wrapper jar to " + jarFile);
            }
        } catch(IOException|IllegalStateException e) {
            throw new MojoFailureException("Could not generate classification JAR:" + e.getMessage(), e);
        }
    }

    String getAemVersion() throws IOException {
        try (InputStream input = getHttpConnectionInputStream("/libs/granite/operations/content/systemoverview/export.json")) {
            JSONParser parser = new JSONParser(input);
            Map<String, Object> response = parser.getParsed();
            getLog().debug("Received JSON response " + response);
            
            Object results = response.get("Instance");
            if (!(results instanceof Map<?, ?>)) {
                throw new IllegalStateException("JSON response did not have an array of results");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> subResult = (Map<String, Object>)results;
            return (String) subResult.get("Adobe Experience Manager");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void retrieveClassificationForMixin(ContentClassification classification, ContentClassificationMapper map) throws IOException {
        // Uses the crxde search to find the current classification
        // (http://localhost:8080/crx/de/query.jsp?_dc=1536334082630&_charset_=utf-8&type=JCR_SQL2&stmt=SELECT%20*%20FROM%20%5Bgranite%3AInternalArea%5D%0A&showResults=true)
        // the index is crucial for that though (property index limited to properties jcr:primaryType and jcr:mixinTypes)
        // for AEM 6.4 we talk about roughly 300 entries
        String query = "SELECT * FROM [" + classification.getLabel() + "]";
        StringBuilder urlParameters = new StringBuilder();
        urlParameters.append("_dc=").append(new Date().getTime()).append("&_charset_=utf-8&type=JCR-SQL2&stmt=").append(URLEncoder.encode(query, "ASCII")).append("&showResults=true");
        try (InputStream input = getHttpConnectionInputStream( "/crx/de/query.jsp?" + urlParameters)) {
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
                if (StringUtils.isNotBlank(resourceType)) {
                    map.put(resourceType, classification, null);
                }
            }
            getLog().info("Retrieved " + ((List)results).size() + " entries for classification " +classification.getLabel());
        }
        
    }

    @SuppressWarnings("unchecked")
    void retrieveDeprecatedResourceTypes(ContentClassificationMapper map) throws IOException {
        // uses query builder api to retrieve all deprecation metadata
        String query = "1_property=cq:deprecated&1_property.operation=exists&p.limit=-1&p.hits=selective&p.properties=" + URLEncoder.encode("jcr:mixinTypes jcr:path cq:deprecated cq:deprecatedReason", "ASCII");
        EnumSet<ContentUsage> allContentUsages = EnumSet.allOf(ContentUsage.class);
        try (InputStream input = getHttpConnectionInputStream( "/bin/querybuilder.json?" + query)) {
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
    private InputStream getHttpConnectionInputStream(String path) throws IOException {
        URL url = new URL(baseUrl, path);
        getLog().debug("Connecting to " + url + "...");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String credentials = username+":"+password;
        // use basic auth
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));  //Java 8
        connection.setRequestProperty("Authorization", "Basic "+encoded);
        return connection.getInputStream();
    }

    File createJarWrapper(File sourceFile, File relativeFileNameInJar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        File outputFile = File.createTempFile("contentclassification", ".jar");
        try (JarOutputStream target = new JarOutputStream(new FileOutputStream(outputFile), manifest)) {
            // convert to forward slashes
            JarEntry entry = new JarEntry(FilenameUtils.separatorsToUnix(relativeFileNameInJar.getPath()));
            entry.setTime(sourceFile.lastModified());
            target.putNextEntry(entry);
            try (InputStream input = new FileInputStream(sourceFile)) {
                IOUtils.copy(input, target);
            }
            target.closeEntry();
        }
        return outputFile;
    }
}
