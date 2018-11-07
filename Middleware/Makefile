all: java.policy compile-middleware

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

compile-middleware: ../Server/RMIInterface.jar
	javac -cp ../Server/RMIInterface.jar Middleware/*.java

clean:
	rm -f Middleware/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy
