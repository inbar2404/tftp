package bgu.spl.net.impl.bidiecho;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.tftp.NameToIdMap;
import bgu.spl.net.impl.tftp.UploadingFiles;
import bgu.spl.net.srv.Connections;

import java.util.concurrent.ConcurrentHashMap;

class holder {
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
}

public class bidiEchoProtocol implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections;

    @Override
    public void start(int connectionId, Connections<String> connections, NameToIdMap nameToIdMap, UploadingFiles uploadingFiles ) {
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        holder.ids_login.put(connectionId, true);
    }

    @Override
    public void process(String msg) {
        shouldTerminate = "bye".equals(msg);
        System.out.println("[" + connectionId + "]: " + msg);
        String echo = createEcho(msg);
        for (Integer id : holder.ids_login.keySet()) {
            if (holder.ids_login.get(id)) {
                connections.send(id, echo);
            }
        }
    }

    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        if (shouldTerminate) {
            this.connections.disconnect(this.connectionId);
            holder.ids_login.remove(this.connectionId);
        }
        return shouldTerminate;
    }
}
