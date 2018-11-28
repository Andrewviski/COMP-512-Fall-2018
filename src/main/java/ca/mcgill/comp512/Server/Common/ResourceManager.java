// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package ca.mcgill.comp512.Server.Common;

import ca.mcgill.comp512.LockManager.DeadlockException;
import ca.mcgill.comp512.LockManager.LockManager;
import ca.mcgill.comp512.LockManager.TransactionAbortedException;
import ca.mcgill.comp512.LockManager.TransactionLockObject;
import ca.mcgill.comp512.Middleware.InvalidTransactionException;
import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

public class ResourceManager implements IResourceManager {
    private static final String storageKey = "StorageKey";
    private static final String trxStateLogFilename = "trxState.log";
    private static final int TIME_TO_LIVE_MS = 15000;
    protected static String name = "Server";
    ResourceManagerState state = new ResourceManagerState();
    private ResourceManagerCrashModes mode = ResourceManagerCrashModes.NONE;
    // 0: masterRecordFilename
    // 1: masterFilename
    // 2: shadowFilename
    // 3: logFilename
    private String[] filenames = {"", "", "", ""};
    private Boolean logAccess = true;

    public ResourceManager(String p_name) {
        name = p_name;
        state.lockManager = new LockManager();

        filenames[0] = name + "_masterRecord";
        filenames[1] = name + "_master";
        filenames[2] = name + "_shadow";
        filenames[3] = name + ".log";


        boolean allFilesExist = true;
        for (String filename : filenames) {
            allFilesExist &= new File(filename).exists();
        }

        // If we have all the required files then we can recover, otherwise we start fresh.
        if (allFilesExist) {
            System.err.println("Recovering from " + logFilename());
            recover();
        } else {
            for (String filename : filenames) {
                File file = new File(filename);
                if (file.exists())
                    file.delete();
                try {
                    System.err.println("Creating " + filename);
                    file.createNewFile();
                    file.deleteOnExit();
                } catch (Exception e) {
                    System.err.println("Failed to create " + filename + " terminating...");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            // write to masterRecord
            try {
                PrintWriter writer = new PrintWriter(masterRecordFilename());
                writer.println(masterFilename());
                writer.close();
            } catch (FileNotFoundException fnfe) {
                System.err.println(masterRecordFilename() + " is unexpectedly missing!");
                fnfe.printStackTrace();
            } catch (Exception e) {
                System.err.println("Failed to write to MasterRecord");
                e.printStackTrace();
            }

        }

        // Create a tx timeout thread
        new Thread(() -> {
            while (true) {
                try {
                    for (Integer xid : state.pendingXids) {
                        if (System.currentTimeMillis() - state.xidTimer.get(xid) > TIME_TO_LIVE_MS) {
                            abort(xid);
                            state.transactionStates.put(xid, "Abort");
                        }
                    }
                    Thread.sleep(1000);
                } catch (InvalidTransactionException e) {
                    System.err.println("Failed to abort transaction-" + e.getXId() + " in the timeout thread");
                    e.printStackTrace();

                } catch (Exception e) {
                    System.err.println("Failed to abort a transaction in the timeout thread");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopTimer(int xid) {
        System.out.println("Stopping the timer for " + xid);
        state.xidTimer.remove(xid);
    }

    private void resetTimer(int xid) {
        System.out.println("Reseting the timer for " + xid);
        state.xidTimer.put(xid, System.currentTimeMillis());
    }

    private String masterRecordFilename() {
        return filenames[0];
    }

    private String masterFilename() {
        return filenames[1];
    }

    private String shadowFilename() {
        return filenames[2];
    }

    private String logFilename() {
        return filenames[3];
    }

    private void Log(String logMessage) {
        synchronized (logAccess) {
            try {
                PrintWriter writer = new PrintWriter(logFilename());
                writer.println(logMessage);
                writer.close();
            } catch (FileNotFoundException fnfe) {
                System.err.println(logFilename() + " is unexpectedly missing!");
                fnfe.printStackTrace();
            } catch (Exception e) {
                System.err.println("Failed to log [ \"" + logMessage + "\" ] into " + logFilename());
                e.printStackTrace();
            }
        }
    }

    private void WriteToDisk(int transactionId) {
        Xlock(transactionId, storageKey);
        String latest = readMasterRecordFile();

        // Write the object
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream((latest.equals(masterFilename())) ? shadowFilename() : masterFilename()));
            objectOut.writeObject(state.m_data);
            objectOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // write to masterRecord
        try {
            PrintWriter writer = new PrintWriter(masterRecordFilename());
            writer.println((latest.equals(masterFilename())) ? shadowFilename() : masterFilename());
            writer.close();
        } catch (FileNotFoundException fnfe) {
            System.err.println(masterRecordFilename() + " is unexpectedly missing!");
            fnfe.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to write to MasterRecord");
            e.printStackTrace();
        }
    }

    private String readMasterRecordFile() {
        String latest = "";
        try {
            Scanner sc = new Scanner(new File(masterRecordFilename()));
            if (!sc.hasNext())
                throw new Exception();
            latest = sc.nextLine();
            sc.close();
        } catch (Exception e) {
            System.err.println("Cannot access data on masterRecord");
            e.printStackTrace();
        }
        return latest;
    }


    private boolean Xlock(int id, String name) throws DeadlockException {
        synchronized (state.lockManager) {
            return state.lockManager.Lock(id, name, TransactionLockObject.LockType.LOCK_WRITE);
        }
    }

    private boolean Slock(int id, String name) throws DeadlockException {
        synchronized (state.lockManager) {
            return state.lockManager.Lock(id, name, TransactionLockObject.LockType.LOCK_READ);
        }
    }

    // Reads a data item
    protected RMItem readData(int xid, String key) throws DeadlockException {
        if (!Slock(xid, key))
            return null;
        // Item has been removed
        synchronized (state.latestOp) {
            if (state.latestOp.containsKey(xid) && state.latestOp.get(xid).containsKey(key) && state.latestOp.get(xid).get(key) == 'd') {
                return null;
            }
        }

        // Get the updated version
        synchronized (state.writeSets) {
            if (!state.writeSets.containsKey(xid))
                state.writeSets.put(xid, new HashMap<>());
            HashMap<String, RMItem> writeSet = state.writeSets.get(xid);
            if (writeSet.containsKey(key))
                return writeSet.get(key);
        }

        // Get datastore's version.
        synchronized (state.m_data) {
            RMItem item = state.m_data.get(key);
            if (item != null) {
                return (RMItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    protected void flushData(String key, RMItem value) {
        synchronized (state.m_data) {
            state.m_data.put(key, value);
        }
    }

    protected void writeTempData(int xid, String key, RMItem value) throws DeadlockException {
        if (!Xlock(xid, key))
            return;
        synchronized (state.writeSets) {
            if (!state.writeSets.containsKey(xid))
                state.writeSets.put(xid, new HashMap<>());
            HashMap<String, RMItem> writeSet = state.writeSets.get(xid);
            writeSet.put(key, value);

            //update latest operation to be a write.
            if (!state.latestOp.containsKey(xid))
                state.latestOp.put(xid, new HashMap<>());
            state.latestOp.get(xid).put(key, 'w');
        }
    }

    // Remove the item out of storage
    protected void flushDataRemoval(String key) {
        synchronized (state.m_data) {
            state.m_data.remove(key);
        }
    }

    protected void removeTempData(int xid, String key) throws DeadlockException {
        if (!Xlock(xid, key))
            return;

        synchronized (state.writeSets) {
            if (state.writeSets.containsKey(xid)) {
                HashMap<String, RMItem> writeSet = state.writeSets.get(xid);
                writeSet.remove(key);

                //update latest operation to be a remove
                if (!state.latestOp.containsKey(xid))
                    state.latestOp.put(xid, new HashMap<>());
                state.latestOp.get(xid).put(key, 'd');
            }
        }
    }

    private void addPendingTransaction(int xid) {
        state.pendingXids.add(xid);
        if (state.xidTimer.containsKey(xid)) {
            resetTimer(xid);
        } else {
            state.xidTimer.put(xid, System.currentTimeMillis());
        }
    }

    // Deletes the encar item
    protected boolean deleteItem(int xid, String key) throws DeadlockException {
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeTempData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    protected int queryNum(int xid, String key) throws DeadlockException {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    protected int queryPrice(int xid, String key) throws DeadlockException {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // Reserve an item
    protected boolean reserveItem(int xid, int customerID, String key, String location) throws DeadlockException {
        Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        ReservableItem item = (ReservableItem) readData(xid, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        } else {
            customer.reserve(key, location, item.getPrice());
            writeTempData(xid, customer.getKey(), customer);

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeTempData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
        Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            // Doesn't exist yet, add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            writeTempData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        } else {
            // Add seats to existing flight and update the price if greater than zero
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            writeTempData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
        }
        return true;
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Car curObj = (Car) readData(xid, Car.getKey(location));
        if (curObj == null) {
            // Car location doesn't exist yet, add it
            Car newObj = new Car(location, count, price);
            writeTempData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing car location and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeTempData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Room curObj = (Room) readData(xid, Room.getKey(location));
        if (curObj == null) {
            // Room location doesn't exist yet, add it
            Room newObj = new Room(location, count, price);
            writeTempData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing object and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeTempData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return deleteItem(xid, Flight.getKey(flightNum));
    }

    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return deleteItem(xid, Car.getKey(location));
    }

    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return deleteItem(xid, Room.getKey(location));
    }

    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryNum(xid, Flight.getKey(flightNum));
    }

    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryNum(xid, Car.getKey(location));
    }

    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryNum(xid, Room.getKey(location));
    }

    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryPrice(xid, Flight.getKey(flightNum));
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryPrice(xid, Car.getKey(location));
    }

    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return queryPrice(xid, Room.getKey(location));
    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return "";
        } else {
            Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
            System.out.println(customer.getBill());
            return customer.getBill();
        }
    }

    public int newCustomer(int xid) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::newCustomer(" + xid + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        writeTempData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            customer = new Customer(customerID);
            writeTempData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
                ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount() + " times");
                item.setReserved(item.getReserved() - reserveditem.getCount());
                item.setCount(item.getCount() + reserveditem.getCount());
                writeTempData(xid, item.getKey(), item);
            }

            // Remove the customer from the storage
            removeTempData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return reserveItem(xid, customerID, Car.getKey(location), location);
    }

    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException {
        addPendingTransaction(xid);
        return reserveItem(xid, customerID, Room.getKey(location), location);
    }

    // Reserve bundle
    public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car,
                          boolean room) throws RemoteException, DeadlockException {
        return false;
    }

    /**
     * @return
     * @throws RemoteException
     */
    public int start() throws RemoteException {
        // This is handled in the middleware.
        return 0;
    }

    /**
     * @param transactionId
     * @return
     * @throws RemoteException
     * @throws TransactionAbortedException
     * @throws InvalidTransactionException
     */
    public synchronized boolean commit(int transactionId) throws RemoteException {
        crashIfModeIs(ResourceManagerCrashModes.AFTER_REC_DECISION);
        System.out.println("Commiting " + transactionId);
        synchronized (state.latestOp) {
            if (state.latestOp.containsKey(transactionId)) {
                for (HashMap.Entry<String, Character> entry : state.latestOp.get(transactionId).entrySet()) {
                    if (entry.getValue() == 'd')
                        flushDataRemoval(entry.getKey());
                    else {
                        synchronized (state.writeSets) {
                            flushData(entry.getKey(), state.writeSets.get(transactionId).get(entry.getKey()));
                        }
                    }
                }
            }
            // write to disk
            System.out.println("Writing " + transactionId + " to disk");
            WriteToDisk(transactionId);

            state.pendingXids.remove(transactionId);
            stopTimer(transactionId);
            state.lockManager.UnlockAll(transactionId);
            state.writeSets.remove(transactionId);
            state.latestOp.remove(transactionId);
        }
        state.transactionStates.put(transactionId, "Commit");
        return true;

    }

    /**
     * @param transactionId
     * @throws RemoteException
     * @throws InvalidTransactionException
     */
    public synchronized void abort(int transactionId) throws RemoteException,
            InvalidTransactionException {
        // TODO: akaba, this will create a bug where a non 2PC thing is going to call abort.
        crashIfModeIs(ResourceManagerCrashModes.AFTER_REC_DECISION);
        System.out.println("Aborting " + transactionId);
        state.pendingXids.remove(transactionId);
        stopTimer(transactionId);
        state.writeSets.remove(transactionId);
        state.lockManager.UnlockAll(transactionId);
        state.latestOp.remove(transactionId);
        state.transactionStates.put(transactionId, "Abort");
    }

    /**
     * @return
     * @throws RemoteException
     */
    public boolean shutdown() throws RemoteException {
//		File log = new File(logFilename);
//		File masterRecordFile = new File(masterRecordFilename);
//		File shadowFile = new File(shadowFilename);
//		File masterFile = new File(masterFilename);
//
//		if (log.exists()) {
//			log.delete();
//		} else {
//			System.err.println(logFilename + " is missing on shutdown!");
//			return false;
//		}
//
//		if (masterRecordFile.exists()) {
//			masterRecordFile.delete();
//		} else {
//			System.err.println(masterRecordFile + " is missing on shutdown!");
//			return false;
//		}
//
//		if (shadowFile.exists()) {
//			shadowFile.delete();
//		} else {
//			System.err.println(shadowFile + " is missing on shutdown!");
//			return false;
//		}
//
//		if (masterFile.exists()) {
//			masterFile.delete();
//		} else {
//			System.err.println(masterFile + " is missing on shutdown!");
//			return false;
//		}

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

    @Override
    public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // Not sure what th point of this? :/
        crashIfModeIs(ResourceManagerCrashModes.AFTER_REC_VOTE_REQ);
        boolean vote = true;
        if (!state.pendingXids.contains(xid)) {
            abort(xid);
            state.transactionStates.put(xid, "Abort");
            vote = false;
        }
        crashIfModeIs(ResourceManagerCrashModes.AFTER_SENDING_ANSWER);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                crashIfModeIs(ResourceManagerCrashModes.AFTER_SENDING_ANSWER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        if (vote) {

            state.transactionStates.put(xid, "Yes");
            stopTimer(xid);
        }
        return vote;
    }

    @Override
    public boolean resetCrashes() throws RemoteException {
        mode = ResourceManagerCrashModes.NONE;
        return true;
    }

    @Override
    public boolean crashMiddleware(TransactionManagerCrashModes mode) throws RemoteException {
        //I'm not a middlware fuck off
        throw new RuntimeException("crashMiddleware called on resourceManager");
    }

    @Override
    public boolean crashResourceManager(String name, ResourceManagerCrashModes mode) throws RemoteException {
        this.mode = mode;
        return true;
    }

    public String getName() throws RemoteException {
        return name;
    }

    private void recover() {
        try (ObjectInputStream transactionStatesFile =
                     new ObjectInputStream(new FileInputStream(trxStateLogFilename));
             ObjectInputStream dataStore =
                     new ObjectInputStream(new FileInputStream(readMasterRecordFile()));
        ) {

            state = (ResourceManagerState) transactionStatesFile.readObject();

            for (int xid : state.pendingXids) {
                if (state.transactionStates.containsKey(xid)) {
                    // Do nothing if you find a commit/abort record
                    if (state.transactionStates.get(xid).equals("Commit") || state.transactionStates.get(xid).equals("Abort")) {
                        continue;
                    }
                    // Waits indefinitely ¯\_(ツ)_/¯ according to Alex
                    else if (state.transactionStates.get(xid).equals("Yes")) {
                        continue;
                    } else {
                        // Technically in 2PC we need to send an abort vote to the coordinator, but ok because it will timeout and abort, right?
                        abort(xid);
                    }
                }
            }
        } catch (EOFException e) {
            System.err.println(readMasterRecordFile() + " is empty, nothing to recover");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveLog() {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(trxStateLogFilename))) {

            oos.writeObject(state);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void crashIfModeIs(ResourceManagerCrashModes mode) {
        if (this.mode == mode) {
            System.exit(1);
        }
    }

    private static class ResourceManagerState implements Serializable {
        private RMHashMap m_data = new RMHashMap();
        private Map<Integer, HashMap<String, RMItem>> writeSets = new HashMap<>();
        private Map<Integer, HashMap<String, Character>> latestOp = new HashMap<>();
        private Map<Integer, Long> xidTimer = new HashMap<>();
        private LockManager lockManager;
        private Set<Integer> pendingXids = new HashSet<>();
        private Map<Integer, String> transactionStates = new HashMap<>();
    }
}
