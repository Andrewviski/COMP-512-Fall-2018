package ca.mcgill.comp512.LockManager;
/* The transaction is deadlocked. Somebody should abort it. */

public class DeadlockException extends RuntimeException
{
	private int m_xid = 0;

	public DeadlockException(int xid, String msg)
	{
		super("The transaction " + xid + " is deadlocked:" + msg);
		m_xid = xid;
	}

	public int getXId()
	{
		return m_xid;
	}
}
