package bgu.spl.net.impl.tftp;

import java.io.IOException;

public class ListeningThread implements Runnable {
    private BlockingConnectionHandler<byte[]> handler;
    private boolean shouldTerminate;

    public ListeningThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }


    public void run() {
        synchronized (handler) {
            while (!shouldTerminate && !Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for keyboard thread to notify
                    handler.wait();
                } catch (InterruptedException ignored) {break;
                }
                try {
                    handler.userLoggedIn = true;
                    handler.receive();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }
        }
        System.out.println("finished LT ");

    }
}
