package ca.mcgill.comp512.LockManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* HashTable class for the Lock Manager */

public class TPHashTable implements Serializable
{
	private static final int HASH_DEPTH = 8;

	private List<List<TransactionObject>> m_vector;
	private int m_tableSize;

	TPHashTable(int p_tableSize)
	{
		m_tableSize = p_tableSize;

		m_vector = new ArrayList<>(p_tableSize);
		for (int i = 0; i < p_tableSize; i++)
		{
			m_vector.add(new ArrayList<>(HASH_DEPTH));
		}
	}

	public int getSize()
	{
		return m_tableSize;
	}

	public synchronized void add(TransactionObject xobj)
	{
		if (xobj == null) return;

		List<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0){
			hashSlot = -hashSlot;
		}
		vectSlot = m_vector.get(hashSlot);
		vectSlot.add(xobj);
	}

	public synchronized List<TransactionObject> elements(TransactionObject xobj)
	{
		if (xobj == null) return (new ArrayList<>());

		List<TransactionObject> vectSlot; // hash slot
		List<TransactionObject> elemVect = new ArrayList<>(24); // return object

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.get(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = (size - 1); i >= 0; i--) {
			xobj2 = vectSlot.get(i);
			if (xobj.key() == xobj2.key()) {
				elemVect.add(xobj2);
			}
		}
		return elemVect;
	}

	public synchronized boolean contains(TransactionObject xobj)
	{
		if (xobj == null) return false;

		List<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.get(hashSlot);
		return vectSlot.contains(xobj);
	}

	public synchronized boolean remove(TransactionObject xobj)
	{
		if (xobj == null) return false;

		List<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.get(hashSlot);
		return vectSlot.remove(xobj);
	}

	public synchronized TransactionObject get(TransactionObject xobj)
	{
		if (xobj == null) return null;

		List<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.get(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = 0; i < size; i++) {
			xobj2 = (TransactionObject)vectSlot.get(i);
			if (xobj.equals(xobj2)) {
				return xobj2;
			}
		}
		return null;
	}

	private void printStatus(String msg, int hashSlot, TransactionObject xobj)
	{
		System.out.println(this.getClass() + "::" + msg + "(slot" + hashSlot + ")::" + xobj.toString());
	}

	public List<TransactionObject> allElements()
	{
		List<TransactionObject> vectSlot = null;
		TransactionObject xobj = null;
		List<TransactionObject> hashContents = new ArrayList<>(1024);

		for (int i = 0; i < m_tableSize; i++) { // walk down hashslots
			if (m_vector.size() > 0) { // contains elements?
				vectSlot = m_vector.get(i);

				for (int j = 0; j < vectSlot.size(); j++) { // walk down single hash slot, adding elements.
					xobj = vectSlot.get(j);
					hashContents.add(xobj);
				}
			}
			// else contributes nothing.
		}

		return hashContents;
	}

	public synchronized void removeAll(TransactionObject xobj)
	{
		if (xobj == null) return;

		List<TransactionObject> vectSlot;

		int hashSlot = (xobj.hashCode() % m_tableSize);
		if (hashSlot < 0) {
			hashSlot = -hashSlot;
		}

		vectSlot = m_vector.get(hashSlot);

		TransactionObject xobj2;
		int size = vectSlot.size();
		for (int i = (size - 1); i >= 0; i--) {
			xobj2 = vectSlot.get(i);
			if (xobj.key() == xobj2.key()) {
				vectSlot.remove(i);
			}
		}
	}
}
