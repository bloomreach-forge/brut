package org.bloomreach.forge.brut.common.repository;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.simple.SimpleAccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.hippoecm.repository.security.HippoAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

public class CustomAccessManager extends HippoAccessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAccessManager.class);

    private SimpleAccessManager delegatee = new SimpleAccessManager();

    @Override
    public void init(AMContext context) throws Exception {
        delegatee.init(context);
    }

    @Override
    public boolean isSystemSession() {
        return true;
    }

    @Override
    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
        delegatee.init(context, acProvider, wspAccessManager);
    }

    @Override
    public void close() {
        try {
            delegatee.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void checkPermission(Path absPath, int permissions) throws RepositoryException {
        delegatee.checkPermission(absPath, permissions);
    }

    @Override
    public void checkRepositoryPermission(int permissions) throws RepositoryException {
        delegatee.checkRepositoryPermission(permissions);
    }

    @Override
    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        return delegatee.isGranted(id, permissions);
    }

    @Override
    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        return delegatee.isGranted(absPath, permissions);
    }

    @Override
    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        return delegatee.isGranted(parentPath, childName, permissions);
    }

    @Override
    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        return delegatee.canRead(itemPath, itemId);
    }

    @Override
    public boolean canAccess(String workspaceName) throws RepositoryException {
        return delegatee.canAccess(workspaceName);
    }

    @Override
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws RepositoryException {
        return delegatee.hasPrivileges(absPath, privileges);
    }

    @Override
    public boolean hasPermission(String path, String actions) throws RepositoryException {
        return true;
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws RepositoryException {
        return delegatee.getEffectivePolicies(absPath);
    }

    @Override
    public Privilege[] getSupportedPrivileges(String absPath) throws RepositoryException {
        return delegatee.getSupportedPrivileges(absPath);
    }

    @Override
    public Privilege privilegeFromName(String privilegeName) {
        try {
            return delegatee.privilegeFromName(privilegeName);
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void updateReferenceFacetRules() {
        //noop
    }

    @Override
    public AccessControlPolicy[] getPolicies(String absPath) throws RepositoryException {
        return delegatee.getPolicies(absPath);
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws RepositoryException {
        return delegatee.getApplicablePolicies(absPath);
    }

    @Override
    public void setPolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        delegatee.setPolicy(absPath, policy);
    }

    @Override
    public void removePolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        delegatee.removePolicy(absPath, policy);
    }

    @Override
    public void stateCreated(ItemState created) {
        // Do nothing
    }

    @Override
    public void stateModified(ItemState modified) {
        // Do nothing
    }

    @Override
    public void stateDestroyed(ItemState destroyed) {
        // Do nothing
    }

    @Override
    public void stateDiscarded(ItemState discarded) {
        // Do nothing
    }
}
