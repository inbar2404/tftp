package bgu.spl.net.srv;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    // TODO: Check in the forum it is ok to add something to the interface
    boolean isConnect(int connectionId);

    // TODO: SAME
    boolean containsKey(String key);
}
