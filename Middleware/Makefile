all: java.policy compile-middleware

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

compile-middleware: Middleware/*.java
	javac Middleware/*.java

clean:
	rm -f Middleware/*.class
	rm -f java.policy 
