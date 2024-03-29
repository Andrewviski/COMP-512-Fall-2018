package ca.mcgill.comp512.LockManager;
/* The transaction requested a lock that it already had. */ 

public class RedundantLockRequestException extends RuntimeException
{
	protected int m_xid = 0;

	public RedundantLockRequestException(int xid, String msg)
	{
		super(msg);
		m_xid = xid;
	}

	public int getXId()
	{
		return m_xid;
	}
}
