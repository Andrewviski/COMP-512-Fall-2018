package Middleware;

import LockManager.*;
import Server.Interface.*;

import java.rmi.RemoteException;
import java.util.*;

public class TranscationsManager {
    private int new_xid = 0;
    private final int TIME_TO_LIVE_MS = 100000;
    private RMIMiddleware ownerMiddleware;

    private static HashSet<Integer> aliveCustomerIds = new HashSet<>();
    private HashSet<Integer> pendingXids = new HashSet<Integer>();
    private HashSet<Integer> updatesFlight = new HashSet<Integer>();
    private HashSet<Integer> updatesCar = new HashSet<Integer>();
    private HashSet<Integer> updatesRoom = new HashSet<Integer>();
    private HashMap<Integer, Timer> xidTimer = new HashMap<Integer, Timer>();

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

    private void stopTimer(int xid) {
        xidTimer.get(xid).cancel();
        xidTimer.remove(xid);
    }

    private void resetTimer(int xid) {
        stopTimer(xid);
        xidTimer.put(xid, new Timer());
        xidTimer.get(xid).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("Transaction " + xid + " is aborted due to timeout");
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

    /// ================================= Interface impl ===============================================================


    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if (GetFlightsManager().addFlight(id, flightNum, flightSeats, flightPrice)) {
            resetTimer(id);
            updatesFlight.add(id);
            return true;
        }
        return false;
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if (GetCarsManager().addCars(id, location, numCars, price)) {
            resetTimer(id);
            updatesCar.add(id);
            return true;
        }
        return false;
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if(GetRoomsManager().addRooms(id, location, numRooms, price)){
            resetTimer(id);
            updatesRoom.add(id);
            return true;
        }
        return false;
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

        if (GetRoomsManager().newCustomer(id, cid) && GetCarsManager().newCustomer(id, cid) && GetFlightsManager().newCustomer(id, cid)) {
            aliveCustomerIds.add(cid);
            resetTimer(id);
            updatesFlight.add(id);
            updatesCar.add(id);
            updatesRoom.add(id);
            return true;
        }
        return false;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if(GetFlightsManager().deleteFlight(id, flightNum)){
            resetTimer(id);
            updatesFlight.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if(GetCarsManager().deleteCars(id, location)){
            resetTimer(id);
            updatesCar.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");

        if(GetRoomsManager().deleteRooms(id, location)){
            resetTimer(id);
            updatesRoom.add(id);
            return true;
        }
        return false;
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        Boolean deleted = (GetRoomsManager().deleteCustomer(id, customerID) && GetCarsManager().deleteCustomer(id, customerID) && GetFlightsManager().deleteCustomer(id, customerID));
        if (deleted) {
            aliveCustomerIds.remove(customerID);
            resetTimer(id);
            updatesFlight.add(id);
            updatesCar.add(id);
            updatesRoom.add(id);
        }
        return deleted;
    }


    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetFlightsManager().queryFlight(id, flightNumber);
    }


    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetCarsManager().queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        return GetRoomsManager().queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        resetTimer(id);
        return GetFlightsManager().queryCustomerInfo(id, customerID) + GetRoomsManager().queryCustomerInfo(id, customerID) + GetCarsManager().queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetFlightsManager().queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetCarsManager().queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        return GetRoomsManager().queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if(GetFlightsManager().reserveFlight(id, customerID, flightNumber)){
            resetTimer(id);
            updatesFlight.add(id);
            return true;
        }
        return false;
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if(GetCarsManager().reserveCar(id, customerID, location)){
            resetTimer(id);
            updatesCar.add(id);
            return true;
        }
        return false;
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if(GetRoomsManager().reserveRoom(id, customerID, location)){
            resetTimer(id);
            updatesRoom.add(id);
            return true;
        }
        return false;
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Bundle with invalid xid");
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

        if(passing){
            if(car)
                updatesCar.add(id);
            if(room)
                updatesRoom.add(id);
            if(flightNumbers.size()>0)
                updatesFlight.add(id);
            resetTimer(id);
        }

        return passing;
    }

    public int start() throws RemoteException {
        //final int xid = Integer.parseInt(String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
        //        String.valueOf(Math.round(Math.random() * 100 + 1)));
        pendingXids.add(new_xid);

        // Schedule a timer for the transaction.
        xidTimer.put(new_xid, new Timer());
        xidTimer.get(new_xid).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("Transaction " + new_xid + " is aborted due to timeout");
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
        return new_xid - 1;
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

        GetFlightsManager().abort(transactionId);
        GetRoomsManager().abort(transactionId);
        GetCarsManager().abort(transactionId);
        pendingXids.remove(transactionId);

        if (updatesFlight.contains(transactionId))
            updatesFlight.remove(transactionId);
        if (updatesRoom.contains(transactionId))
            updatesRoom.remove(transactionId);
        if (updatesCar.contains(transactionId))
            updatesCar.remove(transactionId);

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



