package org.bloomreach.forge.brut.common.repository.utils;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.nodetype.NodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

public class NodeTypeUtils {
    private NodeTypeUtils() {
        // utility class
    }

    public static void createNodeType(Session session, String nodeType) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        if (!nodeTypeManager.hasNodeType(nodeType)) {
            registerNodeOrMixin(session, nodeType, "nt:unstructured", false);
        }
    }

    public static void createNodeType(Session session, String nodeType, String superType) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        createNodeType(session, superType);
        if (!nodeTypeManager.hasNodeType(nodeType)) {
            registerNodeOrMixin(session, nodeType, superType, false);
        }
    }

    public static void createMixin(Session session, String mixinType) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        if (!nodeTypeManager.hasNodeType(mixinType)) {
            registerNodeOrMixin(session, mixinType, "nt:unstructured", true);
        }
    }

    public static void createMixin(Session session, String mixinType, String superType) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        createMixin(session, superType);
        if (!nodeTypeManager.hasNodeType(mixinType)) {
            registerNodeOrMixin(session, mixinType, superType, true);
        }
    }

    public static String getOrRegisterNamespace(Session session, String name) throws RepositoryException {
        String uri = null;
        if (name.indexOf(':') > -1) {
            String prefix = name.substring(0, name.indexOf(':'));
            NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            try {
                uri = namespaceRegistry.getURI(prefix);
            } catch (RepositoryException e) {
                uri = "https://www.bloomreach.com/" + prefix + "/nt/1.0";
                namespaceRegistry.registerNamespace(prefix, uri);
            }
        }
        return uri;
    }


    private static void registerNodeOrMixin(Session session, String nodeType, String superType, boolean isMixin) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        String uri = getOrRegisterNamespace(session, nodeType);
        NameFactory nameFactory = NameFactoryImpl.getInstance();
        Name name = nameFactory.create(uri, getLocalName(nodeType));
        ValueFactory valueFactory = session.getValueFactory();
        Name[] supertypes;
        String namespace = getOrRegisterNamespace(session, superType);
        supertypes = new Name[]{nameFactory.create(namespace, getLocalName(superType))};
        QNodeTypeDefinitionImpl ntd = new QNodeTypeDefinitionImpl(name, supertypes, new Name[0], isMixin, false, true, true, null, new QPropertyDefinition[0], new QNodeDefinition[0]);
        NodeTypeDefinition nodeTypeDefinition = new NodeTypeDefinitionImpl(ntd, getNamePathResolver(session), valueFactory);
        nodeTypeManager.registerNodeType(nodeTypeDefinition, false);
    }

    private static NamePathResolver getNamePathResolver(Session session) {
        NamePathResolver result = null;
        Session realSession = ReflectionUtils.unwrapSessionDecorator(session);
        if (realSession instanceof NamePathResolver) {
            result = (NamePathResolver) realSession;
        }
        return result;
    }


    private static String getLocalName(String nodeType) {
        return nodeType.substring(nodeType.indexOf(':') + 1);
    }

}
