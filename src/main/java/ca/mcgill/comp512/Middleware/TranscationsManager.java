package ca.mcgill.comp512.Middleware;

import ca.mcgill.comp512.LockManager.DeadlockException;
import ca.mcgill.comp512.LockManager.TransactionAbortedException;
import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TranscationsManager {
    private static class TranscationsManagerState implements Serializable {
        HashSet<Integer> aliveCustomerIds = new HashSet<>();
        HashSet<Integer> pendingXids = new HashSet<>();
        HashMap<Integer, Integer> involvementMask = new HashMap<>();
        HashMap<Integer, Long> xidTimer = new HashMap<>();
        HashMap<Integer, String> transactionStates = new HashMap<>();
        Integer idGen = 1;
        private IResourceManager.TransactionManagerCrashModes mode = IResourceManager.TransactionManagerCrashModes.NONE;
    }

    public enum TWOPHASECOMMIT_DECISION {
        COMMIT, ABORT, PROTOCOL_FAILED;
    }


    private int TWOPHASECOMMIT_DELAY = 7000;
    public static final int FLIGHTS_FLAG = 1;  // 0001
    public static final int ROOMS_FLAG = 2;  // 0010
    public static final int CARS_FLAG = 4;  // 0100

    private final int TIME_TO_LIVE_MS = 25000;
    private RMIMiddleware ownerMiddleware;
    private TranscationsManagerState state;



    private Boolean logAccess = true;

    private String stateFilename() {
        return ownerMiddleware.stateFilename;
    }

    TranscationsManager(RMIMiddleware ownerMiddleware) {
        this.ownerMiddleware = ownerMiddleware;
        this.state = new TranscationsManagerState();


        // Create a tx timeout thread
        new Thread(() -> {
            while (true) {
                try {
                    for (Integer xid : state.pendingXids) {
                        if (System.currentTimeMillis() - state.xidTimer.get(xid) > TIME_TO_LIVE_MS) {
                            System.out.println("Aborting " + xid + " due to timeout");

                            abort(xid);
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
        state.xidTimer.remove(xid);
        saveState();
    }

    private void resetTimer(int xid) {
        System.out.println("Reseting the timer for " + xid);
        state.xidTimer.put(xid, System.currentTimeMillis());
        saveState();
    }

    private void UpdateMask(int transactionId, int flag) {
        int newMask = (state.involvementMask.get(transactionId) | flag);
        state.involvementMask.put(transactionId, newMask);
        saveState();
    }

    /// ================================= Interface impl ===============================================================


    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException, DeadlockException {

        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().addFlight(id, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().addCars(id, location, numCars, price);
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().addRooms(id, location, numRooms, price);
    }

    public int newCustomer(int id) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
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

    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);

        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        if (GetRoomsManager().newCustomer(id, cid) && GetCarsManager().newCustomer(id, cid) && GetFlightsManager().newCustomer(id, cid)) {
            state.aliveCustomerIds.add(cid);
            saveState();
            return true;
        }
        return false;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().deleteFlight(id, flightNum);
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        resetTimer(id);
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().deleteCars(id, location);
    }

    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().deleteRooms(id, location);
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        Boolean deleted = (GetRoomsManager().deleteCustomer(id, customerID) && GetCarsManager().deleteCustomer(id, customerID) && GetFlightsManager().deleteCustomer(id, customerID));
        if (deleted) {
            state.aliveCustomerIds.remove(customerID);
            saveState();
        }
        return deleted;
    }


    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().queryFlight(id, flightNumber);
    }


    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        return GetFlightsManager().queryCustomerInfo(id, customerID) + GetRoomsManager().queryCustomerInfo(id, customerID) + GetCarsManager().queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid.");
        resetTimer(id);
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().reserveFlight(id, customerID, flightNumber);
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().reserveCar(id, customerID, location);
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().reserveRoom(id, customerID, location);
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!state.pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Bundle with invalid xid");
        resetTimer(id);
        if (!state.aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");

        if (car) {
            UpdateMask(id, CARS_FLAG);
            if (GetCarsManager().queryCars(id, location) == 0)
                return false;
        }

        if (room) {
            UpdateMask(id, ROOMS_FLAG);
            if (GetRoomsManager().queryRooms(id, location) == 0)
                return false;
        }

        for (String flightIdString : flightNumbers) {
            try {
                int flightId = Integer.parseInt(flightIdString);
                UpdateMask(id, FLIGHTS_FLAG);
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

        return passing;
    }

    public int start() throws RemoteException {
        synchronized (state.idGen) {
            int new_xid = state.idGen++;

            state.pendingXids.add(new_xid);
            state.involvementMask.put(new_xid, 0);

            // Schedule a timer for the transaction.
            System.out.println("Starting the timer for " + new_xid);
            state.xidTimer.put(new_xid, System.currentTimeMillis());
            saveState();
            return new_xid;
        }
    }

    private ArrayList<Boolean> VotingPhase(int transactionId, List<String> requiredServers) {
        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.BEFORE_SEND_VOTE_REQ);

        System.out.println("2PC-" + transactionId + ": asking for votes from:");
        for (String name : requiredServers)
            System.out.print(name + " ");
        System.out.println();

        ArrayList<Boolean> votes = new ArrayList<>();
        if (requiredServers.size() > 0) {
            ArrayList<Callable<Boolean>> voteRequests = new ArrayList<>();
            ArrayList<Future<Boolean>> future_votes = new ArrayList<>();
            for (String name : requiredServers) {
                voteRequests.add(() -> {
                            if (ownerMiddleware.dead.get(name).get()) {
                                System.err.println("A required server is dead, commit cannot proceed!");
                                return false;
                            }
                            boolean vote = getResourceManagerForName(name).prepare(transactionId);
                            Thread.sleep(800);
                            return vote;

                        }
                );
            }
            final ExecutorService service = Executors.newFixedThreadPool(voteRequests.size());
            for (Callable<Boolean> voteRequest : voteRequests)
                future_votes.add(service.submit(voteRequest));

            service.shutdown();

            for (Future<Boolean> vote : future_votes) {
                try {
                    votes.add(vote.get());
                    crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_REC_SOME_REPLIES);
                } catch (Exception e) {
                    System.err.println("Cannot get vote results from one of the servers");
                    return new ArrayList<>();
                }
            }
            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SEND_VOTE_REQ);
        }
        System.out.println("Got the required votes!");
        return votes;
    }

    private TWOPHASECOMMIT_DECISION DecisionPhase(int transactionId, ArrayList<Boolean> votes, List<String> requiredServers) {

        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_REC_ALL_REPLIES);

        boolean decision = (votes.size() == requiredServers.size()) && state.pendingXids.contains(transactionId);
        for (Boolean vote : votes)
            decision &= vote;
        System.out.println("Sending a " + ((decision) ? "Yes" : "No") + " decision to participants...");


        boolean decision_passed = false;
        if (decision) {
            state.transactionStates.put(transactionId, "Commit");
            saveState();
            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_DECIDING);
            for (String name : requiredServers) {
                try {
                    IResourceManager rm = getResourceManagerForName(name);
                    try {
                        getResourceManagerForName(name).commit(transactionId);
                        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_SOME_DECISIONS);
                        return TWOPHASECOMMIT_DECISION.COMMIT;
                    } catch (Exception e) {
                        System.err.println("Failed to send a commit decision to " + rm.toString());
                    }
                } catch (Exception e) {
                    System.err.println(name + " resource manager did not receive the commit decision [ " + e.getMessage() + " ]");
                }
                decision_passed = true;
            }
        } else {
            state.transactionStates.put(transactionId, "Abort");
            saveState();
            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_DECIDING);
            for (String name : requiredServers) {
                try {
                    IResourceManager rm = getResourceManagerForName(name);
                    try {
                        System.out.println("Aborting " + transactionId + " of a NO vote");
                        rm.abort(transactionId);
                        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_SOME_DECISIONS);
                        return TWOPHASECOMMIT_DECISION.ABORT;
                    } catch (Exception e) {
                        System.err.println("Failed to send an abort decision to " + rm.toString());
                    }
                } catch (Exception e) {
                    System.err.println(name + " resource manager did not receive the abort decision [ " + e.getMessage() + " ]");
                }
                decision_passed = true;
            }
        }

        if (decision_passed && decision) {
            return TWOPHASECOMMIT_DECISION.COMMIT;
        } else if(decision_passed && decision){
            return TWOPHASECOMMIT_DECISION.ABORT;
        } else {
            return TWOPHASECOMMIT_DECISION.PROTOCOL_FAILED;
        }
    }

    private TWOPHASECOMMIT_DECISION TwoPC_Protocol(int transactionId) {
        List<String> requiredServers = GetRequiredServers(transactionId);

        state.transactionStates.put(transactionId, "Start");
        saveState();

        TWOPHASECOMMIT_DECISION d = DecisionPhase(transactionId, VotingPhase(transactionId, requiredServers), requiredServers);
        if (d == TWOPHASECOMMIT_DECISION.PROTOCOL_FAILED) {
            System.out.println("2PC-" + transactionId + ": failed, retrying in " + TWOPHASECOMMIT_DELAY / 1000 + " Seconds...");
            // Sleep then retry again.
            try {
                Thread.sleep(TWOPHASECOMMIT_DELAY);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (!state.pendingXids.contains(transactionId)) {
                System.err.println(transactionId + " already aborted due to timeout, cannot retry 2PC");
                return d;
            }
            d = DecisionPhase(transactionId, VotingPhase(transactionId, requiredServers), requiredServers);
        }
        return d;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!state.pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }

        TWOPHASECOMMIT_DECISION protocolSuccess = TwoPC_Protocol(transactionId);

        if (protocolSuccess == TWOPHASECOMMIT_DECISION.COMMIT || protocolSuccess == TWOPHASECOMMIT_DECISION.ABORT) {
            clearLocalVars(transactionId);
            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_ALL_DECISIONS);
            return true;
        } else if (protocolSuccess == TWOPHASECOMMIT_DECISION.PROTOCOL_FAILED) {
            System.out.println("Aborting " + transactionId + " as protocol was not successful");
            abort(transactionId);
        }
        return false;
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        if (!state.pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }

        List<String> requiredServers = GetRequiredServers(transactionId);
        String msg = "Aborting " + transactionId + " on ";

        for (String name : requiredServers) {
            msg += name;
        }

        System.out.println(msg);
        state.transactionStates.put(transactionId, "Abort");
        for (String name : requiredServers) {
            if (ownerMiddleware.dead.get(name).get()) {
                System.err.println("Failed to request abort on " + name + " since it's dead!");
            } else {
                getResourceManagerForName(name).abort(transactionId);
            }
        }
        clearLocalVars(transactionId);
    }

    public void clearLocalVars(int transactionId) {
        stopTimer(transactionId);
        state.pendingXids.remove(transactionId);
        state.involvementMask.remove(transactionId);

        saveState();
    }

    public boolean shutdown() throws RemoteException {
        File log = new File(stateFilename());
        if (log.exists()) {
            log.delete();
            return true;
        } else {
            System.err.println(stateFilename() + " is missing on shutdown!");
            return false;
        }
    }

    private List<String> GetRequiredServers(int transactionId) {
        List<String> requiredServers = new ArrayList<>();
        int invo_mask = state.involvementMask.get(transactionId);

        if ((invo_mask & FLIGHTS_FLAG) != 0) {
            requiredServers.add("Flights");
        }

        if ((invo_mask & ROOMS_FLAG) != 0) {
            requiredServers.add("Rooms");
        }

        if ((invo_mask & CARS_FLAG) != 0) {
            requiredServers.add("Cars");
        }
        return requiredServers;
    }

    public String getName() throws RemoteException {
        return "Middleware";
    }

    public IResourceManager getResourceManagerForName(String name) throws RemoteException {
        switch (name) {
            case "Flights":
                return GetFlightsManager();
            case "Cars":
                return GetCarsManager();
            case "Rooms":
                return GetRoomsManager();
            default:
                return null;
        }
    }

    private void crashIfModeIs(IResourceManager.TransactionManagerCrashModes mode) {
        if (this.state.mode == mode) {
            System.exit(1);
        }
    }


    public boolean SetCrashMode(IResourceManager.TransactionManagerCrashModes mode) {
        this.state.mode = mode;
        saveState();
        return true;
    }

    public void recoverState() {
        try (ObjectInputStream ios =
                     new ObjectInputStream(new FileInputStream(stateFilename()))) {

            state = (TranscationsManagerState) ios.readObject();

            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.DURING_RECOVERY);

            for (int xid : state.pendingXids) {
                // Does not find a Start-2PC record but transaction active, you abort
                if (!state.transactionStates.containsKey(xid)) {
                    System.out.println("Aborting " + xid + " because did not find a Start-2PC record but transaction active");
                    abort(xid);
                } else {
                    // Finds a Start-2PC record, abort (or resend vote request but we don't)
                    if (state.transactionStates.get(xid).equals("Start")) {
                        System.out.println("Found start-2PC for " + xid + ", aborting");
                        abort(xid);
                    }
                    // Resend commit request
                    else if (state.transactionStates.get(xid).equals("Commit")) {
                        System.out.println("Found Commit for " + xid + ", resending commit decisions");
                        List<String> requiredServers = GetRequiredServers(xid);
                        for (String name : requiredServers) {
                            try {
                                IResourceManager rm = getResourceManagerForName(name);
                                try {
                                    getResourceManagerForName(name).commit(xid);
                                } catch (Exception e) {
                                    System.err.println("Failed to send a commit decision to " + rm.toString());
                                }
                            } catch (Exception e) {
                                System.err.println(name + " resource manager did not receive the commit decision [ " + e.getMessage() + " ]");
                            }
                        }
                        clearLocalVars(xid);
                    }
                    // Resend abort request
                    else if (state.transactionStates.get(xid).equals("Abort")) {
                        System.out.println("Aborting " + xid + " Resend abort request");
                        abort(xid);
                    }
                }
            }

        } catch (EOFException e) {
            System.err.println(stateFilename() + " is empty, no existing previous state to restore.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void saveState() {
        synchronized (logAccess) {
            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(stateFilename()))) {

                oos.writeObject(state);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



