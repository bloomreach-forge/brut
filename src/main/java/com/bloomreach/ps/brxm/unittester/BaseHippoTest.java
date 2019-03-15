package com.bloomreach.ps.brxm.unittester;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import com.bloomreach.ps.brxm.jcr.repository.InMemoryJcrRepository;
import com.bloomreach.ps.brxm.unittester.exception.SetupTeardownException;

public abstract class BaseHippoTest extends AbstractRepoTest {

    public static final SimpleCredentials ADMIN = new SimpleCredentials("admin", "admin".toCharArray());
    private InMemoryJcrRepository repository;

    @Override
    public void setup() {
        try {
            repository = new InMemoryJcrRepository();
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
        super.setup();
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
