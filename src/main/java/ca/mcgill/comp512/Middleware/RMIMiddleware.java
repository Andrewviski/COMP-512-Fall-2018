package ca.mcgill.comp512.Middleware;

import ca.mcgill.comp512.LockManager.DeadlockException;
import ca.mcgill.comp512.LockManager.TransactionAbortedException;
import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class RMIMiddleware implements IResourceManager {
    private static int HEARTBEAT_FREQUENCY = 2000;
    private static boolean HeartbeatOutputEnabled = true;
    private static String middlwareName = "Middleware";
    private static String s_rmiPrefix = "group16_";

    private static final int SERVER_COUNT = 3;
    public static final int middlewarePort = 54006;


    // These arrays will store server names, server hostnames, and resource managers for each resources in
    // the following order: Flights, Rooms, Cars, Customers.
    private static String[] server_names = {"Flights", "Rooms", "Cars"};
    private static String[] server_hostnames = {"localhost", "localhost", "localhost"};
    private static int server_ports[] = {54002, 54003, 54004};
    public HashMap<String, AtomicBoolean> dead = new HashMap<>();
    private static IResourceManager[] resourceManagers = new IResourceManager[SERVER_COUNT];
    private TranscationsManager txManager;

    RMIMiddleware() {
        dead.put("Flights", new AtomicBoolean(true));
        dead.put("Cars", new AtomicBoolean(true));
        dead.put("Rooms", new AtomicBoolean(true));
        txManager = new TranscationsManager(this);
    }

    // Resource managers accessors.
    public IResourceManager GetFlightsManager() {
        if (dead.get("Flights").get()) {
            System.err.println("Trying to access a dead Flights server");
            throw new DeadResourceManagerException("Flights", "");
        }
        return resourceManagers[0];
    }

    public IResourceManager GetRoomsManager() {
        if (dead.get("Rooms").get()) {
            System.err.println("Trying to access a dead Cars server");
            throw new DeadResourceManagerException("Rooms", "");
        }
        return resourceManagers[1];
    }

    public IResourceManager GetCarsManager() {
        if (dead.get("Cars").get()) {
            System.err.println("Trying to access a dead Rooms server");
            throw new DeadResourceManagerException("Cars", "");
        }
        return resourceManagers[2];
    }

    private static void ReportHeartbeat(String msg) {
        if (!HeartbeatOutputEnabled)
            return;
        System.err.println(msg);
    }


    private static void ReportMiddleWareError(String msg, Exception e) {
        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0m" + msg + " ]");
        e.printStackTrace();
        System.exit(1);
    }

    public static void main(String args[]) {
        if (args.length != 0 && args.length != SERVER_COUNT && args.length != SERVER_COUNT * 2) {

            ReportMiddleWareError("We got " + Integer.toString(args.length) + "Args, Usage: java server.Middleware.RMIMiddleware [flights_server_hostname] [rooms_server_hostname] [cars_server_hostname]", null);
        }

        try {
            RMIMiddleware middleware = new RMIMiddleware();
            middleware.parseServersConfig(args);
            middleware.setup();
            middleware.searchForServers();

            // Create a heartbeat thread
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(HEARTBEAT_FREQUENCY);
                        for (int i = 0; i < SERVER_COUNT; i++) {
                            if (!middleware.dead.get(server_names[i]).get()) {
                                try {
                                    middleware.resourceManagers[i].getName();
                                } catch (Exception e) {
                                    middleware.dead.get(server_names[i]).set(true);
                                    ReportHeartbeat(server_names[i] + " died, disconnecting!");
                                }
                            } else {
                                middleware.connectToServer(i);
                                if (!middleware.dead.get(server_names[i]).get())
                                    ReportHeartbeat(server_names[i] + " is alive, reconnected!");
                            }
                        }
                    } catch (Exception e) {
                        ReportHeartbeat("Heartbreat exception at middleware");
                    }
                }
            }).start();

            System.out.println("Middleware is ready and listening on port " + middlewarePort);
        } catch (Exception e) {
            ReportMiddleWareError("Uncaught Exception", e);
        }
    }

    private void parseServersConfig(String args[]) {
        if (args.length == 0)
            return;
        // Parse hostnames and port numbers.
        for (int i = 0; i < SERVER_COUNT; i++) {
            server_hostnames[i] = args[i];
            if (args.length > 4) {
                try {
                    server_ports[i] = Integer.parseInt(args[SERVER_COUNT + i]);
                } catch (NumberFormatException e) {
                    ReportMiddleWareError("One of the specified ports is not a number!", e);
                }
            }
        }
    }

    private void setup() {
        // Create the RMI server entry.
        try {
            // Dynamically generate the stub (client proxy).
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(this, middlewarePort);

            // Bind the remote object's stub in the registry.
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(middlewarePort);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(middlewarePort);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + middlwareName, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind(s_rmiPrefix + middlwareName);
                    System.out.println("'" + middlwareName + "' resource manager unbound");
                } catch (Exception e) {
                    ReportMiddleWareError("Unbounding failed", e);
                }
            }));
            System.out.println("'" + middlwareName + "' resource manager server ready and bound to '" + s_rmiPrefix + middlwareName + "'");
        } catch (Exception e) {
            ReportMiddleWareError("Uncaught exception", e);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    private void searchForServers() {
        boolean[] found = new boolean[SERVER_COUNT];
        while (true) {

            // check if we are done and stop the search in that case.
            boolean done = true;
            for (int i = 0; i < SERVER_COUNT; i++)
                done &= found[i];
            if(done)
                break;

            // sleep for 1 sec then try again.
            try {
                Thread.sleep(1000);
            } catch (Exception e){
                ReportMiddleWareError("Cannot find servers",e);
            }

            for (int i = 0; i < SERVER_COUNT; i++) {
                if(!found[i])
                    found[i]=searchForServer(i);
            }
        }
        System.out.println("Middleware up on port " + middlewarePort + " and connected to servers on ports: " + Arrays.toString(server_ports));
    }

    public boolean searchForServer(int resource_manager_index) {
        String server_host = server_hostnames[resource_manager_index];
        int port = server_ports[resource_manager_index];
        String server_name = server_names[resource_manager_index];

        try {
            Registry registry = LocateRegistry.getRegistry(server_host, port);
            resourceManagers[resource_manager_index] = (IResourceManager) registry.lookup(s_rmiPrefix + server_name);
            System.out.println("Connected to server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_names[resource_manager_index] + "]");
            dead.get(server_name).set(false);
            return true;
        } catch (Exception e) {
            System.out.println("Waiting for Server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_names[resource_manager_index] + "]");
        }
        dead.get(server_name).set(true);
        return false;
    }

    public void connectToServer(int resource_manager_index) {
        String server_host = server_hostnames[resource_manager_index];
        int port = server_ports[resource_manager_index];
        String server_name = server_names[resource_manager_index];

        try {
            Registry registry = LocateRegistry.getRegistry(server_host, port);
            resourceManagers[resource_manager_index] = (IResourceManager) registry.lookup(s_rmiPrefix + server_name);
            ReportHeartbeat("Connected to server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_names[resource_manager_index] + "]");
            dead.get(server_name).set(false);
            return;
        } catch (Exception e) {
            ReportHeartbeat("Checking if Server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_names[resource_manager_index] + "] has recovered");
        }
        dead.get(server_name).set(true);
    }

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.addFlight(id, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.addCars(id, location, numCars, price);

    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.addRooms(id, location, numRooms, price);
    }

    public int newCustomer(int id) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.newCustomer(id);
    }

    public boolean newCustomer(int id, int cid) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.newCustomer(id, cid);
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.deleteFlight(id, flightNum);
    }


    public boolean deleteCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.deleteCars(id, location);
    }


    public boolean deleteRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.deleteRooms(id, location);
    }


    public boolean deleteCustomer(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.deleteCustomer(id, customerID);
    }

    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryFlight(id, flightNumber);
    }

    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryCars(id, location);
    }


    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryRooms(id, location);
    }


    public String queryCustomerInfo(int id, int customerID) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryCustomerInfo(id, customerID);
    }


    public int queryFlightPrice(int id, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryFlightPrice(id, flightNumber);
    }


    public int queryCarsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryCarsPrice(id, location);
    }


    public int queryRoomsPrice(int id, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.queryRoomsPrice(id, location);
    }


    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.reserveFlight(id, customerID, flightNumber);
    }


    public boolean reserveCar(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.reserveCar(id, customerID, location);
    }


    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.reserveRoom(id, customerID, location);
    }


    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException, InvalidTransactionException, DeadlockException {
        return txManager.bundle(id, customerID, flightNumbers, location, car, room);
    }

    @Override
    public int start() throws RemoteException {
        return txManager.start();
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        return txManager.commit(transactionId);
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        txManager.abort(transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        if (GetFlightsManager().shutdown() && GetCarsManager().shutdown() && GetRoomsManager().shutdown()) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        return txManager.shutdown();
    }

    @Override
    public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        //fuck off
        throw new RuntimeException("Prepare called in Middleware");
    }

    @Override
    public boolean resetCrashes() throws RemoteException {
        return this.txManager.SetCrashMode(TransactionManagerCrashModes.NONE);
    }

    @Override
    public boolean crashMiddleware(TransactionManagerCrashModes mode) throws RemoteException {
        if (mode == null)
            return false;
        return this.txManager.SetCrashMode(mode);
    }

    @Override
    public boolean crashResourceManager(String name, ResourceManagerCrashModes mode) throws RemoteException {
        if (mode == null)
            return false;
        switch (name) {
            case "Flights":
                return GetFlightsManager().crashResourceManager("Flights", mode);
            case "Cars":
                return GetCarsManager().crashResourceManager("Cars", mode);
            case "Rooms":
                return GetRoomsManager().crashResourceManager("Rooms", mode);
            default:
                System.err.println("Trying to crash a non-existing resourceManager!");
                break;

        }
        return false;
    }

    public String getName() throws RemoteException {
        return middlwareName;
    }
}


