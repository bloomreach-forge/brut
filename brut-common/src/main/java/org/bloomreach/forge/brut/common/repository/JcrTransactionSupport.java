package org.bloomreach.forge.brut.common.repository;

import org.hippoecm.repository.api.HippoSession;

import javax.jcr.Session;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class JcrTransactionSupport {

    private XAResource xa;
    private Xid xid;

    public void begin(Session session) throws Exception {
        xa = ((HippoSession) session).getXAResource();
        xid = SimpleXid.create();
        xa.start(xid, XAResource.TMNOFLAGS);
    }

    public void rollback() throws Exception {
        xa.end(xid, XAResource.TMSUCCESS);
        xa.rollback(xid);
        xa = null;
        xid = null;
    }

    public void commit() throws Exception {
        xa.end(xid, XAResource.TMSUCCESS);
        xa.commit(xid, true);
        xa = null;
        xid = null;
    }
}
