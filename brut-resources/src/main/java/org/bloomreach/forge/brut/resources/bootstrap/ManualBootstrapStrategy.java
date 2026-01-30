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
        LOG.debug("Manual bootstrap (minimal HST setup) for project: {}", projectNamespace);

        if (!context.getHcmConfigPatterns().isEmpty()) {
            LOG.warn("HCM config patterns provided but no hcm-module.yaml found - consider adding one");
        }

        LOG.info("Manual bootstrap completed (no HST JCR nodes created)");
    }

    @Override
    public boolean canHandle(BootstrapContext context) {
        return true;
    }
}
