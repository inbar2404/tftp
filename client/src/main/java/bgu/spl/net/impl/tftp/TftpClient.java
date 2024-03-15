package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TftpClient {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[]{"127.0.0.1", "7777"};
        }

        if (args.length < 2) {
            System.out.println("you must supply two arguments: host, port");
            System.exit(1);
        }


        try {
            Socket sock = new Socket(args[0], Integer.parseInt(args[1]));
            BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());

            List<Thread> threads = new ArrayList<>();
            BlockingConnectionHandler<byte[]> handler = new BlockingConnectionHandler<>(
                    sock, new TftpClientEncoderDecoder(), new TftpClientProtocol(out), threads);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


