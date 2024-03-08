package bgu.spl.net.impl.tftp;

import java.util.HashMap;

public class NameToIdMap {
    private HashMap<String, Integer> namesToIds;

    public NameToIdMap() {
        this.namesToIds = new HashMap<>();
    }

    public void add(String name, int id) {
        this.namesToIds.put(name, id);
    }

    public void remove(int id) {
        this.namesToIds.values().removeIf(value -> value == id);
    }

    public boolean contains(String name) {
        return this.namesToIds.containsKey(name);
    }

    public boolean contains(int id) {
        return this.namesToIds.containsValue(id);
    }
}
