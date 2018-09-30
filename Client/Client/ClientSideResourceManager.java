package Client;

import Server.Interface.IResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class ClientSideResourceManager implements IResourceManager {
    private PrintWriter out;
    private BufferedReader in;

    public ClientSideResourceManager(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) {
        String rpc = "addFlight," + Integer.toString(id) + "," + Integer.toString(flightNum)
                + "," +  Integer.toString(flightSeats)+ "," +  Integer.toString(flightPrice);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) {
        String rpc = "addCars," + Integer.toString(id) + "," + location
                + "," +  Integer.toString(numCars)+ "," +  Integer.toString(price);

        out.println(rpc);

        return readBooleanResponse();
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
    public boolean addRooms(int id, String location, int numRooms, int price) {
        String rpc = "addRooms," + Integer.toString(id) + "," + location
                + "," +  Integer.toString(numRooms)+ "," +  Integer.toString(price);

        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public int newCustomer(int id) {
        String rpc = "newCustomer," + Integer.toString(id);

        out.println(rpc);

        return readIntegerResponse();
    }

    @Override
    public boolean newCustomer(int id, int cid) {
        String rpc = "newCustomer," + Integer.toString(id) + "," + Integer.toString(cid);

        out.println(rpc);

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

        return readStringResponse();
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
        String rpc = "reserveCar," + Integer.toString(id) + "," + Integer.toString(customerID) + "," + location;
        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) {
        String rpc = "bundle," + Integer.toString(id) + "," + Integer.toString(customerID) + ",[" + String.join(" ", flightNumbers)
                + "]," + location + "," + String.valueOf(car) + "," + String.valueOf(room);
        out.println(rpc);

        return readBooleanResponse();
    }

    @Override
    public String getName() {
        String rpc = "getName";
        out.println(rpc);

        return readStringResponse();
    }
}
