package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

public class TftpPacket {

    private PacketOpcode opcode;
    private byte[] message;
    private int len;
    private String arg;

    public TftpPacket(PacketOpcode opcode, byte[] message, int len) {
        this.opcode = opcode;
        this.message = message;
        this.len = len;
        convertMessage();
    }

    public PacketOpcode getOpcode() {
        return this.opcode;
    }

    public byte[] encode() {
        // TODO: Implement
        return null;
    }

    private void convertMessage() {
        // TODO: Handle other opcodes
        switch (opcode) {
            // In this case arg is the user-name
            case LOGRQ:
                this.arg = new String(message, 2, len - 3);
        }
    }

    public void process(int connectionID, Connections<TftpPacket> connections) {
        if (!connections.isConnect(connectionID)) {
            // TODO: Complete other cases
            switch (opcode) {
                case LOGRQ: processLOGRQ(connectionID, connections);
            }
        } else {
            // TODO: Update to the right values in the new packet
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, null, 0));
        }
    }

    private void processLOGRQ(int connectionID, Connections<TftpPacket> connections) {
        if (!connections.containsKey(this.arg)) {
            // TODO: Update to the right values in the new packet
            connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, null, 0));
        } else {
            // TODO: Update to the right values in the new packet
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, null, 0));
        }
    }
}