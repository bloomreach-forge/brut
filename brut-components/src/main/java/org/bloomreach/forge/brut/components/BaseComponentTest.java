package org.bloomreach.forge.brut.components;

import org.bloomreach.forge.brut.common.repository.BrxmTestingRepository;
import org.bloomreach.forge.brut.components.exception.SetupTeardownException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public abstract class BaseComponentTest extends AbstractRepoTest {

    public static final SimpleCredentials ADMIN = new SimpleCredentials("admin", "admin".toCharArray());
    private BrxmTestingRepository repository;

    @Override
    public void setup() {
        if (repository == null) {
            try {
                repository = new BrxmTestingRepository();
                Session session = repository.login(ADMIN);
                rootNode = session.getRootNode();
            } catch (Exception e) {
                throw new SetupTeardownException(e);
            }
            componentManager.addComponent(Repository.class.getName(), repository);
            componentManager.addComponent(Credentials.class.getName() + ".hstconfigreader", getWritableCredentials());
            componentManager.addComponent(Credentials.class.getName() + ".default", getWritableCredentials());
            componentManager.addComponent(Credentials.class.getName() + ".binaries", getWritableCredentials());
            componentManager.addComponent(Credentials.class.getName() + ".preview", getWritableCredentials());
            componentManager.addComponent(Credentials.class.getName() + ".writable", getWritableCredentials());
        }
        super.setup();
    }

    protected BrxmTestingRepository getRepository() {
        return repository;
    }

    protected Credentials getWritableCredentials() {
        return ADMIN;
    }

    @Override
    public void teardown() {
        super.teardown();
        repository.close();
    }
}
