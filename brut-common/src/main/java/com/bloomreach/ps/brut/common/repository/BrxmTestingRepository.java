package com.bloomreach.ps.brut.common.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrxmTestingRepository implements Repository, AutoCloseable {

    private final RepositoryImpl originalRepository;
    private final Repository repository;
    private final File repositoryFolder;

    private static final Logger LOG = LoggerFactory.getLogger(BrxmTestingRepository.class);

    public BrxmTestingRepository() throws RepositoryException, IOException {
        InputStream configFile = BrxmTestingRepository.class.getClassLoader().getResourceAsStream(getRepositoryConfigFileLocation());
        this.repositoryFolder = Files.createTempDirectory("repository-").toFile();
        RepositoryConfig config = RepositoryConfig.create(configFile, this.repositoryFolder.getAbsolutePath());
        this.originalRepository = new HippoRepository(config);
        this.repository = new DecoratorFactoryImpl().getRepositoryDecorator(originalRepository);
    }

    protected String getRepositoryConfigFileLocation(){
        return "repository.xml";
    }

    public void shutdown() throws IOException {
        originalRepository.shutdown();
        FileUtils.deleteDirectory(repositoryFolder);
    }

    @Override
    public Session login(Credentials credentials, String workspaceName) throws RepositoryException {
        return repository.login(credentials, workspaceName);
    }

    @Override
    public String getDescriptor(String key) {
        return repository.getDescriptor(key);
    }

    @Override
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    @Override
    public Value getDescriptorValue(String key) {
        return repository.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return repository.getDescriptorValues(key);
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return repository.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return repository.isSingleValueDescriptor(key);
    }

    @Override
    public Session login() throws RepositoryException {
        return repository.login();
    }

    @Override
    public Session login(Credentials credentials) throws RepositoryException {
        return repository.login(credentials);
    }

    @Override
    public Session login(String workspace) throws RepositoryException {
        return repository.login(workspace);
    }

    @Override
    public void close() {
        try {
            shutdown();
        } catch (IOException e) {
            LOG.warn("Failed to removed temporary repository folder.", e);
        }
    }
}
