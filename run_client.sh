#!/usr/bin/env bash
# Usage: ./run_client.sh [middleware_hostname] [port] [middleware_rmiobject]

java -Djava.security.policy=java.policy -cp RMIInterface.jar:target/classes/ Client.RMIClient $1 $2 $3