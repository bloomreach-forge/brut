package org.bloomreach.forge.brut.resources.bootstrap;

import org.onehippo.cm.engine.ConfigurationConfigService;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Bridge for invoking package-private methods in ConfigurationConfigService via reflection.
 * <p>
 * brXM's ConfigurationConfigService has several methods that are package-private but
 * are needed for test environment bootstrap. This class encapsulates the reflection
 * logic needed to access these methods.
 * <p>
 * <strong>Methods accessed via reflection:</strong>
 * <ul>
 *   <li>applyNamespacesAndNodeTypes(ConfigurationModel, ConfigurationModel, Session)</li>
 *   <li>computeAndWriteDelta(ConfigurationModel, ConfigurationModel, Session, boolean)</li>
 * </ul>
 *
 * @since 5.2.0
 */
public final class ConfigServiceReflectionBridge {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceReflectionBridge.class);
    private static final int MAX_ITEM_EXISTS_RETRIES = 10;

    private ConfigServiceReflectionBridge() {
        // Utility class
    }

    /**
     * Invokes package-private applyNamespacesAndNodeTypes method via reflection.
     *
     * @param configService the ConfigurationConfigService instance
     * @param baseline      the baseline configuration model
     * @param update        the update configuration model
     * @param session       the JCR session
     * @throws Exception if invocation fails
     */
    public static void invokeApplyNamespacesAndNodeTypes(ConfigurationConfigService configService,
                                                         ConfigurationModelImpl baseline,
                                                         ConfigurationModelImpl update,
                                                         Session session) throws Exception {
        try {
            Method method = ConfigurationConfigService.class.getDeclaredMethod(
                "applyNamespacesAndNodeTypes",
                org.onehippo.cm.model.ConfigurationModel.class,
                org.onehippo.cm.model.ConfigurationModel.class,
                Session.class
            );
            method.setAccessible(true);
            method.invoke(configService, baseline, update, session);
            method.setAccessible(false);

            LOG.debug("Successfully invoked applyNamespacesAndNodeTypes via reflection");
        } catch (Exception e) {
            LOG.error("Failed to invoke applyNamespacesAndNodeTypes via reflection", e);
            throw new RepositoryException("Reflection invocation failed for applyNamespacesAndNodeTypes", e);
        }
    }

    /**
     * Invokes package-private computeAndWriteDelta method via reflection.
     *
     * @param configService the ConfigurationConfigService instance
     * @param baseline      the baseline configuration model
     * @param update        the update configuration model
     * @param session       the JCR session
     * @param forceApply    whether to force apply changes
     * @throws Exception if invocation fails
     */
    public static void invokeComputeAndWriteDelta(ConfigurationConfigService configService,
                                                  ConfigurationModelImpl baseline,
                                                  ConfigurationModelImpl update,
                                                  Session session,
                                                  boolean forceApply) throws Exception {
        try {
            Method method = ConfigurationConfigService.class.getDeclaredMethod(
                "computeAndWriteDelta",
                org.onehippo.cm.model.ConfigurationModel.class,
                org.onehippo.cm.model.ConfigurationModel.class,
                Session.class,
                boolean.class
            );
            method.setAccessible(true);
            method.invoke(configService, baseline, update, session, forceApply);
            method.setAccessible(false);

            LOG.debug("Successfully invoked computeAndWriteDelta via reflection");
        } catch (Exception e) {
            LOG.error("Failed to invoke computeAndWriteDelta via reflection", e);
            throw new RepositoryException("Reflection invocation failed for computeAndWriteDelta", e);
        }
    }

    /**
     * Applies configuration delta with automatic retry on ItemExistsException.
     * <p>
     * When ConfigService encounters a node that already exists, this method
     * removes the conflicting node and retries the operation.
     *
     * @param configService the ConfigurationConfigService instance
     * @param baseline      the baseline configuration model
     * @param update        the update configuration model
     * @param session       the JCR session
     * @throws RepositoryException if the operation fails after all retries
     */
    public static void applyConfigDeltaWithRetries(ConfigurationConfigService configService,
                                                   ConfigurationModelImpl baseline,
                                                   ConfigurationModelImpl update,
                                                   Session session) throws RepositoryException {
        Set<String> removedPaths = new HashSet<>();
        int attempts = 0;
        while (true) {
            try {
                invokeComputeAndWriteDelta(configService, baseline, update, session, true);
                return;
            } catch (RepositoryException e) {
                String existingPath = extractItemExistsPath(e);
                if (existingPath == null || removedPaths.contains(existingPath) || attempts >= MAX_ITEM_EXISTS_RETRIES) {
                    throw e;
                }
                session.refresh(false);
                if (!removeExistingNode(session, existingPath)) {
                    throw e;
                }
                removedPaths.add(existingPath);
                attempts++;
                LOG.info("Removed existing node {} after ItemExistsException; retrying ConfigService delta.", existingPath);
            } catch (Exception e) {
                throw new RepositoryException("Failed to compute configuration delta", e);
            }
        }
    }

    /**
     * Extracts the path from an ItemExistsException message.
     *
     * @param error the exception to extract from
     * @return the path, or null if not found
     */
    public static String extractItemExistsPath(Throwable error) {
        ItemExistsException itemExists = findItemExistsException(error);
        if (itemExists == null) {
            return null;
        }
        String message = itemExists.getMessage();
        if (message == null) {
            return null;
        }
        String marker = "exists:";
        int start = message.indexOf(marker);
        if (start < 0) {
            return null;
        }
        String path = message.substring(start + marker.length()).trim();
        return path.isEmpty() ? null : path;
    }

    /**
     * Finds an ItemExistsException in the exception chain.
     *
     * @param error the exception to search
     * @return the ItemExistsException, or null if not found
     */
    public static ItemExistsException findItemExistsException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ItemExistsException) {
                return (ItemExistsException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Removes an existing node at the given path.
     *
     * @param session the JCR session
     * @param path    the path to remove
     * @return true if the node was removed, false otherwise
     * @throws RepositoryException if operation fails
     */
    public static boolean removeExistingNode(Session session, String path) throws RepositoryException {
        if (path == null || !path.startsWith("/") || !session.nodeExists(path)) {
            return false;
        }
        Node node = session.getNode(path);
        if (node.getDepth() == 0) {
            return false;
        }
        node.remove();
        return true;
    }
}
