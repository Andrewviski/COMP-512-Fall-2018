package Server.RMI;

import Server.Interface.IResourceManager;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Vector;

public class ServerNetworkInterface implements IResourceManager {
    private PrintWriter out;
    private BufferedReader in;

    public ServerNetworkInterface(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) {
        return false;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) {
        return false;
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) {
        return false;
    }

    @Override
    public int newCustomer(int id) {
        return 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) {
        return false;
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) {
        return false;
    }

    @Override
    public boolean deleteCars(int id, String location) {
        return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        return false;
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) {
        return false;
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        return 0;
    }

    @Override
    public int queryCars(int id, String location) {
        return 0;
    }

    @Override
    public int queryRooms(int id, String location) {
        return 0;
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) {
        return null;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        return 0;
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) {
        return false;
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) {
        return false;
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) {
        return false;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }
}
