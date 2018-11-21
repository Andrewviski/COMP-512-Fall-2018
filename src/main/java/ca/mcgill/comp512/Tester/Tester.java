package ca.mcgill.comp512.Tester;

import ca.mcgill.comp512.Client.RMIClient;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static ca.mcgill.comp512.Client.Command.fromString;

class TestCommand<ResponseType extends Object> {
    public String command;
    public ResponseType expectedResponse;
    public ResponseType actualResponse = null;

    public TestCommand(String command, ResponseType response) {
        this.command = command;
        this.expectedResponse = response;
    }

    public boolean Verify(RMIClient client) {
        if (actualResponse == null) {
            List<String> args = Arrays.asList(command.split(","));
            try {
                actualResponse = (ResponseType) client.execute(fromString(args.get(0)), args);
            } catch (Exception e) {
                System.err.println("TestCommand [ " + command + " ] crashed, printing stacktrace...");
                e.printStackTrace();
            }
        }
        if (expectedResponse != null)
            return expectedResponse.equals(actualResponse);
        else
            return actualResponse == null;
    }

    public String toString() {
        return command.toString() + " [ Expecting: " + expectedResponse.toString() + " ]";
    }
}

public class Tester {
    static RMIClient Spawn(String args[]) {
        // Get a client.
        RMIClient tester_client = new RMIClient();
        tester_client.ParseMiddlewareServerConfig(args);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        tester_client.connectServer();
        return tester_client;
    }

