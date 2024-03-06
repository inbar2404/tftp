package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

import java.nio.charset.StandardCharsets;

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
        return message;
    }

    private void convertMessage() {
        // TODO: Handle other opcodes
        switch (opcode) {
            // In this case arg is the user-name
            case LOGRQ:
                this.arg = new String(message, 2, len - 2);
        }
    }

    public boolean process(int connectionID, Connections<TftpPacket> connections) {
        // TODO: Complete other cases
        boolean shouldFinish = false;

        if (opcode == PacketOpcode.NOT_INIT) {
            byte[] msg = buildError(4, "Illegal TFTP operation");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        } else if (opcode != PacketOpcode.LOGRQ && !NameToIdMap.contains(connectionID)) {
            byte[] msg = buildError(6, "User not logged in");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }

        switch (opcode) {
            case LOGRQ: {
                processLOGRQ(connectionID, connections);
                break;
            }
            case DISC: {
                shouldFinish = true;
                processDISC(connectionID, connections);
                break;
            }
        }
        return shouldFinish;
    }

    private void processLOGRQ(int connectionID, Connections<TftpPacket> connections) {
        System.out.println("proc logrq");
        byte[] msg;
        if (NameToIdMap.contains(connectionID)) {
            msg = buildError(0, "this user already connected from this socket");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        } else if (!NameToIdMap.contains(this.arg)) {
            NameToIdMap.add(arg, connectionID);
            msg = buildAck(0);
            connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
        } else {
            msg = buildError(7, "User already logged in");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }
    }

    private void processDISC(int connectionID, Connections<TftpPacket> connections) {
        System.out.println("proc disc");

        byte[] msg;
        msg = buildAck(0);
        connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
    }


    private byte[] buildAck(int seqNumber) {
        // TODO  : handle seqNumber>0
        byte[] msg;
        if (seqNumber == 0) {
            msg = new byte[]{0, 4, 0, 0};
            return msg;
        }
        return null;
    }

    // Builds bytes array representing the error packet, with corresponding code and message.
    public static byte[] buildError(int errorCode, String errMsg) {
        int opcode = 5;
        byte[] errMsgBytes = errMsg.getBytes(StandardCharsets.UTF_8);
        int length = 2 + 2 + errMsgBytes.length + 1; // 2 bytes opcode + 2 bytes errorCode + errMsgBytes + 1 byte zero terminator
        byte[] encodedMessage = new byte[length];
        // Encode opcode
        encodedMessage[0] = (byte) (opcode >> 8);
        encodedMessage[1] = (byte) opcode;
        // Encode errorCode
        encodedMessage[2] = (byte) (errorCode >> 8);
        encodedMessage[3] = (byte) errorCode;
        // Encode errMsg
        System.arraycopy(errMsgBytes, 0, encodedMessage, 4, errMsgBytes.length);
        // Add zero terminator
        encodedMessage[length - 1] = 0;

        return encodedMessage;
    }
}