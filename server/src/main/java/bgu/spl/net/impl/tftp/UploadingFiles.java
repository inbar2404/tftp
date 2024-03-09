package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;

public class UploadingFiles {
    private ConcurrentHashMap<String, Integer> uploadingFiles;

    public UploadingFiles() {
        this.uploadingFiles = new ConcurrentHashMap<>();
    }

    public void add(String name, int id) {
        this.uploadingFiles.put(name, id);
    }

    public void remove(String fileName) {
        this.uploadingFiles.entrySet().removeIf(entry -> entry.getKey().equals(fileName));
    }

    public boolean contains(String name) {
        return this.uploadingFiles.containsKey(name);
    }
}
