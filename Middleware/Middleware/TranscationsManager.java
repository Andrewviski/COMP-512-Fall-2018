package Middleware;

import LockManager.*;
import Server.Interface.*;

import java.rmi.RemoteException;
import java.util.*;

public class TranscationsManager {
    private int new_xid=0;
    private final int TIME_TO_LIVE_MS = 100000;
    private RMIMiddleware ownerMiddleware;
    private LockManager lockManager = new LockManager();
    private static HashSet<Integer> aliveCustomerIds = new HashSet<>();
    private HashSet<Integer> pendingXids = new HashSet<Integer>();
    private HashMap<Integer,Boolean> updatesFlight = new HashMap<Integer, Boolean>();
    private HashMap<Integer,Boolean> updatesCar = new HashMap<Integer, Boolean>();
    private HashMap<Integer,Boolean> updatesRoom = new HashMap<Integer, Boolean>();
    private HashMap<Integer,Timer> xidTimer = new HashMap<Integer,Timer>();

    TranscationsManager(RMIMiddleware ownerMiddleware) {
        this.ownerMiddleware = ownerMiddleware;
    }

    // Resource managers accessors.
    public IResourceManager GetFlightsManager() {
        return ownerMiddleware.GetFlightsManager();
    }

    public IResourceManager GetRoomsManager() {
        return ownerMiddleware.GetRoomsManager();
    }

    public IResourceManager GetCarsManager() {
        return ownerMiddleware.GetCarsManager();
    }

    private void stopTimer(int xid){
        xidTimer.get(xid).cancel();
        xidTimer.remove(xid);
    }
    private void resetTimer(int xid) {
        stopTimer(xid);
        xidTimer.put(xid,new Timer());
        xidTimer.get(xid).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("Transaction "+xid+" is aborted due to timeout");
                        try {
                            abort(xid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                TIME_TO_LIVE_MS
        );
    }

    private void Xlock(int id, String name) {
        try {
            lockManager.Lock(id, name, TransactionLockObject.LockType.LOCK_WRITE);
        } catch (DeadlockException de) {
            System.err.println(de.getMessage());
            try {
                abort(id);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void Slock(int id, String name) {
        try {
            lockManager.Lock(id, name, TransactionLockObject.LockType.LOCK_READ);
        } catch (DeadlockException de) {
            System.err.println(de.getMessage());
            try {
                abort(id);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /// ================================= Interface impl ===============================================================



    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Xlock(id, FlightsIndetifier(flightNum));
        resetTimer(id);
        return GetFlightsManager().addFlight(id, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Xlock(id, CarIndetifier(location));
        resetTimer(id);
        return GetCarsManager().addCars(id, location, numCars, price);
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Xlock(id, RoomIndetifier(location));
        resetTimer(id);
        return GetRoomsManager().addRooms(id, location, numRooms, price);
    }

    public int newCustomer(int id) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        int newCid = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));

        if (newCustomer(id, newCid))
            return newCid;

        System.err.println("Cannot add a new customer with id:" + newCid);
        throw new RemoteException();
    }

    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Xlock(id, CustomerIndetifier(cid));
        resetTimer(id);
        if (GetRoomsManager().newCustomer(id, cid) && GetCarsManager().newCustomer(id, cid) && GetFlightsManager().newCustomer(id, cid)) {
            aliveCustomerIds.add(cid);
            return true;
        }
        return false;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        Xlock(id, FlightsIndetifier(flightNum));
        resetTimer(id);
        return GetFlightsManager().deleteFlight(id, flightNum);
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        Xlock(id, CarIndetifier(location));
        resetTimer(id);
        return GetCarsManager().deleteCars(id, location);
    }


    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Xlock(id, RoomIndetifier(location));
        resetTimer(id);
        return GetRoomsManager().deleteRooms(id, location);
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        Xlock(id, CustomerIndetifier(customerID));
        resetTimer(id);
        Boolean deleted = (GetRoomsManager().deleteCustomer(id, customerID) && GetCarsManager().deleteCustomer(id, customerID) && GetFlightsManager().deleteCustomer(id, customerID));
        if (deleted)
            aliveCustomerIds.remove(customerID);
        return deleted;
    }


    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Slock(id, FlightsIndetifier(flightNumber));
        resetTimer(id);
        return GetFlightsManager().queryFlight(id, flightNumber);
    }


    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Slock(id, CarIndetifier(location));
        resetTimer(id);
        return GetCarsManager().queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        Slock(id, RoomIndetifier(location));
        resetTimer(id);
        return GetRoomsManager().queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        Slock(id, CustomerIndetifier(customerID));
        resetTimer(id);
        return GetFlightsManager().queryCustomerInfo(id, customerID) + GetRoomsManager().queryCustomerInfo(id, customerID) + GetCarsManager().queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        Slock(id, FlightsIndetifier(flightNumber));
        resetTimer(id);
        return GetFlightsManager().queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        Slock(id, CarIndetifier(location));
        resetTimer(id);
        return GetCarsManager().queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        Slock(id, RoomIndetifier(location));
        resetTimer(id);
        return GetRoomsManager().queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        Xlock(id, FlightsIndetifier(flightNumber));
        Slock(id, CustomerIndetifier(customerID));
        resetTimer(id);
        return GetFlightsManager().reserveFlight(id, customerID, flightNumber);
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        Xlock(id, CarIndetifier(location));
        Slock(id, CustomerIndetifier(customerID));
        resetTimer(id);
        return GetCarsManager().reserveCar(id, customerID, location);
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        Xlock(id, RoomIndetifier(location));
        Slock(id, CustomerIndetifier(customerID));
        resetTimer(id);
        return GetRoomsManager().reserveRoom(id, customerID, location);
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Bundle with invalid xid");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");


