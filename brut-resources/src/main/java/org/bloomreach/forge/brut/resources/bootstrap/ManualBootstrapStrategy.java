package org.bloomreach.forge.brut.resources.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Legacy bootstrap strategy that performs minimal HST initialization.
 * <p>
 * This strategy does not create JCR nodes for HST configuration. It relies on
 * Spring/HST container setup to provide HST functionality without persistent
 * JCR configuration nodes.
 * <p>
 * <strong>Used when:</strong>
 * <ul>
 *   <li>No hcm-module.yaml is present on classpath</li>
 *   <li>Tests use AbstractJaxrsTest/AbstractPageModelTest which don't require JCR HST nodes</li>
 *   <li>Backwards compatibility with existing BRUT tests is required</li>
 * </ul>
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 *   <li>No persistent HST configuration in JCR</li>
 *   <li>Cannot query HST configuration via JCR APIs</li>
 *   <li>Limited to tests that don't need actual HST nodes</li>
 * </ul>
 * <p>
 * For tests requiring actual HST JCR structure, use {@link ConfigServiceBootstrapStrategy}
 * by providing an hcm-module.yaml descriptor on the classpath.
 *
 * @see ConfigServiceBootstrapStrategy
 * @since 5.2.0
 */
public class ManualBootstrapStrategy implements JcrBootstrapStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ManualBootstrapStrategy.class);

    @Override
    public void initializeHstStructure(Session session, String projectNamespace,
                                      BootstrapContext context) throws RepositoryException {
        LOG.info("========================================");
        LOG.info("Using manual bootstrap strategy (minimal HST setup)");
        LOG.info("Project Namespace: {}", projectNamespace);
        LOG.info("========================================");

        LOG.debug("No HST JCR structure will be created");
        LOG.debug("Tests will rely on Spring/HST container setup without persistent JCR nodes");

        // Check if HCM config patterns were provided (legacy approach)
        if (!context.getHcmConfigPatterns().isEmpty()) {
            LOG.warn("HCM config patterns provided but manual strategy does not process them");
            LOG.warn("Consider adding hcm-module.yaml to use ConfigServiceBootstrapStrategy instead");
        }

        LOG.info("========================================");
        LOG.info("Manual bootstrap strategy completed");
        LOG.info("Note: No HST JCR nodes created (minimal setup)");
        LOG.info("========================================");
    }

    @Override
    public boolean canHandle(BootstrapContext context) {
        // Always returns true - this is the fallback strategy
        LOG.debug("Manual bootstrap strategy can always handle as fallback");
        return true;
    }
}
