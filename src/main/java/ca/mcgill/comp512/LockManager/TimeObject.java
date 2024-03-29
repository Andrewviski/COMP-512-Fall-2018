package ca.mcgill.comp512.LockManager;

import java.util.Date;

public class TimeObject extends TransactionObject
{
	private Date m_date = new Date();

	// The data members inherited are
	// LockManager.TransactionObject:: private int m_xid;

	TimeObject()
	{
		super();
	}

	TimeObject(int xid)
	{
		super(xid);
	}

	public long getTime()
	{
		return m_date.getTime();
	}
}
