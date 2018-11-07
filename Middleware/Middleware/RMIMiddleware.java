package Middleware;

import Server.Interface.IResourceManager;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Vector;

public class RMIMiddleware implements IResourceManager {
    
    private static String middlwareName = "Middleware";
    private static String s_rmiPrefix = "group16_";

    private static final int SERVER_COUNT = 3;

    public static final int middlewarePort = 54006;

    // These arrays will store server names, server hostnames, and resource managers for each resources in
    // the following order: Flights, Rooms, Cars, Customers.
    private static String[] server_name = {"Flights", "Rooms", "Cars"};
    private static String[] server_hostname = {"localhost", "localhost", "localhost"};
    private static int server_ports[] = {54002, 54003, 54004};
    private static IResourceManager[] resourceManagers = new IResourceManager[SERVER_COUNT];
    private static HashSet<Integer> aliveCustomerIds=new HashSet<>();

    // Resource managers accessors.
    public IResourceManager GetFlightsManager() {
        return resourceManagers[0];
    }

    public IResourceManager GetRoomsManager() {
        return resourceManagers[1];
    }

    public IResourceManager GetCarsManager() {
        return resourceManagers[2];
    }

    private static void ReportMiddleWareError(String msg, Exception e) {
        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0m" + msg+" ]");
        System.exit(1);
    }

    public static void main(String args[]) {
        if (args.length != 0 && args.length != SERVER_COUNT && args.length != SERVER_COUNT * 2) {

            ReportMiddleWareError("We got "+Integer.toString(args.length)+ "Args, Usage: java server.Middleware.RMIMiddleware [flights_server_hostname] [rooms_server_hostname] [cars_server_hostname]", null);
        }

        try {
            RMIMiddleware middleware=new RMIMiddleware();
            middleware.parseServersConfig(args);
            middleware.setup();
            middleware.connectToServers();
            System.out.println("Middleware is ready and listening on port "+middlewarePort);
        } catch (Exception e) {
            ReportMiddleWareError("Uncaught Exception", e);
        }
    }

    private void parseServersConfig(String args[]) {
        if(args.length==0)
            return;
        // Parse hostnames and port numbers.
        for (int i = 0; i < SERVER_COUNT; i++) {
            server_hostname[i] = args[i];
            if (args.length > 4) {
                try {
                    server_ports[i] = Integer.parseInt(args[SERVER_COUNT + i]);
                } catch (NumberFormatException e) {
                    ReportMiddleWareError("One of the specified ports is not a number!", e);
                }
            }
        }
    }

    private void setup(){
        // Create the RMI server entry.
        try {
            // Create a new Server object.
            // IResourceManager server = new RMIResourceManager(middlwareName);

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

    private void connectToServers() {
        for (int i = 0; i < SERVER_COUNT; i++)
            connectServer(server_hostname[i], server_ports[i], server_name[i], i);
        System.out.println("Middleware up on port " + middlewarePort + " and connected to servers on ports: " + Arrays.toString(server_ports));
    }

    public void connectServer(String server_host, int port, String server_name, int resource_manager_index) {
        try {
            boolean firstAttempt = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server_host, port);
                    resourceManagers[resource_manager_index] = (IResourceManager)registry.lookup(s_rmiPrefix + server_name);
                    System.out.println("Connected to server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_name + "]");
                    break;
                }
                catch (NotBoundException |RemoteException e) {
                    if (firstAttempt) {
                        ReportMiddleWareError("Waiting for Server [" + server_host + ":" + port + "/" + s_rmiPrefix + server_name + "]",e);
                        firstAttempt = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            ReportMiddleWareError("Cannot connect to Server at(" + server_host + ":" + port + ")", e);
        }
    }
    
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return GetFlightsManager().addFlight(id,flightNum,flightSeats,flightPrice);
    }
    
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return GetCarsManager().addCars(id,location,numCars,price);
    }
    
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return GetRoomsManager().addRooms(id,location,numRooms,price);
    }
    
    public int newCustomer(int id) throws RemoteException {
        int newCid=Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        if(newCustomer(id,newCid))
            return newCid;

        // 0 Indicates failure.
        return 0;
    }
    
    public boolean newCustomer(int id, int cid) throws RemoteException {
        if(GetRoomsManager().newCustomer(id,cid) && GetCarsManager().newCustomer(id,cid) && GetFlightsManager().newCustomer(id,cid)){
            aliveCustomerIds.add(cid);
            return true;
        }
        return false;
    }
    
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return GetFlightsManager().deleteFlight(id,flightNum);
    }

    
    public boolean deleteCars(int id, String location) throws RemoteException {
        return GetCarsManager().deleteCars(id,location);
    }

    
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return GetRoomsManager().deleteRooms(id, location);
    }

    
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        return (GetRoomsManager().deleteCustomer(id,customerID) && GetCarsManager().deleteCustomer(id,customerID) &&GetFlightsManager().deleteCustomer(id,customerID));
    }

    
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        return GetFlightsManager().queryFlight(id,flightNumber);
    }

    
    public int queryCars(int id, String location) throws RemoteException {
        return GetCarsManager().queryCars(id,location);
    }

    
    public int queryRooms(int id, String location) throws RemoteException {
        return GetRoomsManager().queryRooms(id,location);
    }

    
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        return GetFlightsManager().queryCustomerInfo(id,customerID) + GetRoomsManager().queryCustomerInfo(id,customerID) + GetCarsManager().queryCustomerInfo(id,customerID);
    }

    
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        return GetFlightsManager().queryFlightPrice(id,flightNumber);
    }

    
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return GetCarsManager().queryCarsPrice(id,location);
    }

    
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return GetRoomsManager().queryRoomsPrice(id,location);
    }

    
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        return GetFlightsManager().reserveFlight(id,customerID,flightNumber);
    }

    
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        return GetCarsManager().reserveCar(id,customerID,location);
    }

    
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        return GetRoomsManager().reserveRoom(id,customerID,location);
    }

    
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        // Need to implement transactions.
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

    
    public String getName() throws RemoteException {
        return middlwareName;
    }
}



