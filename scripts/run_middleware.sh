#!/usr/bin/env bash
#Usage: ./run_middleware.sh [<hostname of Flights>] [<hostname of Rooms>] [<hostname of Cars>] [Flights port] [Rooms port] [Cars port]

PORT=54006
kill -9  $(lsof -i:$PORT -t) > /dev/null 2>&1
sleep 0.2
while true; do java -Djava.security.policy=java.policy -cp ../target/512-project-1.0-SNAPSHOT.jar ca.mcgill.comp512.Middleware.RMIMiddleware $1 $2 $3 $4 $5 $6 && break; done