package Client;

import Middleware.RMIMiddleware;

import static org.junit.jupiter.api.Assertions.*;

class RMIClientTest {
    private RMIClient rmiClient;
    private RMIMiddleware middleWare;

    private String middleware_hostname="localhost";
    private String middleware_servername="Middleware";
    private int middleware_port=54002;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rmiClient=new RMIClient();

        //rmiClient.connectServer(middleware_hostname,middleware_port,middleware_servername);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void TestAddCars() {
    }
}