package ca.mcgill.comp512.Tester;

import ca.mcgill.comp512.Client.RMIClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ca.mcgill.comp512.Client.Command.fromString;

class TestCommand<ResponseType extends Object> {
	public String command;
	public ResponseType expectedResponse;
	public ResponseType actualResponse = null;

	public TestCommand(String command, ResponseType response) {
		this.command = command;
		this.expectedResponse = response;
	}

	public boolean Verify(RMIClient client) {
		if (actualResponse == null) {
			List<String> args = Arrays.asList(command.split(","));
			try {
				actualResponse = (ResponseType) client.execute(fromString(args.get(0)), args);
			} catch (Exception e) {
				System.err.println("TestCommand [ " + command + " ] crashed, printing stacktrace...");
				e.printStackTrace();
			}
		}
		if (expectedResponse != null)
			return expectedResponse.equals(actualResponse);
		else
			return actualResponse == null;
	}

	public String toString() {
		return command.toString() + " [ Expecting: " + expectedResponse.toString() + " ]";
	}
}

public class Tester {
	static RMIClient Spawn(String args[]) {
		// Get a client.
		RMIClient tester_client = new RMIClient();
		tester_client.ParseMiddlewareServerConfig(args);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		tester_client.connectServer();
		return tester_client;
	}

	static void TearDown(RMIClient client) {
		try {
			client.execute(fromString("quit"), Arrays.asList("quit"));
		} catch (Exception e) {
			System.err.println("TearDown crashed!");
			e.printStackTrace();
		}
	}

	static int generateXid(RMIClient client) {
		try {
			return (int) client.execute(fromString("start"), Arrays.asList("start"));
		} catch (Exception e) {
			System.err.println("generateXid crashed!");
			e.printStackTrace();
			return -1;
		}
	}

