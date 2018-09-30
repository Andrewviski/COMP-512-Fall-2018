
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerInterface {

    private static class ServerRequest {
        String request;
        PrintWriter clientOut;

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
        queue.add(new ServerRequest(request, clientOut));
    }

    public void startProcessing()
    {
        new Thread(() -> {
            while(true){
                try {
                    ServerRequest request = queue.take();
                    sendRequestToServer(request.request);
                    String response = readReplyFromServer();

                    request.clientOut.println(response);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


}
