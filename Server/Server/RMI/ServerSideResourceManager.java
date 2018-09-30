// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Common.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerSideResourceManager extends ResourceManager {
    private static String s_serverName = "Server";
    private static String s_rmiPrefix = "group16_";


    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverName = args[0];
        }

        // Create the RMI server entry
        try {
            // Create a new Server object
            ServerSideResourceManager server = new ServerSideResourceManager(s_serverName);
            ServerSocket serverSocket = new ServerSocket(1099);

            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String request = in.readLine();


                server.processRequest(request, out);
            }


        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

    }

    private void processRequest(String request, PrintWriter out) {
        String[] parts = request.split(",");
        String methodName = parts[0];

        boolean boolean_return;
        String string_return;
        int int_return;

        switch (methodName) {
            case "addFlight":
                boolean_return = addFlight(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                out.println(boolean_return);
                break;

            case "addCars":
                boolean_return = addCars(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                out.println(boolean_return);
                break;

            case "addRooms":
                boolean_return = addRooms(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                out.println(boolean_return);
                break;

            case "newCustomer":
                if (parts.length == 2) {
                    int_return = newCustomer(Integer.parseInt(parts[1]));
                    out.println(int_return);
                } else if (parts.length == 3) {
                    boolean_return = newCustomer(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    out.println(boolean_return);
                }

                break;

            case "deleteFlight":
                boolean_return = deleteFlight(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                out.println(boolean_return);
                break;

            case "deleteCars":
                boolean_return = deleteCars(Integer.parseInt(parts[1]), parts[2]);
                out.println(boolean_return);
                break;

            case "deleteRooms":
                boolean_return = deleteRooms(Integer.parseInt(parts[1]), parts[2]);
                out.println(boolean_return);
                break;

            case "deleteCustomer":
                boolean_return = deleteCustomer(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                out.println(boolean_return);
                break;

            case "queryFlight":
                int_return = queryFlight(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                out.println(int_return);
                break;

            case "queryCars":
                int_return = queryCars(Integer.parseInt(parts[1]), parts[2]);
                out.println(int_return);
                break;

            case "queryRooms":
                int_return = queryRooms(Integer.parseInt(parts[1]), parts[2]);
                out.println(int_return);
                break;

            case "queryCustomerInfo":
                string_return = queryCustomerInfo(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                out.println(string_return);
                break;

            case "queryFlightPrice":
                int_return = queryFlightPrice(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                out.println(int_return);
                break;

            case "queryCarsPrice":
                int_return = queryCarsPrice(Integer.parseInt(parts[1]), parts[2]);
                out.println(int_return);
                break;

            case "queryRoomsPrice":
                int_return = queryRoomsPrice(Integer.parseInt(parts[1]), parts[2]);
                out.println(int_return);
                break;

            case "reserveFlight":
                boolean_return = reserveFlight(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                out.println(boolean_return);
                break;

            case "reserveCar":
                boolean_return = reserveCar(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]);
                out.println(boolean_return);
                break;

            case "reserveRoom":
                boolean_return = reserveRoom(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]);
                out.println(boolean_return);
                break;

            case "bundle":

            case "getName":
                string_return = getName();
                out.println(string_return);
                break;

            default:
                throw new IllegalArgumentException("No such method name found " + methodName);
        }
    }

    public ServerSideResourceManager(String name) {
        super(name);
    }
}
