package org.bloomreach.forge.brut.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class TestConfigurationLoggerTest {

    @Test
    void logConfiguration_logsWhenDebugEnabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logConfiguration(mockLog, TestConfigurationLoggerTest.class, "PageModel", log -> {
            log.debug("Custom config line");
        });

        verify(mockLog).debug(eq("{} test: {} - resolving configuration"), eq("PageModel"), eq("TestConfigurationLoggerTest"));
        verify(mockLog).debug("Custom config line");
    }

    @Test
    void logConfiguration_skipsWhenDebugDisabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(false);

        TestConfigurationLogger.logConfiguration(mockLog, TestConfigurationLoggerTest.class, "PageModel", log -> {
            log.debug("Should not be called");
        });

        verify(mockLog, never()).debug(anyString(), any(), any());
    }

    @Test
    void logConfiguration_handlesNullLogger() {
        // Should not throw
        TestConfigurationLogger.logConfiguration(null, TestConfigurationLoggerTest.class, "PageModel", log -> {});
    }

    @Test
    void logConfiguration_handlesNullTestClass() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logConfiguration(mockLog, null, "PageModel", log -> {});

        verify(mockLog).debug(eq("{} test: {} - resolving configuration"), eq("PageModel"), eq("Unknown"));
    }

    @Test
    void logSuccess_logsCompletionMessage() {
        Logger mockLog = mock(Logger.class);

        TestConfigurationLogger.logSuccess(mockLog, "PageModel", TestConfigurationLoggerTest.class);

        verify(mockLog).info(eq("{} test initialized: {}"), eq("PageModel"), eq("TestConfigurationLoggerTest"));
    }

    @Test
    void logSuccess_handlesNullLogger() {
        // Should not throw
        TestConfigurationLogger.logSuccess(null, "PageModel", TestConfigurationLoggerTest.class);
    }

    @Test
    void logFailure_logsErrorDetails() {
        Logger mockLog = mock(Logger.class);
        Exception error = new RuntimeException("Test error");

        TestConfigurationLogger.logFailure(mockLog, "PageModel", TestConfigurationLoggerTest.class, error);

        verify(mockLog).error(
            eq("{} test failed to initialize: {} - {}"),
            eq("PageModel"),
            eq("TestConfigurationLoggerTest"),
            eq("Test error")
        );
    }

    @Test
    void logFailure_handlesNullError() {
        Logger mockLog = mock(Logger.class);

        TestConfigurationLogger.logFailure(mockLog, "PageModel", TestConfigurationLoggerTest.class, null);

        verify(mockLog).error(
            eq("{} test failed to initialize: {} - {}"),
            eq("PageModel"),
            eq("TestConfigurationLoggerTest"),
            eq("Unknown error")
        );
    }

    @Test
    void logBeanPatterns_logsPatternsWhenDebugEnabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);
        List<String> patterns = Arrays.asList("pattern1", "pattern2");

        TestConfigurationLogger.logBeanPatterns(mockLog, patterns);

        verify(mockLog).debug(eq("Bean patterns: {}"), eq(patterns));
    }

    @Test
    void logBeanPatterns_warnsOnEmptyPatterns() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logBeanPatterns(mockLog, Collections.emptyList());

        verify(mockLog).warn("No bean patterns configured");
    }

    @Test
    void logBeanPatterns_warnsOnNullPatterns() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logBeanPatterns(mockLog, null);

        verify(mockLog).warn("No bean patterns configured");
    }

    @Test
    void logSpringConfigs_logsConfigsWhenDebugEnabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);
        List<String> configs = Arrays.asList("/config.xml");

        TestConfigurationLogger.logSpringConfigs(mockLog, configs);

        verify(mockLog).debug(eq("Spring configs: {}"), eq(configs));
    }

    @Test
    void logHstRoot_logsDefaultWhenEmpty() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logHstRoot(mockLog, "");

        verify(mockLog).debug(eq("HST root: {}"), eq("/hst:hst"));
    }

    @Test
    void logHstRoot_logsProvidedRoot() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logHstRoot(mockLog, "/hst:myproject");

        verify(mockLog).debug(eq("HST root: {}"), eq("/hst:myproject"));
    }

    @Test
    void logModules_logsModulesWhenDebugEnabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);
        List<String> modules = Arrays.asList("module1", "module2");

        TestConfigurationLogger.logModules(mockLog, "Addon Modules", modules);

        verify(mockLog).debug(eq("{}: {}"), eq("Addon Modules"), eq(modules));
    }

    @Test
    void logModules_skipsOnNullOrEmptyModules() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(true);

        TestConfigurationLogger.logModules(mockLog, "Test", null);
        TestConfigurationLogger.logModules(mockLog, "Test", Collections.emptyList());

        verify(mockLog, never()).debug(eq("{}: {}"), anyString(), anyList());
    }

    @Test
    void logModules_skipsWhenDebugDisabled() {
        Logger mockLog = mock(Logger.class);
        when(mockLog.isDebugEnabled()).thenReturn(false);
        List<String> modules = Arrays.asList("module1");

        TestConfigurationLogger.logModules(mockLog, "Test", modules);

        verify(mockLog, never()).debug(anyString(), any(), any());
    }
}
