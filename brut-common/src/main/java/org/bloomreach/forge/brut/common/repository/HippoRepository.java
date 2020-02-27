package org.bloomreach.forge.brut.common.repository;

import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;

import javax.jcr.RepositoryException;

class HippoRepository extends RepositoryImpl {
    HippoRepository(RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
    }
}
