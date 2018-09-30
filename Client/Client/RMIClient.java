package Client;



import java.net.Socket;
import java.io.*;

public class RMIClient extends Client
{
    private static String s_serverHost = "localhost";
    private static int middleware_port = 1099;
    private static String s_serverName = "Middleware";

    private static String s_rmiPrefix = "group16_";

    public static void main(String args[])
    {
        if (args.length > 0)
        {
            s_serverHost = args[0];
        }
        if (args.length > 1)
        {
            s_serverName = args[1];
        }
        if (args.length > 2)
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            RMIClient client = new RMIClient();
            client.connectServer();
            client.start();
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void connectServer()
    {
        try {
            Socket echoSocket = new Socket(s_serverHost, middleware_port);
            PrintWriter  out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));

            m_resourceManager = new ClientSideResourceManager(out, in);

        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

