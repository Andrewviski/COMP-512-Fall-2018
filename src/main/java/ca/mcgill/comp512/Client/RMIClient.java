package ca.mcgill.comp512.Client;

import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient extends Client {
    private static String middlewareHostname = "localhost";
    private static int middlewarePort = 54006;
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
                middlewarePort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ReportClientError("Usage: java client.RMIClient [middleware_hostname] [Port] [middleware_name]", e);
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
            boolean firstAttempt = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(middlewareHostname, middlewarePort);
                    resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + middlewareName);
                    System.out.println("Connected to middleware server [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "]");
                    break;
                } catch (NotBoundException | RemoteException e) {
                    if (firstAttempt) {
                        ReportClientError("Waiting for middleware server [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "]", e);
                        firstAttempt = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            ReportClientError("Cannot connect to middlware at(" + middlewareHostname + ":" + middlewarePort + ")", e);
        }
    }
}