all: java.policy lockmanager client server middleware

java.policy:
	@echo "Creating client java policy"
	@echo "grant {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

interface: Server/Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Server/Interface/IResourceManager.class

lockmanager:
	javac -d target/classes LockManager/LockManager/*.java

client: interface lockmanager
	javac -d target/classes -cp RMIInterface.jar LockManager/LockManager/*.java Server/Server/Interface/*.java Client/Client/*.java

server-common: lockmanager
	javac -d target/classes -cp RMIInterface.jar LockManager/LockManager/*.java Server/Server/Interface/*.java Server/Server/Common/*.java

server: interface lockmanager server-common
	javac -d target/classes -cp RMIInterface.jar Server/Server/Interface/*.java LockManager/LockManager/*.java Server/Server/Common/*.java Server/Server/RMI/*.java

middleware: RMIInterface.jar lockmanager
	javac -d target/classes -cp RMIInterface.jar Server/Server/Interface/*.java LockManager/LockManager/*.java Middleware/Middleware/*.java

clean:
	rm -f LockManager/LockManager/*.class
	rm -f Client/Client/*.class
	rm -f Middleware/Middleware/*.class
	rm -f Server/Server/Common/*.class
	rm -f Server/Server/RMI/*.class
	rm -f java.policy
	rm -rf target/classes/*

