package LockManager;

public class TransactionAbortedException extends RuntimeException
{
    private int m_xid = 0;

    public TransactionAbortedException(int xid, String msg)
    {
        super("The transaction " + xid + " is aborted:" + msg);
        m_xid = xid;
    }

    int getXId()
    {
        return m_xid;
    }
}
