# comp512-project

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager
./run_servers.sh # convenience script for starting multiple resource managers
./run_servers2.0.sh # convenience script for starting multiple resource managers on cs.mcgill.ca machine using stored credentials.
./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] 
    [<hostname of Cars>] [<hostname of Customers>] # convenience script for starting a middle ware connected to the specified four resource managers.
```

To run the RMI client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]]
```
