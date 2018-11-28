package ca.mcgill.comp512.Server.Interface;

import ca.mcgill.comp512.LockManager.DeadlockException;
import ca.mcgill.comp512.LockManager.TransactionAbortedException;
import ca.mcgill.comp512.Middleware.InvalidTransactionException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Simplified version from CSE 593 Univ. of Washington
 * <p>
 * Distributed  System in Java.
 * <p>
 * failure reporting is done using two pieces, exceptions and boolean
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * <p>
 * If there is a boolean return value and you're not sure how it
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IResourceManager extends Remote {
    public enum TransactionManagerCrashModes {
        NONE, BEFORE_SEND_VOTE_REQ, AFTER_SEND_VOTE_REQ, AFTER_REC_SOME_REPLIES, AFTER_REC_ALL_REPLIES, AFTER_DECIDING, AFTER_SENDING_SOME_DECISIONS, AFTER_SENDING_ALL_DECISIONS, DURING_RECOVERY
    }

    public enum ResourceManagerCrashModes {
        NONE, AFTER_REC_VOTE_REQ, AFTER_DECIDING_VOTE_ANSWER, AFTER_SENDING_ANSWER, AFTER_REC_DECISION, DURING_RECOVERY
    }

    /**
     * Add seats to a flight.
     * <p>
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Add car at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Add room at a location.
     * <p>
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Delete the flight.
     * <p>
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */
    public boolean deleteFlight(int id, int flightNum)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Delete all cars at a location.
     * <p>
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */
    public boolean deleteCars(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Delete all rooms at a location.
     * <p>
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException, InvalidTransactionException, DeadlockException;

    /**
     * @return
     * @throws RemoteException
     */
    public int start() throws RemoteException;

    /**
     * @param transactionId
     * @return
     * @throws RemoteException
     * @throws TransactionAbortedException
     * @throws InvalidTransactionException
     */
    public boolean commit(int transactionId) throws RemoteException,
            TransactionAbortedException, InvalidTransactionException;

    /**
     * @param transactionId
     * @throws RemoteException
     * @throws InvalidTransactionException
     */
    public void abort(int transactionId) throws RemoteException,
            InvalidTransactionException;

    /**
     * @return
     * @throws RemoteException
     */
    public boolean shutdown() throws RemoteException;

    /**
     * Ask the resource manager to prepare to commit transaction xid
     *
     * @param xid
     * @return
     */
    public boolean prepare(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException;


    /**
     * Disable crashes in the whole system.
     *
     * @return
     */
    public boolean resetCrashes() throws RemoteException;

    /**
     * Enable a crash mode at the middleware
     *
     * @param mode
     * @return
     * @throws RemoteException
     */
    public boolean crashMiddleware(TransactionManagerCrashModes mode) throws RemoteException;

    /**
     * Enable a crash mode at a resource manager
     *
     * @param name
     * @param mode
     * @return
     * @throws RemoteException
     */
    public boolean crashResourceManager(String name, ResourceManagerCrashModes mode) throws RemoteException;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
            throws RemoteException;
}