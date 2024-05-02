package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectedHandlersMap;

    public ConnectionsImpl() {
        this.connectedHandlersMap = new ConcurrentHashMap<>();
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connectedHandlersMap.put(connectionId, handler);
        System.out.println("aaa");
    }

    @Override
    public boolean send(int connectionId, T msg) {
        boolean outcome = false;
        ConnectionHandler client = connectedHandlersMap.get(connectionId);
        if (client != null) {
            client.send(msg);
            outcome = true;
        }
        return outcome;
    }

    @Override
    public void disconnect(int connectionId) {
        connectedHandlersMap.remove(connectionId);
    }

    public Set<Integer> getConnectionIds() {
        return connectedHandlersMap.keySet();
    }
}
