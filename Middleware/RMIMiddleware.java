// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.IResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;


public class RMIMiddleware implements IResourceManager {
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group16_";

    private static final int SERVER_COUNT=4;
    public static final int s_serverPort = 1099;

    // These arrays will store server names, server hostnames, and resource managers for each resources in
    // the following order: Flights, Rooms, Cars, Customers.
    private String[] server_name={"Flights","Rooms","Cars","Customers"};
    private String[] server_hostname={"localhost","localhost","localhost","localhost"};
    private IResourceManager[] resource_managers = new IResourceManager[SERVER_COUNT];

    // Resource managers accessors.
    public IResourceManager GetFlightsManager(){
        return resource_managers[0];
    }

    public IResourceManager GetRoomsManager(){
        return resource_managers[1];
    }

    public IResourceManager GetCarsManager(){
        return resource_managers[2];
    }

    public IResourceManager GetCustomersManager(){
        return resource_managers[3];
    }

    public static void main(String args[]) {
        if (args.length > 4) {
            System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUsage: java server.RMIMiddleware [flights_server_hostname] [rooms_server_hostname] [cars_server_hostname] [customers_server_hostname]");
            System.exit(1);
        }

        // Create the RMI server entry
        try {
            // Create a new middleware object
            RMIMiddleware middleware=new RMIMiddleware();

            middleware.connectToServers(args);
            System.out.println("'" + s_serverName + "' middleware server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void connectToServers(String args[]){
        for(int i=0;i<SERVER_COUNT;i++) {
            if(i< args.length)
                server_hostname[i]=args[i];
            connectServer(server_hostname[i], s_serverPort, server_name[i], i);
        }
    }

    public void connectServer(String server_host, int port, String server_name, int resource_manager_index) {

        try {
            Socket echoSocket = new Socket(server_host, port);
            PrintWriter  out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));

            resource_managers[resource_manager_index] = new ServerNetworkInterface(out, in);
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // IResourceManage implementation
    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)  {
        return GetFlightsManager().addFlight(id,flightNum,flightSeats,flightPrice);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price)  {
        return GetCarsManager().addCars(id,location,numCars,price);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price)  {
        return GetRoomsManager().addRooms(id,location,numRooms,price);
    }

    @Override
    public int newCustomer(int id)  {
        return GetCustomersManager().newCustomer(id);
    }

    @Override
    public boolean newCustomer(int id, int cid)  {
        return  GetCustomersManager().newCustomer(id,cid);
    }

    @Override
    public boolean deleteFlight(int id, int flightNum)  {
        return GetFlightsManager().deleteFlight(id,flightNum);
    }

    @Override
    public boolean deleteCars(int id, String location)  {
        return GetCarsManager().deleteCars(id,location);
    }

    @Override
    public boolean deleteRooms(int id, String location)  {
        return GetRoomsManager().deleteRooms(id,location);
    }

    @Override
    public boolean deleteCustomer(int id, int customerID)  {
        return GetCustomersManager().deleteCustomer(id,customerID);
    }

    @Override
    public int queryFlight(int id, int flightNumber)  {
        return GetFlightsManager().queryFlight(id,flightNumber);
    }

    @Override
    public int queryCars(int id, String location)  {
        return GetCarsManager().queryCars(id,location);
    }

    @Override
    public int queryRooms(int id, String location)  {
        return GetRoomsManager().queryRooms(id,location);
    }

    @Override
    public String queryCustomerInfo(int id, int customerID)  {
        return GetCustomersManager().queryCustomerInfo(id,customerID);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber)  {
        return GetFlightsManager().queryFlightPrice(id,flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location)  {
        return GetCarsManager().queryCarsPrice(id,location);
    }

    @Override
    public int queryRoomsPrice(int id, String location)  {
        return GetRoomsManager().queryRoomsPrice(id,location);
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber)  {
        return GetFlightsManager().reserveFlight(id,customerID,flightNumber);
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location)  {
        return GetCarsManager().reserveCar(id,customerID,location);
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location)  {
        return GetRoomsManager().reserveRoom(id,customerID,location);
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)  {

        // Reserve all flights.
        for(String s_flightnumber: flightNumbers) {
            int flightnumber;
            try {
                flightnumber = Integer.parseInt(s_flightnumber);
            } catch (NumberFormatException e) {
                return false;
            }
            if (!GetFlightsManager().reserveFlight(id, customerID, flightnumber))
                return false;
        }

        if(car && !GetCarsManager().reserveCar(id,customerID,location)){
            return false;
        }

        if(room && !GetRoomsManager().reserveRoom(id,customerID,location)){
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return s_serverName;
    }
}
