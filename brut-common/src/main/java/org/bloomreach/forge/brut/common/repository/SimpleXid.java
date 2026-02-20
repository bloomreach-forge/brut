package org.bloomreach.forge.brut.common.repository;

import javax.transaction.xa.Xid;
import java.nio.ByteBuffer;
import java.util.UUID;

public record SimpleXid(int formatId, byte[] globalTransactionId, byte[] branchQualifier) implements Xid {

    public static SimpleXid create() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return new SimpleXid(0x1234, buf.array(), new byte[0]);
    }

    @Override
    public int getFormatId() {
        return formatId;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    @Override
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }
}
