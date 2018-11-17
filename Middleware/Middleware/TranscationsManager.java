package Middleware;

import LockManager.*;
import Server.Interface.*;

import java.rmi.RemoteException;
import java.util.*;

public class TranscationsManager {
    private final int TIME_TO_LIVE_MS = 120000;
    private RMIMiddleware ownerMiddleware;

    private static HashSet<Integer> aliveCustomerIds = new HashSet<>();
    private HashSet<Integer> pendingXids = new HashSet<Integer>();
    private HashSet<Integer> updatesFlight = new HashSet<Integer>();
    private HashSet<Integer> updatesCar = new HashSet<Integer>();
    private HashSet<Integer> updatesRoom = new HashSet<Integer>();
    private HashMap<Integer, Long> xidTimer = new HashMap<Integer, Long>();

    TranscationsManager(RMIMiddleware ownerMiddleware) {
        this.ownerMiddleware = ownerMiddleware;
        new Thread(() -> {
            while (true) {
                try {
                    for (Integer xid : pendingXids) {
                        if (System.currentTimeMillis() - xidTimer.get(xid) > TIME_TO_LIVE_MS) {
                            abort(xid);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {

                }
            }
        }).start();
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

    private void stopTimer(int xid) {
        System.out.println("Stopping the timer for " + xid);
        xidTimer.remove(xid);
    }

    private void resetTimer(int xid) {
        System.out.println("Reseting the timer for " + xid);
        xidTimer.put(xid, System.currentTimeMillis());
    }

    /// ================================= Interface impl ===============================================================


    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,InvalidTransactionException,DeadlockException {

        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetFlightsManager().addFlight(id, flightNum, flightSeats, flightPrice)) {
            updatesFlight.add(id);
            return true;
        }
        return false;
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException,InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetCarsManager().addCars(id, location, numCars, price)) {
            updatesCar.add(id);
            return true;
        }
        return false;
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException,InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetRoomsManager().addRooms(id, location, numRooms, price)) {
            updatesRoom.add(id);
            return true;
        }
        return false;
    }

    public int newCustomer(int id) throws RemoteException,InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        int newCid = Integer.parseInt(
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));

        if (newCustomer(id, newCid))
            return newCid;

        System.err.println("Cannot add a new customer with id:" + newCid);
        throw new RemoteException();
    }

    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetRoomsManager().newCustomer(id, cid) && GetCarsManager().newCustomer(id, cid) && GetFlightsManager().newCustomer(id, cid)) {
            aliveCustomerIds.add(cid);
            updatesFlight.add(id);
            updatesCar.add(id);
            updatesRoom.add(id);
            return true;
        }
        return false;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetFlightsManager().deleteFlight(id, flightNum)) {
            updatesFlight.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        resetTimer(id);
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if (GetCarsManager().deleteCars(id, location)) {
            updatesCar.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        if (GetRoomsManager().deleteRooms(id, location)) {
            updatesRoom.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        resetTimer(id);
        Boolean deleted = (GetRoomsManager().deleteCustomer(id, customerID) && GetCarsManager().deleteCustomer(id, customerID) && GetFlightsManager().deleteCustomer(id, customerID));
        if (deleted) {
            aliveCustomerIds.remove(customerID);
            updatesFlight.add(id);
            updatesCar.add(id);
            updatesRoom.add(id);
        }
        return deleted;
    }


    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetFlightsManager().queryFlight(id, flightNumber);
    }


    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetCarsManager().queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetRoomsManager().queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        return GetFlightsManager().queryCustomerInfo(id, customerID) + GetRoomsManager().queryCustomerInfo(id, customerID) + GetCarsManager().queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetFlightsManager().queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetCarsManager().queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetRoomsManager().queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid.");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if (GetFlightsManager().reserveFlight(id, customerID, flightNumber)) {
            updatesFlight.add(id);
            return true;
        }
        return false;
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if (GetCarsManager().reserveCar(id, customerID, location)) {
            updatesCar.add(id);
            return true;
        }
        return false;
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if (GetRoomsManager().reserveRoom(id, customerID, location)) {
            updatesRoom.add(id);
            return true;
        }
        return false;
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,InvalidTransactionException,DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Bundle with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");


        if (car && GetCarsManager().queryCars(id, location) == 0)
            return false;
        if (room && GetRoomsManager().queryRooms(id, location) == 0)
            return false;

        for (String flightIdString : flightNumbers) {
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
        if (car)
            passing &= GetCarsManager().reserveCar(id, customerID, location);

        if (room)
            passing &= GetRoomsManager().reserveRoom(id, customerID, location);

        for (String flightIdString : flightNumbers) {
            try {
                int flightId = Integer.parseInt(flightIdString);
                passing &= GetFlightsManager().reserveFlight(id, customerID, flightId);
            } catch (NumberFormatException e) {
                System.err.println("FlightId " + flightIdString + " is not a number!\n");
                return false;
            }
        }

        if (passing) {
            if (car)
                updatesCar.add(id);
            if (room)
                updatesRoom.add(id);
            if (flightNumbers.size() > 0)
                updatesFlight.add(id);
        }

        return passing;
    }

    public int start() throws RemoteException {
        int new_xid = Integer.parseInt(
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                        String.valueOf(Math.round(Math.random() * 100 + 1)));
        pendingXids.add(new_xid);

        // Schedule a timer for the transaction.
        System.out.println("Starting the timer for " + new_xid);
        xidTimer.put(new_xid, System.currentTimeMillis());
        return new_xid;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }

        List<IResourceManager> requiredServers = new ArrayList<>();

        if (updatesFlight.contains(transactionId))
            requiredServers.add(GetFlightsManager());

        if (updatesRoom.contains(transactionId))
            requiredServers.add(GetCarsManager());

        if (updatesRoom.contains(transactionId))
            requiredServers.add(GetRoomsManager());

        Boolean status = true;
        for (IResourceManager rm : requiredServers)
            status &= rm.commit(transactionId);

        if (status) {
            pendingXids.remove(transactionId);
            stopTimer(transactionId);
        }
        return status;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }
        pendingXids.remove(transactionId);
        stopTimer(transactionId);

        if (updatesFlight.contains(transactionId)) {
            GetFlightsManager().abort(transactionId);
            updatesFlight.remove(transactionId);
        }
        if (updatesRoom.contains(transactionId)) {
            GetRoomsManager().abort(transactionId);
            updatesRoom.remove(transactionId);
        }
        if (updatesCar.contains(transactionId)) {
            GetCarsManager().abort(transactionId);
            updatesCar.remove(transactionId);
        }


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