    static void TearDown(RMIClient client) {
        try {
            client.execute(fromString("quit"), Arrays.asList("quit"));
        } catch (Exception e) {
            System.err.println("TearDown crashed!");
            e.printStackTrace();
        }
    }
    static int generateXid(RMIClient client){
        try {
            return  (int) client.execute(fromString("start"), Arrays.asList("start"));
        } catch (Exception e) {
            System.err.println("MileStone1Test crashed!");
            e.printStackTrace();
            return -1;
        }
    }
    static List<TestCommand<?>> AddCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        for (String s : flights)
            commands.add(new TestCommand<Boolean>("AddFlight," + xid + "," + s, true));
        for (String s : cars)
            commands.add(new TestCommand<Boolean>("AddCars," + xid + "," + s, true));
        for (String s : rooms)
            commands.add(new TestCommand<Boolean>("AddRooms," + xid + "," + s, true));
        for (String s : customers)
            commands.add(new TestCommand<Boolean>("AddCustomerID," + xid + "," + s, true));
        return commands;
    }

    static List<TestCommand<?>> ReserveCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        for (String s : flights)
            commands.add(new TestCommand<Boolean>("ReserveFlight," + xid + "," + s, true));
        for (String s : cars)
            commands.add(new TestCommand<Boolean>("ReserveCars," + xid + "," + s, true));
        for (String s : rooms)
            commands.add(new TestCommand<Boolean>("ReserveRooms," + xid + "," + s, true));
        return commands;
    }

    static List<TestCommand<?>> DeleteCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        for (String s : flights)
            commands.add(new TestCommand<Boolean>("DeleteFlight," + xid + "," + s, true));
        for (String s : cars)
            commands.add(new TestCommand<Boolean>("DeleteCars," + xid + "," + s, true));
        for (String s : rooms)
            commands.add(new TestCommand<Boolean>("DeleteRooms," + xid + "," + s, true));
        for (String s : customers)
            commands.add(new TestCommand<Boolean>("DeleteCustomer," + xid + "," + s, true));
        return commands;
    }

    static List<TestCommand<?>> QueryCommandsConstructor(int xid, List<Map.Entry<String, Integer>> flights, List<Map.Entry<String, Integer>> cars, List<Map.Entry<String, Integer>> rooms, List<Map.Entry<String, String>> customers) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        for (Map.Entry<String, Integer> e : flights)
            commands.add(new TestCommand<Integer>("QueryFlight," + xid + "," + e.getKey(), e.getValue()));
        for (Map.Entry<String, Integer> e : cars)
            commands.add(new TestCommand<Integer>("QueryCars," + xid + "," + e.getKey(), e.getValue()));
        for (Map.Entry<String, Integer> e : rooms)
            commands.add(new TestCommand<Integer>("QueryRooms," + xid + "," + e.getKey(), e.getValue()));
        for (Map.Entry<String, String> e : customers)
            commands.add(new TestCommand<String>("QueryCustomer," + xid + "," + e.getKey(), e.getValue()));
        return commands;
    }

    static List<TestCommand<?>> QueryPriceCommandsConstructor(int xid, List<Map.Entry<String, Integer>> flights, List<Map.Entry<String, Integer>> cars, List<Map.Entry<String, Integer>> rooms) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        for (Map.Entry<String, Integer> e : flights)
            commands.add(new TestCommand<Integer>("QueryFlightPrice," + xid + "," + e.getKey(), e.getValue()));
        for (Map.Entry<String, Integer> e : cars)
            commands.add(new TestCommand<Integer>("QueryCarsPrice," + xid + "," + e.getKey(), e.getValue()));
        for (Map.Entry<String, Integer> e : rooms)
            commands.add(new TestCommand<Integer>("QueryRoomsPrice," + xid + "," + e.getKey(), e.getValue()));
        return commands;
    }

    static List<TestCommand<?>> AllServersDataAddingStressTest(RMIClient client) {
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        int xid=generateXid(client);
        List<String> flights=Arrays.asList("1,1,1","2,2,2","3,3,3");
        List<String> cars=Arrays.asList("montreal,1,1","paris,2,2","ottawa,3,3");
        List<String> rooms=Arrays.asList("montreal,1,1","paris,2,2","ottawa,3,3");
        List<String> customers=Arrays.asList("1","11","111");
        List<TestCommand<?>> temp= AddCommandsConstructor(xid,flights,cars,rooms,customers);
        temp.add(new TestCommand<Boolean>("Commit," + xid, true));
        return temp;
    }

    static List<TestCommand<?>>  MileStone1Test(RMIClient client){
        List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
        int xid=generateXid(client);
        // 1. Adding data + Distribution
        commands.add(new TestCommand<Boolean>("AddFlight," + xid + ",1,3,10", true));
        commands.add(new TestCommand<Boolean>("AddCars," + xid + ",Montreal,5,20", true));
        commands.add(new TestCommand<Boolean>("AddRooms," + xid + ",Montreal,1,100", true));
        try {
            TimeUnit.MILLISECONDS.sleep(900);
        }catch (Exception e){

        }

        // 2. Querying data + Distribution
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 3));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
        commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 5));
        commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Paris", 0));
        commands.add(new TestCommand<Integer>("QueryFlightPrice," + xid + ",1", 10));


        // 3. Basic Customers
        commands.add(new TestCommand<Boolean>("AddCustomerID," + xid + ",1", true));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", ""));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",100", null));

        // 4. Reservations + Customers (bill)
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 3));
        commands.add(new TestCommand<Boolean>("ReserveFlight," + xid + ",1,1", true));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,"));
        commands.add(new TestCommand<Boolean>("DeleteFlight," + xid + ",1", false));

        commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 5));
        commands.add(new TestCommand<Boolean>("ReserveCar," + xid + ",1,Montreal", true));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,1 car-montreal $20,"));

        // 5. Bundle
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
        commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 4));
        commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 1));
        commands.add(new TestCommand<Boolean>("Bundle," + xid + ",1,1,2,Montreal,true,true", false));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
        commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 4));
        commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 1));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,1 car-montreal $20,"));

        commands.add(new TestCommand<Boolean>("Bundle," + xid + ",1,1,Montreal,true,true", true));
        commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 1));
        commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 3));
        commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 0));
        commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "2 flight-1 $10,1 room-montreal $100,2 car-montreal $20,"));
        commands.add(new TestCommand<Boolean>("Commit," + xid, true));

        return commands;
    }

    boolean RunGenericTest(RMIClient tester_client, List<TestCommand<?>> test) {
        Random rand = new Random();
        for (int i=0;i<test.size();i++) {
            //TestCommand test_command=test.get(rand.nextInt(test.size()-1));
            TestCommand test_command=test.get(i);
            if (!test_command.Verify(tester_client)) {
                System.out.println(test_command.command + "...... Failed!");
                return false;
            } else {
                System.out.println(test_command.command + "...... Passed!");
            }
        }
        return true;
    }

    void StressTest(String[] args,int NClients){
        for(int i=0;i<NClients;i++){
            new Thread(() -> {
                RMIClient tester_client=Spawn(args);
                RunGenericTest(tester_client,AllServersDataAddingStressTest(tester_client));
            }).start();
        }
    }
    public static void main(String args[]) {
        Tester tester = new Tester();
        tester.StressTest(args,2);
    }
}
