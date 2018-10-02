#@IgnoreInspection BashAddShebang
#Usage: ./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] [<hostname of Cars>] [<hostname of Customers>] [Flights port] [Rooms port] [Cars port] [Customers port]

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Middleware.RMIMiddleware $1 $2 $3 $4 $5 $6 $7 $8