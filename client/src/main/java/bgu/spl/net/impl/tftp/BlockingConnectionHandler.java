package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class BlockingConnectionHandler<T> implements ConnectionHandler<T> {

    private final MessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    public boolean userLoggedIn = false;

    private List<Thread> threads;
    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol, List<Thread> threads) throws IOException {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.threads = threads;
        in = new BufferedInputStream(sock.getInputStream());
        out = new BufferedOutputStream(sock.getOutputStream());
    }

    // TODO : check synchronize cases
    public synchronized void receive() throws IOException {
        int read;
        protocol.setShouldTerminate(false);
        while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
            T nextMessage = encdec.decodeNextByte((byte) read);
            if (nextMessage != null) {
                userLoggedIn = protocol.process(nextMessage);
            }
        }
        this.notifyAll();
    }

    // TODO : check if need reset the threads
    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    @Override
    public synchronized void send(T msg) {
        try {
            if (msg != null) {
                out.write(encdec.encode(msg));
                out.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        notifyAll();
    }
}
