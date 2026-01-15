package org.bloomreach.forge.brut.resources.bootstrap;

import org.onehippo.cm.engine.ConfigurationConfigService;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.parser.ModuleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Bootstrap strategy using brXM's ConfigurationConfigService.
 * <p>
 * This strategy uses the same configuration service that brXM uses in production,
 * ensuring test environments match production structure exactly. This eliminates
 * manual JCR node construction and reduces maintenance burden.
 * <p>
 * <strong>Note:</strong> This strategy uses reflection to access package-private methods
 * in ConfigurationConfigService. This is necessary because brXM does not expose these
 * methods publicly, but is consistent with BRUT's existing use of reflection elsewhere.
 * <p>
 * <strong>Requirements:</strong>
 * <ul>
 *   <li>hcm-module.yaml must exist on classpath (META-INF/hcm-module.yaml)</li>
 *   <li>Module descriptor must reference hcm-config and hcm-content sources</li>
 * </ul>
 * <p>
 * <strong>Benefits over manual construction:</strong>
 * <ul>
 *   <li>Automatic sync with brXM changes - no manual updates needed</li>
 *   <li>Uses production bootstrap flow - same as real CMS</li>
 *   <li>Eliminates custom YAML parsers - leverages brXM's parser</li>
 *   <li>Version resilient - brXM changes propagate automatically</li>
 * </ul>
 *
 * @see ManualBootstrapStrategy
 * @since 5.2.0
 */
