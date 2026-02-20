package org.bloomreach.forge.brut.common.repository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleXidTest {

    @Test
    void create_returnsDifferentXids() {
        SimpleXid xid1 = SimpleXid.create();
        SimpleXid xid2 = SimpleXid.create();
        assertFalse(Arrays.equals(xid1.getGlobalTransactionId(), xid2.getGlobalTransactionId()));
    }

    @Test
    void fields_satisfyXidContract() {
        SimpleXid xid = SimpleXid.create();
        assertNotNull(xid.getGlobalTransactionId());
        assertEquals(16, xid.getGlobalTransactionId().length);
        assertNotNull(xid.getBranchQualifier());
    }
}
