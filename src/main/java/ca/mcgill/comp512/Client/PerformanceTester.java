package ca.mcgill.comp512.Client;

import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PerformanceTester {
    private RMIClient client;

    public static void main(String[] args) {

        PerformanceTester performanceTester = new PerformanceTester(args);
        performanceTester.launchTest(10, 1);
    }

    public PerformanceTester(String[] clientArgs) {
        this.client = new RMIClient();
        client.ParseMiddlewareServerConfig(clientArgs);

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        client.connectMiddleware();
    }

    private void launchTest(int loopCount, int transactionPerSecond) {
        int ms = 1000 / transactionPerSecond;

        ArrayList<int[]> transactionTimes = new ArrayList<int[]>();

        int xid = generateTransactionId();

        List<String> args = new ArrayList<>();

        for (int i = 0; i < loopCount; i++) {
            Integer customerID = 0;
            try {
                customerID = (Integer) execute(Command.AddCustomer, args);
            } catch (ClassCastException e) {
                System.err.println("Did not receive a valid customer ID");
                return;
            }

            int time = (int) System.currentTimeMillis();

            launchSingleResourceRequest(xid);
//          launchDualResourceRequest(xid);
//          launchTripleResourceRequest(xid);

            // time now contains the transaction's run time
            time = (int) System.currentTimeMillis() - time;
            int[] array = {time, i};
            transactionTimes.add(array);
            System.out.println("Completed Transaction: " + i + ", of:" + loopCount);

            try {
                if (ms - time > 0) {
                    Thread.sleep(ms - time);
                } else {
                    System.out.println("Warning, cannot execute this many transactions per second");
                }
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }

            for (int j = 0; j < transactionTimes.size(); j++) {
                System.out.println(transactionTimes.get(j)[0] + "," + transactionTimes.get(j)[1] + "\n");
            }
        }
    }


    private void launchSingleResourceRequest(int xid) {
        int flightId1 = (int) (Math.random() * 100000);
        int flightId2 = (int) (Math.random() * 100000);
        int flightId3 = (int) (Math.random() * 100000);

        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1", "5")), true);
        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId2), "1", "5")), true);

        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1")), true);
        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId2), "1")), true);

        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId3), "1", "5")), true);
        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId3), "1")), true);

        expect(execute(Command.Commit, Arrays.asList(Integer.toString(xid))), true);
    }

    private int generateTransactionId() {
        List<String> args = new ArrayList<>();

        Integer xid = 0;
        try {
            xid = (Integer) execute(Command.Start, args);
        } catch (ClassCastException e) {
            System.err.println("Did not receive a valid xid");
        }
        return -1;
    }


    private void launchDualResourceRequest(int xid) {
        int flightId1 = (int) (Math.random() * 100000);
        int flightId2 = (int) (Math.random() * 100000);

        String city = generateRandomString();

        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1", "5")), true);
        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId2), "1", "5")), true);
        expect(execute(Command.AddCars, Arrays.asList(Integer.toString(xid), city, "1", "5")), true);

        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1")), true);
        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId2), "1")), true);
        expect(execute(Command.ReserveCar, Arrays.asList(Integer.toString(xid), city, "1")), true);

        expect(execute(Command.Commit, Arrays.asList(Integer.toString(xid))), true);
    }

    private void launchTripleResourceRequest(int xid) {
        int flightId1 = (int) (Math.random() * 100000);
        int flightId2 = (int) (Math.random() * 100000);

        String city1 = generateRandomString();
        String city2 = generateRandomString();


        expect(execute(Command.AddFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1", "5")), true);
        expect(execute(Command.ReserveFlight, Arrays.asList(Integer.toString(xid), Integer.toString(flightId1), "1")), true);

        expect(execute(Command.AddCars, Arrays.asList(Integer.toString(xid), city1, "1", "5")), true);
        expect(execute(Command.ReserveCar, Arrays.asList(Integer.toString(xid), city1, "1")), true);

        expect(execute(Command.AddRooms, Arrays.asList(Integer.toString(xid), city2, "1", "5")), true);
        expect(execute(Command.ReserveRoom, Arrays.asList(Integer.toString(xid), city2, "1")), true);

        expect(execute(Command.Commit, Arrays.asList(Integer.toString(xid))), true);
    }

    private Object execute(Command command, List<String> args) {
        try {
            return client.execute(command, args);
        } catch (RemoteException e) {
            System.err.println("Could execute command" + command.toString());
        }
        return null;
    }

    private static void expect(Object actual, Object expectation) {
        if (actual == null) {
            System.err.println("actual value is null");
        }

        if (!actual.equals(expectation)) {
            System.err.println("Could execute command");
        }
    }

    private static String generateRandomString() {
        byte[] array = new byte[7];
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));
        return generatedString;
    }
}
