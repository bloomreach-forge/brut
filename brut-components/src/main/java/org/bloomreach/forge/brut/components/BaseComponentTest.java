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

    /**
     * Injects an externally-created (e.g. shared) repository before {@link #setup()} is called.
     * When a repository is pre-injected, {@code setup()} skips the {@link BrxmTestingRepository}
     * constructor and reuses the provided instance instead.
     *
     * @param repo the repository to use; must not be null
     */
    public void setRepository(BrxmTestingRepository repo) {
        this.repository = repo;
    }

    @Override
    public void setup() {
        if (repository == null) {
            try {
                repository = new BrxmTestingRepository();
            } catch (Exception e) {
                throw new SetupTeardownException(e);
            }
        }
        if (rootNode == null) {
            try {
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

    /**
     * Delegates to {@link org.bloomreach.forge.brut.common.repository.BrxmTestingRepository#recordInitialization}
     * so that base node-type registration runs exactly once per shared repository, skipping the
     * ~50 redundant {@code hasNodeType()} calls on every subsequent test class.
     */
    @Override
    protected boolean shouldRegisterBaseNodeTypes() {
        return repository.recordInitialization("__baseNodeTypes__");
    }

    /**
     * Delegates to {@link org.bloomreach.forge.brut.common.repository.BrxmTestingRepository#recordInitialization}
     * so that a shared (injected) repository is only bootstrapped once regardless of how many test
     * instances call {@code setup()}.
     */
    @Override
    protected boolean shouldImportNodeStructure() {
        return repository.recordInitialization(getPathToTestResource());
    }

    @Override
    public void teardown() {
        super.teardown();
        repository.close();
    }
}
