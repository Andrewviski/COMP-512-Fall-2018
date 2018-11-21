SRC = ./src/main/java/ca/mcgill/comp512
BIN = ./target/classes

all: java.policy
	javac -d $(BIN) $(SRC)/Client/*java $(SRC)/LockManager/*java $(SRC)/Middleware/*java $(SRC)/Tester/*java $(SRC)/Server/Common/*java $(SRC)/Server/RMI/*java $(SRC)/Server/makInterface/*.java
    
java.policy:
	@echo "Creating client java policy"
	@echo "grant {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

clean:
	rm -rf $(BIN)/*
