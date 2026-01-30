package org.example;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery;
import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that bean packages are automatically detected from project-settings.xml.
 *
 * <p>The framework discovers from project-settings.xml:</p>
 * <ul>
 *   <li>selectedProjectPackage = org.example</li>
 *   <li>selectedBeansPackage = org.example.beans</li>
 * </ul>
 *
 * <p>This results in bean packages being auto-configured as:
 * org.example.beans, org.example.model, org.example.domain</p>
 *
 * <p>Note: Full integration test with @BrxmComponentTest is not included because
 * the demo project has duplicate @Node beans in beans/ and model/ packages with
 * the same jcrType, which causes a conflict when all packages are scanned.</p>
 */
public class ZeroConfigBeanPackagesTest {

    @Test
    @DisplayName("ProjectDiscovery auto-detects bean packages from project-settings.xml")
    void projectDiscoveryDetectsBeanPackages() {
        List<String> packages = ProjectDiscovery.resolveBeanPackages(
            Paths.get(System.getProperty("user.dir")),
            (String) null,  // no test package
            BeanPackageOrder.BEANS_FIRST,
            true  // include domain
        );

        // Should detect from project-settings.xml:
        // selectedBeansPackage = org.example.beans
        // selectedProjectPackage = org.example -> .model, .domain
        assertFalse(packages.isEmpty(), "Should detect packages from project-settings.xml");
        assertTrue(packages.contains("org.example.beans"),
            "Should detect org.example.beans from selectedBeansPackage");
        assertTrue(packages.contains("org.example.model"),
            "Should detect org.example.model from selectedProjectPackage");
        assertTrue(packages.contains("org.example.domain"),
            "Should detect org.example.domain from selectedProjectPackage");
    }

    @Test
    @DisplayName("Bean packages respect order preference")
    void beanPackagesRespectOrder() {
        List<String> beansFirst = ProjectDiscovery.resolveBeanPackages(
            Paths.get(System.getProperty("user.dir")),
            (String) null,
            BeanPackageOrder.BEANS_FIRST,
            false
        );

        List<String> modelFirst = ProjectDiscovery.resolveBeanPackages(
            Paths.get(System.getProperty("user.dir")),
            (String) null,
            BeanPackageOrder.MODEL_FIRST,
            false
        );

        // BEANS_FIRST: beans should come before model
        int beansIdxBF = beansFirst.indexOf("org.example.beans");
        int modelIdxBF = beansFirst.indexOf("org.example.model");
        assertTrue(beansIdxBF < modelIdxBF,
            "BEANS_FIRST should put beans before model");

        // MODEL_FIRST: model should come before beans
        int modelIdxMF = modelFirst.indexOf("org.example.model");
        int beansIdxMF = modelFirst.indexOf("org.example.beans");
        assertTrue(modelIdxMF < beansIdxMF,
            "MODEL_FIRST should put model before beans");
    }

    @Test
    @DisplayName("Domain package is optional")
    void domainPackageIsOptional() {
        List<String> withDomain = ProjectDiscovery.resolveBeanPackages(
            Paths.get(System.getProperty("user.dir")),
            (String) null,
            BeanPackageOrder.BEANS_FIRST,
            true
        );

        List<String> withoutDomain = ProjectDiscovery.resolveBeanPackages(
            Paths.get(System.getProperty("user.dir")),
            (String) null,
            BeanPackageOrder.BEANS_FIRST,
            false
        );

        assertTrue(withDomain.contains("org.example.domain"),
            "Should include domain when includeDomain=true");
        assertFalse(withoutDomain.contains("org.example.domain"),
            "Should exclude domain when includeDomain=false");
    }
}
