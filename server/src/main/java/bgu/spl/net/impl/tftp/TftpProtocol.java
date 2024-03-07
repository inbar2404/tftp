package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.util.LinkedList;

public class TftpProtocol implements BidiMessagingProtocol<TftpPacket> {

    private int connectionId;
    private Connections<TftpPacket> connections;
    private boolean shouldTerminate = false;

    protected static boolean serverFinisheSendingDirData;
    protected static int seqNumSent = 1;
    protected static int lastFileStartIndex = 0;
    protected static LinkedList<String> files;
    /////////////////////////////////////////////////////
    private PacketOpcode opcode;
    private boolean notConnected;



    @Override
    public void start(int connectionId, Connections<TftpPacket> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.serverFinisheSendingDirData = false;
        this.seqNumSent = 1;
    }

    @Override
    public void process(TftpPacket message) {
        //shouldTerminate = message.process(connectionId, connections);
        // TODO: Complete other cases
        opcode = message.getOpcode();
        notConnected = opcode != PacketOpcode.LOGRQ && !NameToIdMap.contains(connectionId);
        if (opcode == PacketOpcode.NOT_INIT) {
            byte[] msg = message.buildError(4, "Illegal TFTP operation");
            connections.send(connectionId, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }
        switch (opcode) {
            case LOGRQ: {
                message.processLOGRQ(connectionId, connections);
                break;
            }
            case DISC: {
                shouldTerminate = true;
                message.processDISC(connectionId, connections);
                break;
            }
            case DELRQ: {
                message.processDELRQ(connectionId, connections);
                break;
            }
            case DIRQ: {
                message.processDIRQ(connectionId, connections,serverFinisheSendingDirData,seqNumSent,lastFileStartIndex,files);
                break;
            }
            case ACK: {
                message.processAck(connectionId, connections,serverFinisheSendingDirData,seqNumSent,lastFileStartIndex,files);
                break;
            }
        }
        if (notConnected && !message.userHadError) {
            byte[] msg = message.buildError(6, "User not logged in");
            connections.send(connectionId, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }
    }
    @Override
    public boolean shouldTerminate() {
        // TODO: Maybe required changes, I just wanted to check if it works
        if (shouldTerminate) {
            NameToIdMap.remove(connectionId);
            this.connections.disconnect(this.connectionId);
        }
        return shouldTerminate;
    }



    // Setter for serverFinisheSendingDirData
    public static void setServerFinisheSendingDirData(boolean value) {
        serverFinisheSendingDirData = value;
    }

    // Setter for seqNumSent
    public static void setSeqNumSent(int value) {
        seqNumSent = value;
    }

    // Setter for lastFileStartIndex
    public static void setLastFileStartIndex(int value) {
        lastFileStartIndex = value;
    }

    // Setter for files
    public static void setFiles(LinkedList<String> value) {
        files = value;
    }

}