	static List<TestCommand<?>> AddCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		for (String s : flights)
			commands.add(new TestCommand<Boolean>("AddFlight," + xid + "," + s, true));
		for (String s : cars)
			commands.add(new TestCommand<Boolean>("AddCars," + xid + "," + s, true));
		for (String s : rooms)
			commands.add(new TestCommand<Boolean>("AddRooms," + xid + "," + s, true));
		for (String s : customers)
			commands.add(new TestCommand<Boolean>("AddCustomerID," + xid + "," + s, true));
		return commands;
	}

	static List<TestCommand<?>> ReserveCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		for (String s : flights)
			commands.add(new TestCommand<Boolean>("ReserveFlight," + xid + "," + s, true));
		for (String s : cars)
			commands.add(new TestCommand<Boolean>("ReserveCars," + xid + "," + s, true));
		for (String s : rooms)
			commands.add(new TestCommand<Boolean>("ReserveRooms," + xid + "," + s, true));
		return commands;
	}

	static List<TestCommand<?>> DeleteCommandsConstructor(int xid, List<String> flights, List<String> cars, List<String> rooms, List<String> customers) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		for (String s : flights)
			commands.add(new TestCommand<Boolean>("DeleteFlight," + xid + "," + s, true));
		for (String s : cars)
			commands.add(new TestCommand<Boolean>("DeleteCars," + xid + "," + s, true));
		for (String s : rooms)
			commands.add(new TestCommand<Boolean>("DeleteRooms," + xid + "," + s, true));
		for (String s : customers)
			commands.add(new TestCommand<Boolean>("DeleteCustomer," + xid + "," + s, true));
		return commands;
	}

	static List<TestCommand<?>> QueryCommandsConstructor(int xid, List<Map.Entry<String, Integer>> flights, List<Map.Entry<String, Integer>> cars, List<Map.Entry<String, Integer>> rooms, List<Map.Entry<String, String>> customers) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		for (Map.Entry<String, Integer> e : flights)
			commands.add(new TestCommand<Integer>("QueryFlight," + xid + "," + e.getKey(), e.getValue()));
		for (Map.Entry<String, Integer> e : cars)
			commands.add(new TestCommand<Integer>("QueryCars," + xid + "," + e.getKey(), e.getValue()));
		for (Map.Entry<String, Integer> e : rooms)
			commands.add(new TestCommand<Integer>("QueryRooms," + xid + "," + e.getKey(), e.getValue()));
		for (Map.Entry<String, String> e : customers)
			commands.add(new TestCommand<String>("QueryCustomer," + xid + "," + e.getKey(), e.getValue()));
		return commands;
	}

	static List<TestCommand<?>> QueryPriceCommandsConstructor(int xid, List<Map.Entry<String, Integer>> flights, List<Map.Entry<String, Integer>> cars, List<Map.Entry<String, Integer>> rooms) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		for (Map.Entry<String, Integer> e : flights)
			commands.add(new TestCommand<Integer>("QueryFlightPrice," + xid + "," + e.getKey(), e.getValue()));
		for (Map.Entry<String, Integer> e : cars)
			commands.add(new TestCommand<Integer>("QueryCarsPrice," + xid + "," + e.getKey(), e.getValue()));
		for (Map.Entry<String, Integer> e : rooms)
			commands.add(new TestCommand<Integer>("QueryRoomsPrice," + xid + "," + e.getKey(), e.getValue()));
		return commands;
	}

	static List<TestCommand<?>> AllServersDataAddingStressTest(RMIClient client) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		int xid = generateXid(client);
		List<String> flights = Arrays.asList("1,1,1", "2,2,2", "3,3,3");
		List<String> cars = Arrays.asList("montreal,1,1", "paris,2,2", "ottawa,3,3");
		List<String> rooms = Arrays.asList("montreal,1,1", "paris,2,2", "ottawa,3,3");
		List<String> customers = Arrays.asList("1", "11", "111");
		List<TestCommand<?>> temp = AddCommandsConstructor(xid, flights, cars, rooms, customers);
		temp.add(new TestCommand<Boolean>("Commit," + xid, true));
		return temp;
	}

	static List<TestCommand<?>> MileStone1Test(RMIClient client) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();
		int xid = 1;
		commands.add(new TestCommand<Integer>("start", 1));

		// 1. Adding data + Distribution
		commands.add(new TestCommand<Boolean>("AddFlight," + xid + ",1,3,10", true));
		commands.add(new TestCommand<Boolean>("AddCars," + xid + ",Montreal,5,20", true));
		commands.add(new TestCommand<Boolean>("AddRooms," + xid + ",Montreal,1,100", true));
		try {
			TimeUnit.MILLISECONDS.sleep(900);
		} catch (Exception e) {

		}

		// 2. Querying data + Distribution
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 3));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
		commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 5));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Paris", 0));
		commands.add(new TestCommand<Integer>("QueryFlightPrice," + xid + ",1", 10));


		// 3. Basic Customers
		commands.add(new TestCommand<Boolean>("AddCustomerID," + xid + ",1", true));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", ""));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",100", null));

		// 4. Reservations + Customers (bill)
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 3));
		commands.add(new TestCommand<Boolean>("ReserveFlight," + xid + ",1,1", true));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,"));
		commands.add(new TestCommand<Boolean>("DeleteFlight," + xid + ",1", false));

		commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 5));
		commands.add(new TestCommand<Boolean>("ReserveCar," + xid + ",1,Montreal", true));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,1 car-montreal $20,"));

		// 5. Bundle
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
		commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 4));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 1));
		commands.add(new TestCommand<Boolean>("Bundle," + xid + ",1,1,2,Montreal,true,true", false));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 2));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",2", 0));
		commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 4));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 1));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "1 flight-1 $10,1 car-montreal $20,"));

		commands.add(new TestCommand<Boolean>("Bundle," + xid + ",1,1,Montreal,true,true", true));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid + ",1", 1));
		commands.add(new TestCommand<Integer>("QueryCars," + xid + ",Montreal", 3));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid + ",Montreal", 0));
		commands.add(new TestCommand<String>("QueryCustomer," + xid + ",1", "2 flight-1 $10,1 room-montreal $100,2 car-montreal $20,"));
		commands.add(new TestCommand<Boolean>("Commit," + xid, true));

		return commands;
	}

	static List<TestCommand<?>> MileStone2Test(RMIClient client1, RMIClient client2) {
		List<TestCommand<?>> commands = new ArrayList<TestCommand<?>>();

		// 1. Simple commit
		int xid1 = 1;
		commands.add(new TestCommand<Integer>("start", 1));

		commands.add(new TestCommand<Boolean>("AddFlight," + xid1 + ",1,10,10", true));
		commands.add(new TestCommand<Boolean>("AddRooms," + xid1 + ",Montreal,15,15", true));
		commands.add(new TestCommand<Boolean>("AddCustomerID," + xid1 + ",1", true));
		commands.add(new TestCommand<Boolean>("ReserveFlight," + xid1 + ",1,1", true));
		commands.add(new TestCommand<Boolean>("ReserveRoom," + xid1 + ",1,Montreal", true));
		commands.add(new TestCommand<String>("QueryCustomer," + xid1 + ",1", "1 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<Boolean>("Commit," + xid1, true));

		int xid2 = 2;
		commands.add(new TestCommand<Integer>("start", 2));

		commands.add(new TestCommand<String>("QueryCustomer," + xid2 + ",1", "1 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<Boolean>("Commit," + xid2, true));

		//2. Simple abort
		int xid3 = 3;
		commands.add(new TestCommand<Integer>("start", 3));

		commands.add(new TestCommand<Boolean>("AddCars," + xid3 + ",Monteal,20,20", true));
		commands.add(new TestCommand<Boolean>("AddRooms," + xid3 + ",Montreal,10,10", true));
		commands.add(new TestCommand<Boolean>("AddCustomerID," + xid3 + ",2", true));
		commands.add(new TestCommand<Boolean>("ReserveFlight," + xid3 + ",1,1", true));
		commands.add(new TestCommand<Boolean>("ReserveFlight," + xid3 + ",2,1", true));
		commands.add(new TestCommand<String>("QueryCustomer," + xid3 + ",1", "2 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<String>("QueryCustomer," + xid3 + ",2", "1 flight-1 $10,"));
		commands.add(new TestCommand<Boolean>("Abort," + xid3, true));

		int xid4 = 4;
		commands.add(new TestCommand<Integer>("start", 4));

		commands.add(new TestCommand<String>("QueryCustomer," + xid4 + ",1", "1 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<String>("QueryCustomer," + xid4 + ",2", ""));
		commands.add(new TestCommand<Boolean>("Commit," + xid4, true));

		// 3. Customer lock
		int xid5 = 5;
		int xid6 = 6;
		commands.add(new TestCommand<Integer>("start", 5));
		commands.add(new TestCommand<Integer>("start", 6));

		commands.add(new TestCommand<Boolean>("ReserveFlight," + xid5 + ",1,1", true));
		commands.add(new TestCommand<String>("QueryCustomer," + xid6 + ",1", null));
		commands.add(new TestCommand<Boolean>("Abort," + xid5, true));

		// 4. Lock conversion
		int xid7 = 7;
		commands.add(new TestCommand<Integer>("start", 7));

		commands.add(new TestCommand<Integer>("QueryRooms," + xid7 + ",Montreal", 14));
		commands.add(new TestCommand<Boolean>("AddRooms," + xid7 + ",Montreal,5,5", true));
		commands.add(new TestCommand<Boolean>("Abort," + xid7, true));


		// 5. Deadlock
		int xid8 = 8;
		int xid9 = 9;
		commands.add(new TestCommand<Integer>("start", 8));
		commands.add(new TestCommand<Integer>("start", 9));

		commands.add(new TestCommand<Integer>("QueryFlight," + xid8 + ",1", 9));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid9 + ",Montreal", 14));
		commands.add(new TestCommand<Boolean>("AddRooms," + xid8 + ",Montreal,10,10", null));
		commands.add(new TestCommand<Boolean>("AddFlight," + xid9 + ",1,10,10", true));
		commands.add(new TestCommand<Boolean>("abort," + xid9, true));

		// 6. Time-to-live check
		int xid10 = 10;
		commands.add(new TestCommand<Integer>("start", 10));

		commands.add(new TestCommand<Integer>("Sleep,20", 0));
		commands.add(new TestCommand<Boolean>("AddFlight," + xid10 + ",1,10,10", null));

		// 7. Bundle atomicity
		int xid11 = 11;
		commands.add(new TestCommand<Integer>("start", 11));

		commands.add(new TestCommand<Integer>("QueryFlight," + xid11 + ",1", 9));
		commands.add(new TestCommand<Integer>("QueryCars," + xid11 + ",Montreal", 0));
		commands.add(new TestCommand<Integer>("QueryRooms," + xid11 + ",Montreal", 14));
		commands.add(new TestCommand<String>("QueryCustomer," + xid11 + ",1", "1 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<Boolean>("Bundle," + xid11 + ",1,1,Montreal,1,1", false));
		commands.add(new TestCommand<String>("QueryCustomer," + xid11 + ",1", "1 flight-1 $10,1 room-montreal $15,"));
		commands.add(new TestCommand<Boolean>("Abort," + xid11, true));

		// 8(b). Multi-operations for those using an isolation store per transaction
		int xid12 = 12;
		commands.add(new TestCommand<Integer>("start", 12));
		commands.add(new TestCommand<Boolean>("AddFlight," + xid12 + ",10,10,10", true));
		commands.add(new TestCommand<Boolean>("Commit," + xid12, true));

		int xid13 = 13;
		commands.add(new TestCommand<Integer>("start", 13));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid13 + ",10", 10));
		commands.add(new TestCommand<Boolean>("DeleteFlight," + xid13 + ",10", true));
		commands.add(new TestCommand<Boolean>("Commit," + xid13, true));

		int xid14 = 14;
		commands.add(new TestCommand<Integer>("start", 14));
		commands.add(new TestCommand<Boolean>("AddFlight," + xid14 + ",10,10,10", true));
		commands.add(new TestCommand<Boolean>("abort," + xid14, true));

		int xid15 = 15;
		commands.add(new TestCommand<Integer>("start", 15));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid15 + ",10", 0));
		commands.add(new TestCommand<Boolean>("DeleteFlight," + xid15 + ",10", false));
		commands.add(new TestCommand<Boolean>("AddFlight," + xid15 + ",10,15,15", true));
		commands.add(new TestCommand<Boolean>("Commit," + xid15, true));

		int xid16 = 16;
		commands.add(new TestCommand<Integer>("start", 16));
		commands.add(new TestCommand<Integer>("QueryFlight," + xid16 + ",10", 15));
		commands.add(new TestCommand<Boolean>("Commit," + xid16, true));

		return commands;
	}

	boolean RunGenericTest(RMIClient tester_client, List<TestCommand<?>> test) {
		for (int i = 0; i < test.size(); i++) {
			//TestCommand test_command=test.get(rand.nextInt(test.size()-1));
			TestCommand test_command = test.get(i);
			if (test_command.command.split(",")[0].equals("Sleep")) {
				try {
					TimeUnit.SECONDS.sleep(Integer.parseInt(test_command.command.split(",")[1]));
					continue;
				} catch (Exception e) {
					System.out.println(test_command.command + "...... Failed!");
					return false;
				}
			}
			if (!test_command.Verify(tester_client)) {
				System.out.println(test_command.command + "...... Failed!");
				return false;
			} else {
				System.out.println(test_command.command + "...... Passed!");
			}
		}
		return true;
	}

	void StressTest(String[] args, int NClients) {
		for (int i = 0; i < NClients; i++) {
			new Thread(() -> {
				RMIClient tester_client1 = Spawn(args);
				RMIClient tester_client2 = Spawn(args);
				RunGenericTest(tester_client1, MileStone2Test(tester_client1, tester_client2));

//				RMIClient tester_client=Spawn(args);
//				RunGenericTest(tester_client,MileStone1Test(tester_client));
			}).start();
		}
	}

	public static void main(String args[]) {
		Tester tester = new Tester();
		tester.StressTest(args, 1);
	}
}
