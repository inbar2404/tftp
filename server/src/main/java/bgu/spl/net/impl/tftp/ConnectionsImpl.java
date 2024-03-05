package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// TODO: I think it should be in a different location
// TODO: Is my implementation is accurate?
public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer,ConnectionHandler<T>> connectedHandlersMap;

    // TODO: consult where it should be
    private HashMap<String, Integer> namesToIds = new HashMap<>();

    public ConnectionsImpl(){
        this.connectedHandlersMap = new ConcurrentHashMap<>();
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connectedHandlersMap.put(connectionId, handler);
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

}
