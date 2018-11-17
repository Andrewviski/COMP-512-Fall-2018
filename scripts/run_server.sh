#!/usr/bin/env bash
#Usage: ./run_server.sh [port] [name]

./run_rmi.sh > /dev/null 2>&1
kill -9  $(lsof -i:$1 -t) > /dev/null 2>&1
sleep 0.2
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd) -cp ../target/classes/ Server.RMI.RMIResourceManager $1 $2