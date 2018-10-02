# Usage: ./run_client.sh [server_hostname] [port] [server_rmiobject]

java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:. Client.RMIClient $1 $2 $3
