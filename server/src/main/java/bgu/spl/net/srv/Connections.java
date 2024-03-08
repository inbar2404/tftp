package bgu.spl.net.srv;

import java.util.Set;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    Set<Integer> getConnectionIds();
}
