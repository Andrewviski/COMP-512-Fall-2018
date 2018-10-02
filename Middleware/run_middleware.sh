#@IgnoreInspection BashAddShebang
#Usage: ./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] [<hostname of Cars>] [<hostname of Customers>] [Flights port] [Rooms port] [Cars port] [Customers port]

kill -9  $(lsof -i:54006 -t) > /dev/null 2>1
sleep 0.2
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Middleware.RMIMiddleware $1 $2 $3 $4 $5 $6 $7 $8