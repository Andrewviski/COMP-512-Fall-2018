#!/usr/bin/env bash
# Usage: ./run_client.sh [middleware_hostname] [port] [middleware_rmiobject]

java -Djava.security.policy=java.policy -cp ../target/512-project-1.0-SNAPSHOT.jar ca.mcgill.comp512.Client.RMIClient $1 $2 $3