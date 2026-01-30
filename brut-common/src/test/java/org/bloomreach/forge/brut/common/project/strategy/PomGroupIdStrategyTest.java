package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PomGroupIdStrategyTest {

    private PomGroupIdStrategy strategy;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        strategy = new PomGroupIdStrategy();
    }

    @Test
    void priorityIs30() {
        assertEquals(30, strategy.getPriority());
    }

    @Test
    void nameReturnsClassName() {
        assertEquals("PomGroupIdStrategy", strategy.getName());
    }

    @Test
    void resolveReturnsEmptyWhenNoProjectRoot() {
        DiscoveryContext context = DiscoveryContext.builder()
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveReturnsEmptyWhenNoPomExists() {
        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveExtractsDirectGroupId() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example.myproject</groupId>
                    <artifactId>my-artifact</artifactId>
                    <version>1.0.0</version>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.myproject.beans", packages.get(0));
        assertEquals("com.example.myproject.model", packages.get(1));
    }

    @Test
    void resolveExtractsParentGroupIdWhenNoDirectGroupId() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>com.example.parent</groupId>
                        <artifactId>parent-artifact</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-artifact</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        List<String> packages = result.get();
        assertEquals(2, packages.size());
        assertEquals("com.example.parent.beans", packages.get(0));
        assertEquals("com.example.parent.model", packages.get(1));
    }

    @Test
    void resolvePrefersDirectGroupIdOverParent() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <parent>
                        <groupId>com.example.parent</groupId>
                        <artifactId>parent-artifact</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <groupId>com.example.child</groupId>
                    <artifactId>child-artifact</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        assertEquals("com.example.child.beans", result.get().get(0));
    }

    @Test
    void resolveRespectsModelFirstOrder() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>my-artifact</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.MODEL_FIRST)
                .includeDomain(false)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        assertEquals("com.example.model", result.get().get(0));
        assertEquals("com.example.beans", result.get().get(1));
    }

    @Test
    void resolveIncludesDomainWhenRequested() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>my-artifact</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .includeDomain(true)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());
        assertTrue(result.get().contains("com.example.domain"));
    }

    @Test
    void resolveSkipsPropertyReferences() throws IOException {
        String pomContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <groupId>${project.groupId}</groupId>
                    <artifactId>my-artifact</artifactId>
                </project>
                """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        DiscoveryContext context = DiscoveryContext.builder()
                .projectRoot(tempDir)
                .order(BeanPackageOrder.BEANS_FIRST)
                .build();

        Optional<List<String>> result = strategy.resolve(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseGroupIdReturnsNullForMalformedPom() throws IOException {
        String pomContent = "not valid xml";
        Path pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, pomContent);

        String groupId = strategy.parseGroupId(pomPath);

        assertNull(groupId);
    }
}
