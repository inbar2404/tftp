package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TftpClient {
    public static void main(String[] args) throws Exception {
        System.out.println("client started!");
        Socket sock;
        try {
            // TODO : CHANGE TO GET THIS FROM USER + Handle case can not connect
            sock = new Socket("127.0.0.1", 7777);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }





        BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());
        List<Thread> threads = new ArrayList<>();




        BlockingConnectionHandler<byte[]> handler = new BlockingConnectionHandler<>(
                sock, new TftpClientEncoderDecoder(), new TftpClientProtocol(out),threads);
        // Build keyboard thread
        KeyboardThread keyboardRunnable = new KeyboardThread(handler);
        Thread keyboardThread = new Thread(keyboardRunnable);

        // Build listening thread
        ListeningThread listeningRunnable = new ListeningThread(handler);
        Thread listeningThread = new Thread(listeningRunnable);
        threads.add(keyboardThread);
        threads.add(listeningThread);

        keyboardThread.start();
        listeningThread.start();

    }
}


