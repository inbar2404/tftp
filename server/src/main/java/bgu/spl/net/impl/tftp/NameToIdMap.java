package bgu.spl.net.impl.tftp;

import java.util.HashMap;

public class NameToIdMap {
    public static HashMap<String, Integer> namesToIds = new HashMap<>();

    // Adding new user logged in (name,id) to the map
    public static void add(String name, int id) {
        namesToIds.put(name, id);
    }

    public static void remove(int id) {
        // Iterate over the entries of the HashMap
        for (HashMap.Entry<String, Integer> entry : namesToIds.entrySet()) {
            // Check if the entry's value matches the provided id
            if (entry.getValue() == id) {
                // If found, remove the entry
                namesToIds.remove(entry.getKey());
                // Exit the loop after removing the entry
                break;
            }
        }
    }

    public static boolean contains(String name) {
        return namesToIds.containsKey(name);
    }

    public static boolean contains(int id) {
        return namesToIds.containsValue(id);
    }
}
