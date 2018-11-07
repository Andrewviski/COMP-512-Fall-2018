package Server.RMI;

import LockManager.TransactionAbortedException;
import Middleware.InvalidTransactionException;
import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RMIResourceManager extends ResourceManager {
    private static Object lock = new Object();
    private static String s_rmiPrefix = "group16_";
    private static String name = "Server";
    private static int port = 54000;
    private HashMap<Integer,RMHashMap> editSet;

    private static void ReportServerError(String msg, Exception e) {
        System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0m" + msg + " ]");
        e.printStackTrace();
        System.exit(1);
    }

    private static void parseConfig(String[] args) {
        if (args.length > 2 || args.length < 1) {
            ReportServerError("Usage: java client.RMIClient [port] [servername]", null);
        }

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                ReportServerError("Port is not a number, please try again!", e);
            }
        }
        if (args.length > 1) {
            name = args[1];
        }
    }

    public static void main(String args[]) {
        parseConfig(args);

        // Create the RMI server entry.
        try {
            // Create a new Server object.
            RMIResourceManager server = new RMIResourceManager(name);

            // Dynamically generate the stub (client proxy).
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(server, port);

            // Bind the remote object's stub in the registry.
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(port);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + name, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind(s_rmiPrefix + name);
                    System.out.println("'" + name + "' resource manager unbound");
                } catch (Exception e) {
                    ReportServerError("Unbounding failed", e);
                }
            }));
            System.out.println("'" + name + "' resource manager server ready and bound to '" + s_rmiPrefix + name + " at port " + port + "'");
        } catch (Exception e) {
            ReportServerError("Uncaught exception", e);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public RMIResourceManager(String name) {
        super(name);
    }

}
