package bgu.spl.net.impl.tftp;

import java.io.IOException;

public class ListeningThread implements Runnable{
    private BlockingConnectionHandler<byte[]> handler;
    private boolean shouldTerminate;

    public ListeningThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }


    public void run() {
        try {
            handler.receive();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
