package ca.mcgill.comp512.Client;

import ca.mcgill.comp512.Server.Interface.IResourceManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;

public class RMIClient extends Client {
    private static String s_rmiPrefix = "group16_";
    private static int HEARTBEAT_FREQUENCY = 2000;


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
            client.searchForMiddleware();

            // Create a heartbeat thread
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(HEARTBEAT_FREQUENCY);
                        if (!client.middleware_dead.get()) {
                            try {
                                client.resourceManager.getName();
                            } catch (Exception e) {
                                client.middleware_dead.set(true);
                                System.out.println("\n"+middlewareName + " died, disconnecting!");
                            }
                        } else {
                            client.connectMiddleware();
                            if (!client.middleware_dead.get()) {
                                System.out.println(middlewareName + " is alive, reconnected!");
                                System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Heartbreat exception at client");
                    }
                }
            }).start();

            client.start();
        } catch (Exception e) {
            ReportClientError("Uncaught exception", e);
        }
    }

    public void searchForMiddleware() {
        try {
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(middlewareHostname, middlewarePort);
                    resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + middlewareName);
                    System.out.println("Connected to middleware server [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "]");
                    middleware_dead.set(false);
                    return;
                } catch (Exception e) {
                    System.out.println("Waiting for middleware server [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "]");
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            ReportClientError("Cannot connect to middlware at(" + middlewareHostname + ":" + middlewarePort + ")", e);
        }
        middleware_dead.set(true);
    }

    public void connectMiddleware() {
        try {
            Registry registry = LocateRegistry.getRegistry(middlewareHostname, middlewarePort);
            resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + middlewareName);
            System.out.println("Connected to middleware server [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "]");
            middleware_dead.set(false);
            return;
        } catch (Exception e) {
            System.out.println("Checking if middleware [" + middlewareHostname + ":" + middlewarePort + "/" + s_rmiPrefix + middlewareName + "] has recovered");
        }
        middleware_dead.set(true);
    }
}