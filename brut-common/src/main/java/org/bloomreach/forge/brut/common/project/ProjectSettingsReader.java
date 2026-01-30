package org.bloomreach.forge.brut.common.project;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.Optional;

public final class ProjectSettingsReader {

    private ProjectSettingsReader() {
    }

    public static Optional<ProjectSettings> read(Path settingsFile) {
        if (settingsFile == null) {
            return Optional.empty();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(settingsFile.toFile());

            return Optional.of(new ProjectSettings(
                getTagValue(doc, "hstRoot"),
                getTagValue(doc, "projectNamespace"),
                getTagValue(doc, "selectedBeansPackage"),
                getTagValue(doc, "selectedRestPackage"),
                getTagValue(doc, "selectedComponentsPackage"),
                getTagValue(doc, "selectedProjectPackage"),
                getTagValue(doc, "repositoryDataModule"),
                getTagValue(doc, "applicationSubModule"),
                getTagValue(doc, "developmentSubModule"),
                getTagValue(doc, "siteModule"),
                getTagValue(doc, "webfilesSubModule")
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String getTagValue(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
