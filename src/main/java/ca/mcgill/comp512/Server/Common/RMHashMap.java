// -------------------------------
// Kevin T. Manley
// CSE 593
// -------------------------------

package ca.mcgill.comp512.Server.Common;

import java.io.Serializable;
import java.util.HashMap;

// A specialization of HashMap with some extra diagnostics
public class RMHashMap extends HashMap<String, RMItem> implements Serializable {
    public RMHashMap() {
        super();
    }

    public String toString() {
        String s = "--- BEGIN RMHashMap ---\n";
        for (String key : keySet()) {
            String value = get(key).toString();
            s = s + "[KEY='" + key + "']" + value + "\n";
        }
        s = s + "--- END RMHashMap ---";
        return s;
    }

    public void dump() {
        System.out.println(toString());
    }

    public Object clone() {
        RMHashMap obj = new RMHashMap();
        for (String key : keySet()) {
            obj.put(key, (RMItem) get(key).clone());
        }
        return obj;
    }
}

