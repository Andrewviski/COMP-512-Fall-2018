package Client;

import java.util.*;

class ClientRequest<ResponseType extends Object> {
    private String command;
    private ResponseType response;

    public ClientRequest(String command, ResponseType response) {
        this.command = command;
        this.response = response;
    }

    public String getCommand() {
        return command;
    }

    public ResponseType getResponse() {
        return response;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setResponse(ResponseType response) {
        this.response = response;
    }

    public String toString(){
        return command.toString()+" [ Expecting: "+response.toString()+" ]";
    }
}

class UnitTests {
    public List<List<ClientRequest<?>>> tests = new ArrayList<>();

    void AddTest(List<ClientRequest<?>> requests) {
        tests.add(requests);
    }

    UnitTests() {
        GenerateUnitTests1();
    }


    // Test provided by Alex for part 1
    void GenerateUnitTests1() {
        List<ClientRequest<?>> test_requests = new ArrayList<ClientRequest<?>>();


        // 1. Adding data + Distribution
        test_requests.add(new ClientRequest<Boolean>("AddFlight,0,1,3,10", true));
        test_requests.add(new ClientRequest<Boolean>("AddCars,0,Montreal,5,20", true));
        test_requests.add(new ClientRequest<Boolean>("AddRooms,0,Montreal,1,100", true));


        // 2. Querying data + Distribution
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 3));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,2", 0));
        test_requests.add(new ClientRequest<Integer>("QueryCars,0,Montreal", 5));
        test_requests.add(new ClientRequest<Integer>("QueryRooms,0,Paris", 0));
        test_requests.add(new ClientRequest<Integer>("QueryFlightPrice,0,1", 10));


        // 3. Basic Customers
        test_requests.add(new ClientRequest<Boolean>("AddCustomerID,0,1", true));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,1",""));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,100", ""));

        // 4. Reservations + Customers (bill)
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 3));
        test_requests.add(new ClientRequest<Boolean>("ReserveFlight,0,1,1", true));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 2));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,1", "1 flight-1 $10,"));
        test_requests.add(new ClientRequest<Boolean>("DeleteFlight,0,1", false));

        test_requests.add(new ClientRequest<Integer>("QueryCars,0,Montreal", 5));
        test_requests.add(new ClientRequest<Boolean>("ReserveCar,0,1,Montreal", true));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,1", "1 flight-1 $10,1 car-montreal $20,"));

        // 5. Bundle
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 2));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,2", 0));
        test_requests.add(new ClientRequest<Integer>("QueryCars,0,Montreal", 4));
        test_requests.add(new ClientRequest<Integer>("QueryRooms,0,Montreal", 1));
        test_requests.add(new ClientRequest<Boolean>("Bundle,0,1,1,2,Montreal,true,true", false));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 2));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,2", 0));
        test_requests.add(new ClientRequest<Integer>("QueryCars,0,Montreal", 4));
        test_requests.add(new ClientRequest<Integer>("QueryRooms,0,Montreal", 1));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,1", "1 flight-1 $10,1 car-montreal $20,"));

        test_requests.add(new ClientRequest<Boolean>("Bundle,0,1,1,Montreal,true,true", true));
        test_requests.add(new ClientRequest<Integer>("QueryFlight,0,1", 1));
        test_requests.add(new ClientRequest<Integer>("QueryCars,0,Montreal", 3));
        test_requests.add(new ClientRequest<Integer>("QueryRooms,0,Montreal", 0));
        test_requests.add(new ClientRequest<String>("QueryCustomer,0,1", "2 flight-1 $10,1 room-montreal $100,2 car-montreal $20,"));
        AddTest(test_requests);
    }

    // Our own tests for part 1 of the project.
    void GenerateUnitTests2() {
        List<ClientRequest<?>> test_requests = new ArrayList<ClientRequest<?>>();

        // Custom Test Bundle
        test_requests.add(new ClientRequest<Boolean>("AddCustomerID,1,101", true));
        test_requests.add(new ClientRequest<Boolean>("AddFlight,1,1,1,5", true));
        test_requests.add(new ClientRequest<Boolean>("AddFlight,1,2,1,20", true));
        test_requests.add(new ClientRequest<Boolean>("AddCars,1,montreal,1,5", true));
        test_requests.add(new ClientRequest<Boolean>("AddRooms,1,montreal,1,10", true));
        test_requests.add(new ClientRequest<Boolean>("Bundle,1,101,1,2,montreal,1,1", true));
        AddTest(test_requests);
    }
}

public class Tester {
    static RMIClient SetUp(String args[]) {
        // Get a client.
        RMIClient tester_client = new RMIClient();
        tester_client.ParseMiddlewareServerConfig(args);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        tester_client.connectServer();
        return tester_client;
    }

    static void TearDown() {

    }

    public static void main(String args[]) {
        UnitTests unittests = new UnitTests();

        for (List<ClientRequest<?>> test : unittests.tests) {
            RMIClient tester_client = SetUp(args);

            for (ClientRequest<?> request : test) {
                List<String> command_args = Arrays.asList(request.getCommand().split(","));
                Command command = Command.fromString(command_args.get(0));
                try {
                    if (!tester_client.execute(command, command_args).equals(request.getResponse())) {
                        System.out.println(request.toString() + "...... Failed!");
                        System.exit(1);
                    } else {
                        System.out.println(request.toString() + "...... Passed!");
                    }
                } catch(Exception e){
                    System.err.println("Client.Tester crashed, printing stacktrace...");
                    e.printStackTrace();
                }
            }
            TearDown();
        }
    }
}
