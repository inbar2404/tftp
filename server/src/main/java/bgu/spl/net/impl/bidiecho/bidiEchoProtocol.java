package bgu.spl.net.impl.bidiecho;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.util.concurrent.ConcurrentHashMap;

// TODO: This is what he did in the zoom but I'm not sure its the best solution
class holder {
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
}

public class bidiEchoProtocol implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        holder.ids_login.put(connectionId, true);
    }

    @Override
    public void process(String msg) {
        // TODO: I think refactor is needed - that's what he did in the zoom
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
