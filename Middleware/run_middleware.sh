#@IgnoreInspection BashAddShebang
#Usage: ./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] [<hostname of Cars>] [Flights port] [Rooms port] [Cars port]

PORT=54006
kill -9  $(lsof -i:$PORT -t) > /dev/null 2>&1
sleep 0.2
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ -cp ../Server/RMIInterface.jar:. Middleware.RMIMiddleware $1 $2 $3 $4 $5 $6