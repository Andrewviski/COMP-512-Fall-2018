package Middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerInterface {

    private static class ServerRequest {
        private String request;
        private PrintWriter clientOut;

        public ServerRequest(String request, PrintWriter clientOut) {
            this.request = request;
            this.clientOut = clientOut;
        }
    }

    private final PrintWriter out;
    private final BufferedReader in;
    BlockingQueue<ServerRequest> queue = new LinkedBlockingQueue<>();


    public ServerInterface(PrintWriter out, BufferedReader in) {
        this.out = out;
        this.in = in;
    }

    private void sendRequestToServer(String line){
        out.println(line);
    }

    private String readReplyFromServer(){
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public void handleRequest(String request, PrintWriter clientOut)
    {
        ServerRequest r=new ServerRequest(request, clientOut);
        sendRequestToServer(r.request);
        String response = readReplyFromServer();
        clientOut.println(response);
    }
}
