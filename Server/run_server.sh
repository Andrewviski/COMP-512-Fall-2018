#Usage: ./run_server.sh [port] [name]

./run_rmi.sh > /dev/null 2>&1
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.ServerSideResourceManager $1 $2
