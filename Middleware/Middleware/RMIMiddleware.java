package Middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class RMIMiddleware {
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group16_";

    private static final int SERVER_COUNT = 4;

    public static final int middleware_port = 54006;

    // These arrays will store server names, server hostnames, and resource managers for each resources in
    // the following order: Flights, Rooms, Cars, Customers.
    private static String[] server_name = {"Flights", "Rooms", "Cars", "Customers"};
    private static String[] server_hostname = {"localhost", "localhost", "localhost", "localhost"};
    private static ServerInterface[] server_interfaces = new ServerInterface[SERVER_COUNT];
    private static int server_ports[] = {54002, 54003, 54004, 54005};

    // Resource managers accessors.
    public ServerInterface GetFlightsManager() {
        return server_interfaces[0];
    }

    public ServerInterface GetRoomsManager() {
        return server_interfaces[1];
    }

    public ServerInterface GetCarsManager() {
        return server_interfaces[2];
    }

    public ServerInterface GetCustomersManager() {
        return server_interfaces[3];
    }

    private static void ReportMiddleWareError(String msg, Exception e) {
        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0m" + msg+" ]");
        System.exit(1);
    }

    public static void main(String args[]) {
        if (args.length != 0 && args.length != SERVER_COUNT && args.length != SERVER_COUNT * 2) {
            ReportMiddleWareError("Usage: java server.Middleware.RMIMiddleware [flights_server_hostname] [rooms_server_hostname] [cars_server_hostname] [customers_server_hostname]", null);
        }

        try {
            RMIMiddleware middleware = new RMIMiddleware();
            middleware.parseServersConfig(args);
            middleware.connectToServers();
            middleware.listenToClients();
            System.out.println("Middleware is ready and listening on port "+middleware_port);
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

    private void connectToServers() {
        for (int i = 0; i < SERVER_COUNT; i++)
            connectServer(server_hostname[i], server_ports[i], server_name[i], i);
        System.out.println("Middleware up on port " + middleware_port + " and connected to servers on ports: " + Arrays.toString(server_ports));
    }

    public void connectServer(String server_host, int port, String server_name, int resource_manager_index) {

        try {
            Socket echoSocket = new Socket(server_host, port);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));
            server_interfaces[resource_manager_index] = new ServerInterface(out, in);
            server_interfaces[resource_manager_index].startProcessing();
        } catch (Exception e) {
            ReportMiddleWareError("Cannot connect to server "+ server_name + " at( "+server_host+":"+port+" )",e);
        }
    }

    private void listenToClients() {
        try {
            ServerSocket serverSocket = new ServerSocket(middleware_port);

            System.out.println("Middleware up and listening to clients");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> {
                    try {
                        PrintWriter out =
                                new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));

                        while (true) {
                            String request = in.readLine();
                            handleRequest(request, out);
                        }

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



