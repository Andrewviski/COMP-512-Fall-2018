package Client;

import static org.junit.jupiter.api.Assertions.*;

class RMIClientTest {
    private RMIClient rmiClient;
    private String middleware_hostname="localhost";
    private String middleware_servername="Middleware";
    private int middleware_port=1099;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rmiClient=new RMIClient();
        rmiClient.connectServer(middleware_hostname,middleware_port,middleware_servername);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void main() {
    }

    @org.junit.jupiter.api.Test
    void connectServer() {
    }

    @org.junit.jupiter.api.Test
    void connectServer1() {
    }

    @org.junit.jupiter.api.Test
    void connectServer2() {
    }

    @org.junit.jupiter.api.Test
    void start() {
    }

    @org.junit.jupiter.api.Test
    void execute() {
    }

    @org.junit.jupiter.api.Test
    void parse() {
    }

    @org.junit.jupiter.api.Test
    void checkArgumentsCount() {
    }

    @org.junit.jupiter.api.Test
    void toInt() {
    }

    @org.junit.jupiter.api.Test
    void toBoolean() {
    }
}