package org.bloomreach.forge.brut.resources.bootstrap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Strategy for initializing JCR repository with HST configuration structure.
 * <p>
 * Implementations provide different approaches to bootstrap the repository:
 * <ul>
 *   <li>{@link ManualBootstrapStrategy} - Manual node construction (legacy approach)</li>
 *   <li>{@link ConfigServiceBootstrapStrategy} - Uses brXM's ConfigurationConfigService (production approach)</li>
 * </ul>
 * <p>
 * The appropriate strategy is selected based on the environment:
 * <ul>
 *   <li>If hcm-module.yaml exists on classpath → ConfigServiceBootstrapStrategy</li>
 *   <li>Otherwise → ManualBootstrapStrategy (fallback)</li>
 * </ul>
 *
 * @since 5.2.0
 */
public interface JcrBootstrapStrategy {

    /**
     * Initializes JCR repository with HST configuration structure.
     *
     * @param session JCR session with admin privileges
     * @param projectNamespace project namespace (e.g., "myproject")
     * @param context bootstrap context containing configuration patterns and resources
     * @throws RepositoryException if initialization fails
     */
    void initializeHstStructure(Session session, String projectNamespace,
                                BootstrapContext context) throws RepositoryException;

    /**
     * Determines if this strategy can handle the current environment.
     * <p>
     * Used for auto-detection of the appropriate bootstrap strategy.
     *
     * @param context bootstrap context
     * @return true if this strategy can handle the environment, false otherwise
     */
    boolean canHandle(BootstrapContext context);
}
