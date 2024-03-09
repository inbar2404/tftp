package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.io.IOException;


public class TftpClient {
    public static void main(String[] args) throws Exception {
        System.out.println("client started!");
        Socket sock;
        try {
            // TODO : CHANGE TO GET THIS FROM USER
            sock = new Socket("127.0.0.1", 7777);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BlockingConnectionHandler<byte[]> handler = new BlockingConnectionHandler<>(
                sock, new TftpClientEncoderDecoder(), new TftpClientProtocol());


        // Build keyboard thread
        KeyboardThread keyboardRunnable = new KeyboardThread(handler);
        Thread keyboardThread = new Thread(keyboardRunnable);

        // Build listening thread
        ListeningThread listeningRunnable = new ListeningThread(handler);
        Thread listeningThread = new Thread(listeningRunnable);

        while(!keyboardThread.isInterrupted())
        {
            keyboardThread.run();
            listeningThread.run();
        }

    }
}


