package Client;

import java.util.*;

class ClientRequest<ResponseType extends Object> {
    private String command;
    private ResponseType response;
    public ClientRequest(String command, ResponseType response){
        this.command = command;
        this.response = response;
    }
    public String getCommand(){ return command; }
    public ResponseType getResponse(){ return response; }
    public void setCommand(String command){ this.command = command; }
    public void setResponse(ResponseType response){ this.response = response; }
}

class UnitTests {
    public List<List<ClientRequest<?>>>  tests=new ArrayList<>();
    void AddTest(List<ClientRequest<?>> requests){
        tests.add(requests);
    }

    UnitTests(){
        GenerateUnitTests();
    }

    void GenerateUnitTests(){
        List<ClientRequest<?>> test_requests=new ArrayList<ClientRequest<?>>();

        // Test Bundle
        test_requests.add(new ClientRequest<Boolean>("AddCustomerID,1,101",true));
        test_requests.add(new ClientRequest<Boolean>("AddFlight,1,1,1,5",true));
        test_requests.add(new ClientRequest<Boolean>("AddFlight,1,2,1,20",true));
        test_requests.add(new ClientRequest<Boolean>("AddCars,1,montreal,1,5",true));
        test_requests.add(new ClientRequest<Boolean>("AddRooms,1,montreal,1,10",true));
        test_requests.add(new ClientRequest<Boolean>("Bundle,1,101,1,2,montreal,1,1",true));
        AddTest(test_requests);
    }
}

public class RMITesterClient extends RMIClient {
    static RMIClient  SetUp(String args[]){
        // Get a client.
        RMIClient tester_client = new RMIClient();
        tester_client.ParseMiddlewareServerConfig(args);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        tester_client.connectServer();
        return tester_client;
    }

    static void TearDown(){

    }

    public static void main(String args[]){
        UnitTests unittests=new UnitTests();

        for(List<ClientRequest<?>> test: unittests.tests) {
            RMIClient tester_client=SetUp(args);

            for(ClientRequest<?> request: test){
                List<String> command_args= Arrays.asList(request.getCommand().split(","));
                Command command=Command.fromString(command_args.get(0));
                if(!tester_client.executeAndReturn(command,command_args).equals(request.getResponse())){
                    System.out.println(request.toString()+"...... Failed!");
                    System.exit(1);
                }else{
                    System.out.println(request.toString()+"...... Passed!");
                }
            }
            TearDown();
        }
    }
}
