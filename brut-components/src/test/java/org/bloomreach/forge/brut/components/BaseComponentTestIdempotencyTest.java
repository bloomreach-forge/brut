package org.bloomreach.forge.brut.components;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.junit.jupiter.api.Test;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BaseComponentTestIdempotencyTest {

    @Test
    void setup_calledTwice_reusesSameRepository() {
        var test = new TestableComponentTest();
        test.setup();
        BrxmTestingRepository first = test.getRepository();
        test.setup();
        BrxmTestingRepository second = test.getRepository();
        assertSame(first, second);
        first.forceClose();
    }

    @Test
    void teardown_whenManaged_doesNotCloseRepository() throws Exception {
        var test = new TestableComponentTest();
        test.setup();
        test.getRepository().setManaged(true);
        test.teardown();
        Session s = test.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(s);
        s.logout();
        test.getRepository().forceClose();
    }

    @Test
    void setup_withInjectedRepository_reusesIt() throws Exception {
        var donor = new TestableComponentTest();
        donor.setup();
        BrxmTestingRepository shared = donor.getRepository();
        shared.setManaged(true);

        var consumer = new TestableComponentTest();
        consumer.setRepository(shared);
        assertDoesNotThrow(consumer::setup, "setup() must succeed when repository is pre-injected");
        assertSame(shared, consumer.getRepository(), "consumer must use the injected repository");

        Session s = consumer.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(s);
        s.logout();
        shared.forceClose();
    }

    private static class TestableComponentTest extends BaseComponentTest {

        @Override
        protected String getAnnotatedClassesResourcePath() {
            return "classpath*:org/bloomreach/forge/brut/components/demo/domain/**/*.class";
        }

        @Override
        public BrxmTestingRepository getRepository() {
            return super.getRepository();
        }
    }
}
