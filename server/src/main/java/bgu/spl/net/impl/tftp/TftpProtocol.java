package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<TftpPacket>  {

    private int connectionId;
    private Connections<TftpPacket> connections;

    @Override
    public void start(int connectionId, Connections<TftpPacket> connections) {
        // TODO: Is something else required?
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(TftpPacket message) {
        switch (message.getOpcode()){
            case LOGRQ:
        }
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        throw new UnsupportedOperationException("Unimplemented method 'shouldTerminate'");
    } 


    
}
