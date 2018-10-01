#@IgnoreInspection BashAddShebang
#Usage: ./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] [<hostname of Cars>] [<hostname of Customers>]

./run_rmi.sh > /dev/null 2>&1
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.Middleware.RMIMiddleware $1 $2 $3 $4