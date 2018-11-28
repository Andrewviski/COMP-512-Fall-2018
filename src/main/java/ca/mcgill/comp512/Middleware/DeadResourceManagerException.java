package ca.mcgill.comp512.Middleware;

public class DeadResourceManagerException extends RuntimeException {
    private String rm_name = "";

    public DeadResourceManagerException(String rm_name, String msg) {
        super(rm_name + " resource manager is dead [" + msg + "]\n");
        this.rm_name = rm_name;
    }

    public String getRMName() {
        return rm_name;
    }
}

