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

public class RMITesterClient extends RMIClient {
    public static List<ClientRequest<?>> test_requests=new ArrayList<ClientRequest<?>>();
    public static void GenerateTestCases(){
        test_requests.add(new ClientRequest<Boolean>("AddFlight,1,1,1,1",true));
    }
    public static void main(String args[]){
        GenerateTestCases();

        RMIClient tester_client=new RMIClient();
        tester_client.ParseMiddlewareServerConfig(args);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        tester_client.connectServer();

        for(ClientRequest<?> request: test_requests){
            List<String> s= Arrays.asList(request.getCommand().split(","));
            Command command=Command.fromString(s.get(0));
            List<String> command_args=s.subList(1, s.size());
            if(tester_client.executeAndReturn(command,command_args)!=request.getResponse()){
                System.out.println(request.toString()+" Failed!");
                System.exit(1);
            }
        }
    }
}
