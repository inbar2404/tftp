package bgu.spl.net.impl.tftp;

public class ListeningThread implements Runnable{
    BlockingConnectionHandler<byte[]> handler;
    boolean shouldTerminate;

    public ListeningThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }


    public void run() {
        handler.receive();

    }
}