        if(car && GetCarsManager().queryCars(id,location)==0)
            return false;
        if(room && GetRoomsManager().queryRooms(id,location)==0)
            return false;

        for(String flightIdString:flightNumbers) {
            try {
                int flightId = Integer.parseInt(flightIdString);
                if (GetFlightsManager().queryFlight(id, flightId) == 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                System.err.println("FlightId " + flightIdString + " is not a number!\n");
                return false;
            }
        }
        Boolean passing = true;
        if(car)
            passing &= GetCarsManager().reserveCar(id, customerID, location);

        if(room)
            passing &=GetRoomsManager().reserveRoom(id, customerID, location);

        for(String flightIdString:flightNumbers) {
            try {
                int flightId = Integer.parseInt(flightIdString);
                passing &= GetFlightsManager().reserveFlight(id,customerID,flightId);
            } catch (NumberFormatException e) {
                System.err.println("FlightId " + flightIdString + " is not a number!\n");
                return false;
            }
        }
        return passing;
    }

    public int start() throws RemoteException {
        //final int xid = Integer.parseInt(String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
        //        String.valueOf(Math.round(Math.random() * 100 + 1)));
        pendingXids.add(new_xid);
        updatesFlight.put(new_xid,false);
        updatesCar.put(new_xid,false);
        updatesRoom.put(new_xid,false);

        // Schedule a timer for the transaction.
        xidTimer.put(new_xid,new Timer());
        xidTimer.get(new_xid).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("Transaction "+new_xid+" is aborted due to timeout");
                        try {
                            abort(new_xid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                TIME_TO_LIVE_MS
        );
        new_xid++;
        return new_xid-1;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }
        Boolean status = false;
        if (GetRoomsManager().readyToCommit() && GetCarsManager().readyToCommit() && GetFlightsManager().readyToCommit()) {
            status = GetFlightsManager().commit(transactionId);
            status &= GetRoomsManager().commit(transactionId);
            status &= GetCarsManager().commit(transactionId);
        }

        if (status) {
            lockManager.UnlockAll(transactionId);
            pendingXids.remove(transactionId);
            stopTimer(transactionId);
        }
        return status;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }

        GetFlightsManager().abort(transactionId);
        GetRoomsManager().abort(transactionId);
        GetCarsManager().abort(transactionId);
        lockManager.UnlockAll(transactionId);
        pendingXids.remove(transactionId);
        stopTimer(transactionId);
    }

    public boolean shutdown() throws RemoteException {
        return false;
    }

    static private String FlightsIndetifier(int flightnum) {
        return "flight-" + flightnum;
    }

    static private String CustomerIndetifier(int cid) {
        return "customer-" + cid;
    }

    static private String CarIndetifier(String location) {
        return "car-" + location;
    }

    static private String RoomIndetifier(String location) {
        return "room-" + location;
    }

    public String getName() throws RemoteException {
        return "Middleware";
    }
}



