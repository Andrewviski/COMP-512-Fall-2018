#!/usr/bin/env bash
# Usage: ./run_client.sh [middleware_hostname] [port] [middleware_rmiobject]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:. Client.Tester $1 $2 $3