package Client;

import LockManager.TransactionAbortedException;
import Middleware.InvalidTransactionException;
import Server.Interface.*;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;


public abstract class Client {
    IResourceManager resourceManager = null;

    public abstract void connectServer();

    public void start() {
        // Prepare for reading commands
        System.out.println();
        System.out.println("Location \"help\" for list of supported commands");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // Read the next command
            String command = "";
            Vector<String> arguments = new Vector<String>();
            try {
                System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
                command = stdin.readLine().trim();
            } catch (IOException io) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m" + io.getLocalizedMessage());
                io.printStackTrace();
                System.exit(1);
            }

            try {
                arguments = parse(command);
                Command cmd = Command.fromString((String) arguments.get(0));
                try {
                    execute(cmd, arguments);
                } catch (Exception e) {
                    connectServer();
                    execute(cmd, arguments);
                }
            } catch (IllegalArgumentException e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mUncaught exception");
                e.printStackTrace();
            }
        }
    }

    public Object execute(Command cmd, List<String> arguments) throws NumberFormatException, RemoteException {
        try {
            switch (cmd) {
                case Help: {
                    if (arguments.size() == 1) {
                        return Command.description();
                    } else if (arguments.size() == 2) {
                        Command l_cmd = Command.fromString((String) arguments.get(1));
                        return l_cmd.toString();
                    } else {
                        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
                    }
                    break;
                }
                case AddFlight: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding a new flight [xid=" + arguments.get(1) + "]");
                    System.out.println("-Flight Number: " + arguments.get(2));
                    System.out.println("-Flight Seats: " + arguments.get(3));
                    System.out.println("-Flight Price: " + arguments.get(4));

                    int id = toInt(arguments.get(1));
                    int flightNum = toInt(arguments.get(2));
                    int flightSeats = toInt(arguments.get(3));
                    int flightPrice = toInt(arguments.get(4));

                    if (resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
                        System.out.println("Flight added");
                        return true;
                    } else {
                        System.out.println("Flight could not be added");
                        return false;
                    }
                }
                case AddCars: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding new cars [xid=" + arguments.get(1) + "]");
                    System.out.println("-Car Location: " + arguments.get(2));
                    System.out.println("-Number of Cars: " + arguments.get(3));
                    System.out.println("-Car Price: " + arguments.get(4));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);
                    int numCars = toInt(arguments.get(3));
                    int price = toInt(arguments.get(4));

                    if (resourceManager.addCars(id, location, numCars, price)) {
                        System.out.println("Cars added");
                        return true;
                    } else {
                        System.out.println("Cars could not be added");
                        return false;
                    }
                }
                case AddRooms: {
                    checkArgumentsCount(5, arguments.size());

                    System.out.println("Adding new rooms [xid=" + arguments.get(1) + "]");
                    System.out.println("-Room Location: " + arguments.get(2));
                    System.out.println("-Number of Rooms: " + arguments.get(3));
                    System.out.println("-Room Price: " + arguments.get(4));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);
                    int numRooms = toInt(arguments.get(3));
                    int price = toInt(arguments.get(4));

                    if (resourceManager.addRooms(id, location, numRooms, price)) {
                        System.out.println("Rooms added");
                        return true;
                    } else {
                        System.out.println("Rooms could not be added");
                        return false;
                    }
                }
                case AddCustomer: {
                    checkArgumentsCount(2, arguments.size());

                    System.out.println("Adding a new customer [xid=" + arguments.get(1) + "]");

                    int id = toInt(arguments.get(1));
                    int customer = resourceManager.newCustomer(id);

                    System.out.println("Add customer ID: " + customer);
                    return customer;
                }
                case AddCustomerID: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Adding a new customer [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));

                    if (resourceManager.newCustomer(id, customerID)) {
                        System.out.println("Add customer ID: " + customerID);
                        return true;
                    } else {
                        System.out.println("Customer could not be added");
                        return false;
                    }
                }
                case DeleteFlight: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting a flight [xid=" + arguments.get(1) + "]");
                    System.out.println("-Flight Number: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int flightNum = toInt(arguments.get(2));

                    if (resourceManager.deleteFlight(id, flightNum)) {
                        System.out.println("Flight Deleted");
                        return true;
                    } else {
                        System.out.println("Flight could not be deleted");
                        return false;
                    }
                }
                case DeleteCars: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting all cars at a particular location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Car Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    if (resourceManager.deleteCars(id, location)) {
                        System.out.println("Cars Deleted");
                        return true;
                    } else {
                        System.out.println("Cars could not be deleted");
                        return false;
                    }
                }
                case DeleteRooms: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting all rooms at a particular location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Car Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    if (resourceManager.deleteRooms(id, location)) {
                        System.out.println("Rooms Deleted");
                        return true;
                    } else {
                        System.out.println("Rooms could not be deleted");
                        return false;
                    }
                }
                case DeleteCustomer: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Deleting a customer from the database [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));

                    if (resourceManager.deleteCustomer(id, customerID)) {
                        System.out.println("Customer Deleted");
                        return true;
                    } else {
                        System.out.println("Customer could not be deleted");
                        return false;
                    }
                }
                case QueryFlight: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying a flight [xid=" + arguments.get(1) + "]");
                    System.out.println("-Flight Number: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int flightNum = toInt(arguments.get(2));

                    int seats = resourceManager.queryFlight(id, flightNum);
                    System.out.println("Number of seats available: " + seats);
                    return seats;
                }
                case QueryCars: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying cars location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Car Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    int numCars = resourceManager.queryCars(id, location);
                    System.out.println("Number of cars at this location: " + numCars);
                    return numCars;
                }
                case QueryRooms: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying rooms location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Room Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    int numRoom = resourceManager.queryRooms(id, location);
                    System.out.println("Number of rooms at this location: " + numRoom);
                    return numRoom;
                }
                case QueryCustomer: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying customer information [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));

                    String bill = resourceManager.queryCustomerInfo(id, customerID);
                    System.out.print(bill);
                    return bill;
                }
                case QueryFlightPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying a flight price [xid=" + arguments.get(1) + "]");
                    System.out.println("-Flight Number: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    int flightNum = toInt(arguments.get(2));

                    int price = resourceManager.queryFlightPrice(id, flightNum);
                    System.out.println("Price of a seat: " + price);
                    return price;
                }
                case QueryCarsPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying cars price [xid=" + arguments.get(1) + "]");
                    System.out.println("-Car Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    int price = resourceManager.queryCarsPrice(id, location);
                    System.out.println("Price of cars at this location: " + price);
                    return price;
                }
                case QueryRoomsPrice: {
                    checkArgumentsCount(3, arguments.size());

                    System.out.println("Querying rooms price [xid=" + arguments.get(1) + "]");
                    System.out.println("-Room Location: " + arguments.get(2));

                    int id = toInt(arguments.get(1));
                    String location = arguments.get(2);

                    int price = resourceManager.queryRoomsPrice(id, location);
                    System.out.println("Price of rooms at this location: " + price);
                    return price;
                }
                case ReserveFlight: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving seat in a flight [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));
                    System.out.println("-Flight Number: " + arguments.get(3));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));
                    int flightNum = toInt(arguments.get(3));

                    if (resourceManager.reserveFlight(id, customerID, flightNum)) {
                        System.out.println("Flight Reserved");
                        return true;
                    } else {
                        System.out.println("Flight could not be reserved");
                        return false;
                    }
                }
                case ReserveCar: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving a car at a location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));
                    System.out.println("-Car Location: " + arguments.get(3));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));
                    String location = arguments.get(3);

                    if (resourceManager.reserveCar(id, customerID, location)) {
                        System.out.println("Car Reserved");
                        return true;
                    } else {
                        System.out.println("Car could not be reserved");
                        return false;
                    }
                }
                case ReserveRoom: {
                    checkArgumentsCount(4, arguments.size());

                    System.out.println("Reserving a room at a location [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));
                    System.out.println("-Room Location: " + arguments.get(3));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));
                    String location = arguments.get(3);

                    if (resourceManager.reserveRoom(id, customerID, location)) {
                        System.out.println("Room Reserved");
                        return true;
                    } else {
                        System.out.println("Room could not be reserved");
                        return false;
                    }
                }
                case Bundle: {
                    if (arguments.size() < 7) {
                        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
                        break;
                    }

                    System.out.println("Reserving an bundle [xid=" + arguments.get(1) + "]");
                    System.out.println("-Customer ID: " + arguments.get(2));
                    for (int i = 0; i < arguments.size() - 6; ++i) {
                        System.out.println("-Flight Number: " + arguments.get(3 + i));
                    }
                    System.out.println("-Location for Car/Room: " + arguments.get(arguments.size() - 3));
                    System.out.println("-Book Car: " + arguments.get(arguments.size() - 2));
                    System.out.println("-Book Room: " + arguments.get(arguments.size() - 1));

                    int id = toInt(arguments.get(1));
                    int customerID = toInt(arguments.get(2));
                    Vector<String> flightNumbers = new Vector<String>();
                    for (int i = 0; i < arguments.size() - 6; ++i) {
                        flightNumbers.addElement(arguments.get(3 + i));
                    }
                    String location = arguments.get(arguments.size() - 3);
                    boolean car = toBoolean(arguments.get(arguments.size() - 2));
                    boolean room = toBoolean(arguments.get(arguments.size() - 1));

                    if (resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
                        System.out.println("Bundle Reserved");
                        return true;
                    } else {
                        System.out.println("Bundle could not be reserved");
                        return false;
                    }
                }
                case Quit:
                    checkArgumentsCount(1, arguments.size());

                    System.out.println("Quitting client");
                    System.exit(0);
                    return "Quitting client";

                case Start:
                    checkArgumentsCount(1, arguments.size());

                    System.out.println("Requesting a new transaction id.");
                    int xid = resourceManager.start();

                    System.out.println("New xid: " + xid);
                    return xid;
                case Commit:
                    checkArgumentsCount(2, arguments.size());
                    int commit_id=toInt(arguments.get(1));
                    if (resourceManager.commit(commit_id)) {
                        System.out.println("Transcation commited");
                        return true;
                    } else {
                        System.out.println("Transcation could not be commited");
                        return false;
                    }
                case Abort:
                    checkArgumentsCount(2, arguments.size());
                    int abort_id=toInt(arguments.get(1));
                    resourceManager.abort(abort_id);
                    System.out.println("Transcation aborted");
                    return true;

                case Shutdown:
                    checkArgumentsCount(1, arguments.size());

                    if (resourceManager.shutdown()) {
                        System.out.println("System has shutdown");
                        System.exit(0);
                        return true;
                    } else {
                        System.out.println("System could not shutdown");
                        return false;
                    }
            }
        } catch (InvalidTransactionException e) {
            System.out.println(e.getMessage());
            System.out.println("Please try with a different xid...");
        } catch (TransactionAbortedException e) {
            System.out.println(e.getMessage());
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    public static Vector<String> parse(String command) {
        Vector<String> arguments = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException {
        if (expected != actual) {
            throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
        }
    }

    public static int toInt(String string) throws NumberFormatException {
        return (new Integer(string)).intValue();
    }

    public static boolean toBoolean(String string)// throws Exception
    {
        return (new Boolean(string)).booleanValue();
    }
}
