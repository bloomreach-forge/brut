package org.bloomreach.forge.brut.resources.bootstrap;

import org.junit.jupiter.api.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualBootstrapStrategyTest {

    @Test
    void testCanHandleAlwaysReturnsTrue() {
        ManualBootstrapStrategy strategy = new ManualBootstrapStrategy();
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Thread.currentThread().getContextClassLoader()
        );

        assertTrue(strategy.canHandle(context));
    }

    @Test
    void testInitializeHstStructureDoesNotThrow() throws RepositoryException {
        ManualBootstrapStrategy strategy = new ManualBootstrapStrategy();
        Session mockSession = mock(Session.class);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Thread.currentThread().getContextClassLoader()
        );

        // Should not throw - just logs and returns
        assertDoesNotThrow(() ->
            strategy.initializeHstStructure(mockSession, "myproject", context)
        );

        // Should not interact with session (minimal setup)
        verifyNoInteractions(mockSession);
    }

    @Test
    void testWithHcmConfigPatternsLogsWarning() throws RepositoryException {
        ManualBootstrapStrategy strategy = new ManualBootstrapStrategy();
        Session mockSession = mock(Session.class);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.singletonList("/hcm-config/**/*.yaml"),  // HCM patterns provided
            Thread.currentThread().getContextClassLoader()
        );

        // Should not throw even with HCM patterns (just warns)
        assertDoesNotThrow(() ->
            strategy.initializeHstStructure(mockSession, "myproject", context)
        );
    }

    @Test
    void testWithNullProjectNamespace() throws RepositoryException {
        ManualBootstrapStrategy strategy = new ManualBootstrapStrategy();
        Session mockSession = mock(Session.class);
        BootstrapContext context = new BootstrapContext(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Thread.currentThread().getContextClassLoader()
        );

        // Should handle null project namespace gracefully
        assertDoesNotThrow(() ->
            strategy.initializeHstStructure(mockSession, null, context)
        );
    }
}
