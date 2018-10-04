package Client;

import Server.Interface.IResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Vector;

public class ClientSideResourceManager implements IResourceManager {
    private PrintWriter out;
    private BufferedReader in;

    public ClientSideResourceManager(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
    }

    private boolean readBooleanResponse() {
        String reply = "";
        try {
            reply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (reply.equals("true")) {
            return true;
        }

        return false;
    }

    private String readStringResponse() {
        String reply = "";
        try {
            reply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return reply;
    }

    private int readIntegerResponse() {
        String reply = "";
        try {
            reply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Integer.parseInt(reply);
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) {
        String rpc = "addFlight," + Integer.toString(id) + "," + Integer.toString(flightNum)
                + "," + Integer.toString(flightSeats) + "," + Integer.toString(flightPrice);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) {
        String rpc = "addCars," + Integer.toString(id) + "," + location
                + "," + Integer.toString(numCars) + "," + Integer.toString(price);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) {
        String rpc = "addRooms," + Integer.toString(id) + "," + location
                + "," + Integer.toString(numRooms) + "," + Integer.toString(price);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public int newCustomer(int id) {
        String rpc = "newCustomer," + Integer.toString(id);

        out.println(rpc);
        readBooleanResponse();
        readBooleanResponse();
        readBooleanResponse();
        return 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) {
        String rpc = "newCustomerID," + Integer.toString(id) + "," + Integer.toString(cid);

        out.println(rpc);

        readBooleanResponse();
        readBooleanResponse();
        return readBooleanResponse();
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) {
        String rpc = "deleteFlight," + Integer.toString(id) + "," + Integer.toString(flightNum);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean deleteCars(int id, String location) {
        String rpc = "deleteCars," + Integer.toString(id) + "," + location;

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        String rpc = "deleteRooms," + Integer.toString(id) + "," + location;

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) {
        String rpc = "deleteCustomer," + Integer.toString(id) + "," + Integer.toString(customerID);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        String rpc = "queryFlight," + Integer.toString(id) + "," + Integer.toString(flightNumber);

        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public int queryCars(int id, String location) {
        String rpc = "queryCars," + Integer.toString(id) + "," + location;

        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public int queryRooms(int id, String location) {
        String rpc = "queryRooms," + Integer.toString(id) + "," + location;
        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) {
        String rpc = "queryCustomerInfo," + Integer.toString(id) + "," + Integer.toString(customerID);
        out.println(rpc);

        String bill1 = String.join("\n", readStringResponse().split(","));
        if (!bill1.equals(""))
            bill1 += "\n";

        String bill2 = String.join("\n", readStringResponse().split(","));
        if (!bill2.equals(""))
            bill2 += "\n";

        String bill3 = String.join("\n", readStringResponse().split(","));
        if (!bill3.equals(""))
            bill3 += "\n";

        if (bill1 == "" && bill2 == "" && bill3 == "")
            return "";
        String bill = "Bill for customer " + customerID + "\n" + bill1 + bill2 + bill3;
        return bill;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        String rpc = "queryFlightPrice," + Integer.toString(id) + "," + Integer.toString(flightNumber);
        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        String rpc = "queryCarsPrice," + Integer.toString(id) + "," + location;
        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        String rpc = "queryRoomsPrice," + Integer.toString(id) + "," + location;
        out.println(rpc);

        return readIntegerResponse();
    }


    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) {
        String rpc = "reserveFlight," + Integer.toString(id) + "," + Integer.toString(customerID) + "," + Integer.toString(flightNumber);
        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) {
        String rpc = "reserveCar," + Integer.toString(id) + "," + Integer.toString(customerID) + "," + location;
        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) {
        String rpc = "reserveRoom," + Integer.toString(id) + "," + Integer.toString(customerID) + "," + location;
        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) {
        // 'Nearly' atomic, will check all conditions before executing.

        // Must check if customer exists
        if (queryCustomerInfo(id, customerID) == "")
            return false;

        boolean can_reserve = true;

        for (String flightNum : flightNumbers) {
            try {
                can_reserve = can_reserve && (queryFlight(id, Integer.parseInt(flightNum)) > 0);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                System.out.println("Flightnum " + flightNum + " is not a number!");
                System.exit(1);
            }
        }

        if (car) {
            can_reserve = (queryCars(id, location) > 0) && can_reserve;
        }

        if (room) {
            can_reserve = (queryRooms(id, location) > 0) && can_reserve;
        }

        boolean res = false;
        if (can_reserve) {
            res = true;

            for (String flightNum : flightNumbers) {
                try {
                    res = reserveFlight(id, customerID, Integer.parseInt(flightNum)) && res;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    System.out.println("Flightnum " + flightNum + " is not a number!");
                    System.exit(1);
                }
            }

            if (car) {
                res = reserveCar(id, customerID, location) && res;
            }

            if (room) {
                res = reserveRoom(id, customerID, location) && res;
            }
        }

        return res;
    }

    @Override
    public String getName() {
        String rpc = "getName";
        out.println(rpc);

        return readStringResponse();
    }
}
