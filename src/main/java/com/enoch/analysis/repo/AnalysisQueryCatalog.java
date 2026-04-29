package com.enoch.analysis.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class AnalysisQueryCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisQueryCatalog.class);
    private static final String DEFAULT_LOCATION = "classpath:queries/analysis-queries.xml";

    private final Map<String, String> queriesById;

    public AnalysisQueryCatalog(
            @Value("${analysis.query.xml.location:" + DEFAULT_LOCATION + "}") String configuredLocation,
            ResourceLoader resourceLoader
    ) {
        this.queriesById = Collections.unmodifiableMap(loadQueries(configuredLocation, resourceLoader));
    }

    public String get(String queryId) {
        String query = queriesById.get(queryId);
        if (!StringUtils.hasText(query)) {
            throw new IllegalStateException("Missing query id in XML catalog: " + queryId);
        }
        return query;
    }

    private Map<String, String> loadQueries(String configuredLocation, ResourceLoader resourceLoader) {
        Resource resource = resolveResource(configuredLocation, resourceLoader);
        if (!resource.exists()) {
            throw new IllegalStateException("Query XML file not found at " + resource);
        }

        try (InputStream input = resource.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder().parse(input);
            NodeList queryNodes = document.getElementsByTagName("query");

            Map<String, String> loaded = new HashMap<>();
            for (int i = 0; i < queryNodes.getLength(); i++) {
                Element element = (Element) queryNodes.item(i);
                String id = element.getAttribute("id");
                String sql = element.getTextContent();
                if (!StringUtils.hasText(id) || !StringUtils.hasText(sql)) {
                    continue;
                }
                loaded.put(id.trim(), sql.trim());
            }

            if (loaded.isEmpty()) {
                throw new IllegalStateException("No queries found in XML catalog: " + resource);
            }

            LOGGER.info("Loaded {} analysis queries from {}", loaded.size(), resource);
            return loaded;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed loading analysis query XML from " + resource, ex);
        }
    }

    private Resource resolveResource(String configuredLocation, ResourceLoader resourceLoader) {
        String location = StringUtils.hasText(configuredLocation) ? configuredLocation.trim() : DEFAULT_LOCATION;
        Resource primary = resourceLoader.getResource(location);
        if (primary.exists()) {
            return primary;
        }
        Resource fallback = resourceLoader.getResource(DEFAULT_LOCATION);
        if (fallback.exists()) {
            LOGGER.warn("Configured query XML location '{}' not found. Falling back to '{}'.", location, DEFAULT_LOCATION);
            return fallback;
        }
        return primary;
    }
}
