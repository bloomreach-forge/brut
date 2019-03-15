package com.bloomreach.ps.brxm.jcr.repository.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.onehippo.cm.engine.JcrContentProcessor;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.ps.brxm.jcr.repository.ImageResourceInputProvider;

public class ImporterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImporterUtils.class);

    private ImporterUtils() {
        //utility class
    }

    private static final Logger LOG = LoggerFactory.getLogger(ImporterUtils.class);

    private static final JcrContentProcessor PROCESSOR = new JcrContentProcessor();

    public static void importYaml(final URL resource, final Node parentNode, String path, String intermediateNodeType) throws RepositoryException {
        try {
            final ResourceInputProvider resourceInputProvider = new ImageResourceInputProvider(resource.getPath());
            Node node = createNode(parentNode, path, intermediateNodeType);
            final ModuleImpl module = new ModuleImpl("import-module", new ProjectImpl("import-project", new GroupImpl("import-group")));
            module.setContentResourceInputProvider(resourceInputProvider);
            final ContentSourceParser sourceParser = new ContentSourceParser(resourceInputProvider);
            sourceParser.parse(resource.openStream(), "/import", resource.getPath(), module);
            final ContentDefinitionImpl contentDefinition = module.getContentSources().iterator().next().getContentDefinition();
            PROCESSOR.importNode(contentDefinition.getNode(), node, ActionType.RELOAD);
        } catch (Exception e) {
            throw new RepositoryException("Import failed", e);
        }
    }

    public static void registerNamespaces(InputStream cndResource, Session session) throws RepositoryException {
        try {
            // Register the custom node types defined in the CND file, using JCR Commons CndImporter
            NodeType[] nodeTypes = CndImporter.registerNodeTypes(new InputStreamReader(cndResource), session);
            for (NodeType nodeType : nodeTypes) {
                LOGGER.info("registered: {}", nodeType.getName());
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    private static Node createNode(final Node rootNode, final String path, final String intermediateNodeType) {
        Node result = rootNode;
        if (path != null && !path.isEmpty()) {
            String[] nodes = PathUtils.normalizePath(path).split("/");
            for (String segment : nodes) {
                try {
                    if (result.hasNode(segment)) {
                        result = result.getNode(segment);
                    } else {
                        result = result.addNode(segment, intermediateNodeType);
                    }
                } catch (RepositoryException e) {
                    LOG.error("error while trying to create the node structure", e);
                }
            }
        }
        return result;
    }

}
