package ca.mcgill.comp512.Middleware;

public class InvalidTransactionException extends RuntimeException {
    private int m_xid = 0;

    public InvalidTransactionException(int xid, String msg) {
        super("The transaction " + xid + " is invalid:" + msg+"\nPlease try with a different xid...\n");
        m_xid = xid;
    }

    public int getXId() {
        return m_xid;
    }
}

