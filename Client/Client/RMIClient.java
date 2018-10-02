package Client;


import java.net.Socket;
import java.io.*;

public class RMIClient extends Client {
    private static String middlewareHostname = "localhost";
    private static int middleware_port = 54006;
    private static String middlewareName = "Middleware";

    private static String s_rmiPrefix = "group16_";

    private static void ReportClientError(String msg, Exception e) {
        System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m" + msg + "]");
        e.printStackTrace();
        System.exit(1);
    }

    public static void ParseMiddlewareServerConfig(String[] args) {
        if (args.length > 0)
            middlewareHostname = args[0];

        if (args.length > 1) {
            try {
                middleware_port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ReportClientError("Usage: java client.RMIClient [server_hostname] [server_name] [Port]", e);
            }

        }

        if (args.length > 2)
            middlewareName = args[2];

        if (args.length > 3)
            ReportClientError("Too Many arguments", null);
    }

    public static void main(String args[]) {
        ParseMiddlewareServerConfig(args);

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            RMIClient client = new RMIClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            ReportClientError("Uncaught exception", e);
        }
    }

    public void connectServer() {
        try {
            Socket echoSocket = new Socket(middlewareHostname, middleware_port);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));
            resourceManager = new ClientSideResourceManager(out, in);
        } catch (Exception e) {
            ReportClientError("Cannot connect to middlware at(" + middlewareHostname + ":" + middleware_port + ")", e);
        }
    }
}