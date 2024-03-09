package bgu.spl.net.impl.tftp;

public class ListeningThread implements Runnable{
    private BlockingConnectionHandler<byte[]> handler;
    private boolean shouldTerminate;

    public ListeningThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }


    public void run() {
        handler.receive();

    }
}