public class ConfigServiceBootstrapStrategy implements JcrBootstrapStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceBootstrapStrategy.class);
    private static final String HCM_MODULE_DESCRIPTOR = "META-INF/hcm-module.yaml";

    @Override
    public void initializeHstStructure(Session session, String projectNamespace,
                                      BootstrapContext context) throws RepositoryException {
        LOG.info("========================================");
        LOG.info("Using ConfigurationConfigService bootstrap strategy");
        LOG.info("Project Namespace: {}", projectNamespace);
        LOG.info("========================================");

        try {
            // Load test modules EXPLICITLY by path (NO classpath scanning)
            // This avoids discovering framework modules that have dependency conflicts
            LOG.info("Loading test modules explicitly (no classpath scanning)...");
            ConfigurationModelImpl configModel = loadTestModulesExplicitly(context.getClassLoader());

            int moduleCount = 0;
            for (Object ignored : configModel.getModules()) {
                moduleCount++;
            }
            LOG.info("Successfully loaded {} test module(s) explicitly", moduleCount);

            // Create empty baseline (first boot scenario)
            LOG.debug("Creating empty baseline for first-boot scenario");
            ConfigurationModelImpl baseline;
            try (ConfigurationModelImpl emptyModel = new ConfigurationModelImpl()) {
                baseline = emptyModel.build();
            }

            // Apply configuration via ConfigService using reflection
            LOG.debug("Instantiating ConfigurationConfigService");
            ConfigurationConfigService configService = new ConfigurationConfigService();

            LOG.info("Applying namespaces and node types via reflection...");
            invokeApplyNamespacesAndNodeTypes(configService, baseline, configModel, session);

            LOG.info("Computing and writing configuration delta via reflection...");
            invokeComputeAndWriteDelta(configService, baseline, configModel, session, true);

            session.save();

            LOG.info("========================================");
            LOG.info("ConfigurationConfigService bootstrap completed successfully");
            LOG.info("HST configuration structure created via production code path");
            LOG.info("========================================");

        } catch (Exception e) {
            LOG.error("========================================");
            LOG.error("ConfigurationConfigService bootstrap FAILED");
            LOG.error("========================================", e);
            throw new RepositoryException("Failed to bootstrap using ConfigurationConfigService", e);
        }
    }

    /**
     * Invokes package-private applyNamespacesAndNodeTypes method via reflection.
     * <p>
     * Method signature: void applyNamespacesAndNodeTypes(ConfigurationModel baseline,
     *                                                     ConfigurationModel update,
     *                                                     Session session)
     */
    private void invokeApplyNamespacesAndNodeTypes(ConfigurationConfigService configService,
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
     * <p>
     * Method signature: void computeAndWriteDelta(ConfigurationModel baseline,
     *                                             ConfigurationModel update,
     *                                             Session session,
     *                                             boolean forceApply)
     */
    private void invokeComputeAndWriteDelta(ConfigurationConfigService configService,
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

    @Override
    public boolean canHandle(BootstrapContext context) {
        try {
            Enumeration<URL> resources = context.getClassLoader().getResources(HCM_MODULE_DESCRIPTOR);
            boolean found = resources.hasMoreElements();

            if (found) {
                LOG.debug("Found hcm-module.yaml on classpath - ConfigService strategy can handle");
            } else {
                LOG.debug("No hcm-module.yaml found on classpath - ConfigService strategy cannot handle");
            }

            return found;
        } catch (IOException e) {
            LOG.warn("Error checking for hcm-module.yaml on classpath", e);
            return false;
        }
    }

    /**
     * Loads test HCM modules EXPLICITLY by path, avoiding classpath scanning.
     * <p>
     * This is the correct approach for test environments because:
     * <ul>
     *   <li>No classpath scanning = no framework module discovery</li>
     *   <li>No dependency conflicts from framework modules</li>
     *   <li>Explicit control over what loads</li>
     *   <li>Still uses ConfigService for production-identical JCR structure</li>
     * </ul>
     * <p>
     * We find test modules in target/test-classes or build/resources/test directories
     * and load them directly using ModuleReader, then let ConfigService write them to JCR.
     *
     * @param classLoader classloader to find test resource directories
     * @return ConfigurationModel with only test modules loaded
     * @throws Exception if error loading modules
     */
    private ConfigurationModelImpl loadTestModulesExplicitly(ClassLoader classLoader) throws Exception {
        ConfigurationModelImpl model = new ConfigurationModelImpl();

        LOG.info("Finding test module directories (target/test-classes or build/resources/test)...");

        // Find test resource directories containing hcm-module.yaml
        List<Path> testModulePaths = new ArrayList<>();
        Enumeration<URL> resources = classLoader.getResources(HCM_MODULE_DESCRIPTOR);

        while (resources.hasMoreElements()) {
            URL moduleUrl = resources.nextElement();

            // Only process file URLs from test directories (not JARs)
            if (moduleUrl.getProtocol().equals("file") &&
                (moduleUrl.getPath().contains("/target/test-classes/") ||
                 moduleUrl.getPath().contains("/build/resources/test/"))) {

                // Get the resource root directory (parent of META-INF)
                Path moduleDescriptorPath = Paths.get(moduleUrl.toURI());
                Path resourceRoot = moduleDescriptorPath.getParent().getParent(); // Go up from META-INF/hcm-module.yaml

                testModulePaths.add(resourceRoot);
                LOG.info("✓ Found test module at: {}", resourceRoot);
            } else {
                LOG.debug("✗ Skipping framework/JAR module: {}", moduleUrl);
            }
        }

        if (testModulePaths.isEmpty()) {
            LOG.warn("No test modules found - returning empty model");
            return model.build();
        }

        LOG.info("Loading {} test module(s) explicitly using ModuleReader (no classpath scanning)...",
            testModulePaths.size());

        // Load each test module explicitly using ModuleReader
        ModuleReader moduleReader = new ModuleReader();
        for (Path modulePath : testModulePaths) {
            Path moduleDescriptor = modulePath.resolve(HCM_MODULE_DESCRIPTOR);

            if (Files.exists(moduleDescriptor)) {
                LOG.info("  Loading module from: {}", moduleDescriptor);

                // Use ModuleReader to load the module (no classpath scanning!)
                ModuleImpl module = moduleReader.read(moduleDescriptor, false).getModule();
                model.addModule(module);

                LOG.info("  ✓ Loaded module from: {}", moduleDescriptor.getParent().getParent());
            } else {
                LOG.warn("  Module descriptor not found: {}", moduleDescriptor);
            }
        }

        LOG.info("Building configuration model from {} test module(s)...", testModulePaths.size());
        ConfigurationModelImpl builtModel = model.build();

        LOG.info("✓ Successfully built model with test modules only (framework modules NOT scanned)");
        return builtModel;
    }
}
