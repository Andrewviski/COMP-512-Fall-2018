package ca.mcgill.comp512.Middleware;

import ca.mcgill.comp512.LockManager.DeadlockException;
import ca.mcgill.comp512.LockManager.TransactionAbortedException;
import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TranscationsManager {
    private Integer idGen = 1;
    public static final int FLIGHTS_FLAG = 1;  // 0001
    public static final int ROOMS_FLAG = 2;  // 0010
    public static final int CARS_FLAG = 4;  // 0100

    private final int TIME_TO_LIVE_MS = 15000;
    private RMIMiddleware ownerMiddleware;

    private static HashSet<Integer> aliveCustomerIds = new HashSet<>();
    private HashSet<Integer> pendingXids = new HashSet<>();
    private HashMap<Integer, Integer> involvementMask = new HashMap<>();
    private HashMap<Integer, Long> xidTimer = new HashMap<>();
    private IResourceManager.TransactionManagerCrashModes mode = IResourceManager.TransactionManagerCrashModes.NONE;
    private String logFilename = "transactionManager.log";
    private Boolean logAccess = true;

    private void Log(String logMessage) {
        synchronized (logAccess) {
            try {
                PrintWriter writer = new PrintWriter(logFilename);
                writer.println(logMessage);
                writer.close();
                System.out.println("\033[0m Log: \033[0m " + logMessage);
            } catch (FileNotFoundException fnfe) {
                System.err.println(logFilename + " is unexpectedly missing!");
                fnfe.printStackTrace();
            } catch (Exception e) {
                System.err.println("Failed to log [ \"" + logMessage + "\" ] into " + logFilename);
                e.printStackTrace();
            }
        }
    }

    TranscationsManager(RMIMiddleware ownerMiddleware) {
        this.ownerMiddleware = ownerMiddleware;
        // If a log exist then we are recovering, otherwise it's a fresh bootup.
        File log = new File(logFilename);
        if (log.exists()) {
            System.err.println("Recovering from " + logFilename);
            recover();
        } else {
            try {
                System.err.println("Creating " + logFilename);
                log.createNewFile();
                log.deleteOnExit();
            } catch (Exception e) {
                System.err.println("Failed to create " + logFilename + " terminating...");
                e.printStackTrace();
                System.exit(-1);
            }
        }

        // Create a tx timeout thread
        new Thread(() -> {
            while (true) {
                try {
                    for (Integer xid : pendingXids) {
                        if (System.currentTimeMillis() - xidTimer.get(xid) > TIME_TO_LIVE_MS) {
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
        xidTimer.remove(xid);
    }

    private void resetTimer(int xid) {
        System.out.println("Reseting the timer for " + xid);
        xidTimer.put(xid, System.currentTimeMillis());
    }

    private void UpdateMask(int transactionId, int flag) {
        int newMask = (involvementMask.get(transactionId) | flag);
        involvementMask.put(transactionId, newMask);
        Log(transactionId + " involvementMask " + newMask);
    }

    /// ================================= Interface impl ===============================================================


    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException, DeadlockException {

        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().addFlight(id, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().addCars(id, location, numCars, price);
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().addRooms(id, location, numRooms, price);
    }

    public int newCustomer(int id) throws RemoteException, InvalidTransactionException, DeadlockException {
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

    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);

        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        if (GetRoomsManager().newCustomer(id, cid) && GetCarsManager().newCustomer(id, cid) && GetFlightsManager().newCustomer(id, cid)) {
            aliveCustomerIds.add(cid);
            return true;
        }
        return false;
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().deleteFlight(id, flightNum);
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        resetTimer(id);
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().deleteCars(id, location);
    }

    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().deleteRooms(id, location);
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        Boolean deleted = (GetRoomsManager().deleteCustomer(id, customerID) && GetCarsManager().deleteCustomer(id, customerID) && GetFlightsManager().deleteCustomer(id, customerID));
        if (deleted) {
            aliveCustomerIds.remove(customerID);
        }
        return deleted;
    }


    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().queryFlight(id, flightNumber);
    }


    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, FLIGHTS_FLAG);
        UpdateMask(id, CARS_FLAG);
        UpdateMask(id, ROOMS_FLAG);
        return GetFlightsManager().queryCustomerInfo(id, customerID) + GetRoomsManager().queryCustomerInfo(id, customerID) + GetCarsManager().queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Querying with invalid xid.");
        resetTimer(id);
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid.");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, FLIGHTS_FLAG);
        return GetFlightsManager().reserveFlight(id, customerID, flightNumber);
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, CARS_FLAG);
        return GetCarsManager().reserveCar(id, customerID, location);
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Reserving with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
            throw new RemoteException("Customer " + customerID + " does not exist.");
        UpdateMask(id, ROOMS_FLAG);
        return GetRoomsManager().reserveRoom(id, customerID, location);
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException, DeadlockException {
        if (!pendingXids.contains(id))
            throw new InvalidTransactionException(id, "Bundle with invalid xid");
        resetTimer(id);
        if (!aliveCustomerIds.contains(customerID))
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
        synchronized (idGen) {
            int new_xid = idGen++;

            Log("START-TX " + new_xid);
            pendingXids.add(new_xid);
            involvementMask.put(new_xid, 0);

            // Schedule a timer for the transaction.
            System.out.println("Starting the timer for " + new_xid);
            xidTimer.put(new_xid, System.currentTimeMillis());
            return new_xid;
        }
    }

    private boolean TwoPC_Protocol(int transactionId) {
        boolean success = true;

        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.BEFORE_SEND_VOTE_REQ);

        List<IResourceManager> requiredServers = new ArrayList<>();
        int invo_mask = involvementMask.get(transactionId);


        if ((invo_mask & FLIGHTS_FLAG) != 0) {
            requiredServers.add(GetFlightsManager());
            System.out.println(transactionId + " need to be commited on flight server.");
        }

        if ((invo_mask & CARS_FLAG) != 0) {
            requiredServers.add(GetCarsManager());
            System.out.println(transactionId + " need to be commited on cars server.");
        }

        if ((invo_mask & ROOMS_FLAG) != 0) {
            requiredServers.add(GetRoomsManager());

        }


        if (requiredServers.size() > 0) {
            final ArrayList<Callable<Boolean>> voteRequests = new ArrayList<>();
            final ArrayList<Future<Boolean>> votes = new ArrayList<>();

            for (IResourceManager rm : requiredServers) {
                voteRequests.add(() -> {
                            if (rm == null) {
                                System.err.println("A required server is dead, commit cannot proceed!");
                                return false;
                            }
                            return rm.prepare(transactionId);
                        }
                );
            }
            final ExecutorService service = Executors.newFixedThreadPool(voteRequests.size());
            for (Callable<Boolean> voteRequest : voteRequests)
                votes.add(service.submit(voteRequest));

            crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SEND_VOTE_REQ);

            for (Future<Boolean> vote : votes) {
                try {
                    success &= vote.get();
                    crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_REC_SOME_REPLIES);
                } catch (Exception e) {
                    System.err.println("Cannot get vote results from one of the servers");
                    e.printStackTrace();
                    service.shutdown();
                    return false;
                }
            }
            service.shutdown();
        }

        // Not sure whats the difference? :/
        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_REC_ALL_REPLIES);
        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_DECIDING);

        if (success) {
            for (IResourceManager rm : requiredServers) {
                try {
                    rm.commit(transactionId);
                    crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_SOME_DECISIONS);
                } catch (Exception e) {
                    System.out.println("Failed to send a commit decision to " + rm.toString());
                    e.printStackTrace();
                }
            }
        } else {
            for (IResourceManager rm : requiredServers) {
                try {
                    rm.abort(transactionId);
                    crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_SOME_DECISIONS);
                } catch (Exception e) {
                    System.out.println("Failed to send an abort decision to " + rm.toString());
                    e.printStackTrace();
                }
            }
        }


        crashIfModeIs(IResourceManager.TransactionManagerCrashModes.AFTER_SENDING_ALL_DECISIONS);
        return success;
    }

    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }
        Log("START-2PC " + transactionId);

        boolean protocolSuccess = TwoPC_Protocol(transactionId);

        if (protocolSuccess) {
            pendingXids.remove(transactionId);
            stopTimer(transactionId);
            return true;
        } else {
            abort(transactionId);
            return false;
        }
    }

    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        if (!pendingXids.contains(transactionId)) {
            throw new InvalidTransactionException(transactionId, "Invalid commit xid.");
        }
        Log("ABORT-2PC " + transactionId);
        pendingXids.remove(transactionId);
        stopTimer(transactionId);

        int invo_mask = involvementMask.get(transactionId);
        involvementMask.remove(transactionId);

        if ((invo_mask & FLIGHTS_FLAG) != 0)
            GetFlightsManager().abort(transactionId);
        if ((invo_mask & ROOMS_FLAG) != 0)
            GetRoomsManager().abort(transactionId);
        if ((invo_mask & CARS_FLAG) != 0)
            GetCarsManager().abort(transactionId);
    }

    public boolean shutdown() throws RemoteException {
        File log = new File(logFilename);
        if (log.exists()) {
            log.delete();
            return true;
        } else {
            System.err.println(logFilename + " is missing on shutdown!");
            return false;
        }
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

    private void crashIfModeIs(IResourceManager.TransactionManagerCrashModes mode) {
        if (this.mode == mode) {
            System.exit(1);
        }
    }

    private void recover() {
        // TODO(abudan): Implement this
    }
}



