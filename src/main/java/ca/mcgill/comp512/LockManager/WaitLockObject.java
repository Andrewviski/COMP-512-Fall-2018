package ca.mcgill.comp512.LockManager;

public class WaitLockObject extends DataLockObject
{
	protected Thread m_thread = null;

	// The data members inherited are 
	// LockManager.TransactionObject:: protected int m_xid;
	// LockManager.TransactionLockObject:: protected String m_data;
	// LockManager.TransactionLockObject:: protected int m_lockType;

	WaitLockObject()
	{
		super();
		m_thread = null;
	}

	WaitLockObject(int xid, String data, LockType lockType)
	{
		super(xid, data, lockType);
		m_thread = null;
	}

	WaitLockObject(int xid, String data, LockType lockType, Thread thread)
	{
		super(xid, data, lockType);
		m_thread = thread;
	}

	public Thread getThread()
	{
		return m_thread;
	}
}
