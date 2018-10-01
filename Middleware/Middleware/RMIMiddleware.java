package Middleware;// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class RMIMiddleware {
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group16_";

    private static final int SERVER_COUNT = 4;

    // This is wrong, every server should have a port number of its own
    public static final int s_serverPort = 1099;

    public static final int middleware_port = 1098;

    // These arrays will store server names, server hostnames, and resource managers for each resources in
    // the following order: Flights, Rooms, Cars, Customers.
    private String[] server_name = {"Flights", "Rooms", "Cars", "Customers"};
    private String[] server_hostname = {"localhost", "localhost", "localhost", "localhost"};
    private ServerInterface[] sever_interfaces = new ServerInterface[SERVER_COUNT];

    // Resource managers accessors.
    public ServerInterface GetFlightsManager() {
        return sever_interfaces[0];
    }

    public ServerInterface GetRoomsManager() {
        return sever_interfaces[1];
    }

    public ServerInterface GetCarsManager() {
        return sever_interfaces[2];
    }

    public ServerInterface GetCustomersManager() {
        return sever_interfaces[3];
    }

    public static void main(String args[]) {
        if (args.length > 4) {
            System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUsage: java server.Middleware.RMIMiddleware [flights_server_hostname] [rooms_server_hostname] [cars_server_hostname] [customers_server_hostname]");
            System.exit(1);
        }

        // Create the RMI server entry
        try {
            // Create a new middleware object
            RMIMiddleware middleware = new RMIMiddleware();

            middleware.connectToServers(args);

            middleware.listenToClients();

            System.out.println("'" + s_serverName + "' middleware server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void connectToServers(String args[]) {
        for (int i = 0; i < SERVER_COUNT; i++) {
            if (i < args.length)
                server_hostname[i] = args[i];
            connectServer(server_hostname[i], s_serverPort, server_name[i], i);
            sever_interfaces[i].startProcessing();
        }

        System.out.println("Middleware up and connected to servers");

    }

    public void connectServer(String server_host, int port, String server_name, int resource_manager_index) {

        try {
            Socket echoSocket = new Socket(server_host, port);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));

            sever_interfaces[resource_manager_index] = new ServerInterface(out, in);
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void listenToClients() {
        try {
            ServerSocket serverSocket = new ServerSocket(1099);

            System.out.println("Middleware up and listening to clients");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> {
                    try {
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));

                        String request = in.readLine();
                        handleRequest(request, out);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(String request, PrintWriter out) {
        String[] parts = request.split(",");

        switch (parts[0]) {
            case "addFlight":
            case "deleteFlight":
            case "queryFlight":
            case "queryFlightPrice":
            case "reserveFlight":
                GetFlightsManager().handleRequest(request, out);
                break;

            case "addCars":
            case "deleteCars":
            case "queryCars":
            case "queryCarsPrice":
            case "reserveCar":
                GetCarsManager().handleRequest(request, out);


                break;

            case "addRooms":
            case "deleteRooms":
            case "queryRooms":
            case "queryRoomsPrice":
            case "reserveRoom":
                GetRoomsManager().handleRequest(request, out);


                break;

            case "newCustomer":
            case "deleteCustomer":
            case "queryCustomerInfo":
                GetCustomersManager().handleRequest(request, out);

                break;

            default:
                throw new IllegalArgumentException("No such method name found " + parts[0]);
        }
    }


}



