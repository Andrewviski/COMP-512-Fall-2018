#!/usr/bin/env bash
#Usage: ./run_server.sh [port] [name]

./run_rmi.sh > /dev/null 2>&1
echo 'killing' $(lsof -i:$1 -t)
kill -9  $(lsof -i:$1 -t) > /dev/null 2>&1
sleep 0.2
while true; do java -Djava.security.policy=java.policy -cp ../target/512-project-1.0-SNAPSHOT.jar ca.mcgill.comp512.Server.RMI.RMIResourceManager $1 $2 && break; done



