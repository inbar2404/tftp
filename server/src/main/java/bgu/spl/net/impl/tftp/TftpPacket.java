package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TftpPacket {

    private PacketOpcode opcode;
    private byte[] message;
    private int len;
    private String arg;
    private boolean notConnected;
    boolean userHadError;

    public TftpPacket(PacketOpcode opcode, byte[] message, int len) {
        this.opcode = opcode;
        this.message = message;
        this.len = len;
        this.userHadError = false;
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
            // In this case arg is the user-name/filename
            case LOGRQ:
            case DELRQ:
                this.arg = new String(message, 2, len - 2);
                break;
        }
    }

    public boolean process(int connectionID, Connections<TftpPacket> connections) {
        // TODO: Complete other cases
        boolean shouldFinish = false;
        notConnected = opcode != PacketOpcode.LOGRQ && !NameToIdMap.contains(connectionID);
        if (opcode == PacketOpcode.NOT_INIT) {
            byte[] msg = buildError(4, "Illegal TFTP operation");
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
            case DELRQ:
                processDELRQ(connectionID, connections);
                break;
        }
        if (notConnected && !userHadError) {
            byte[] msg = buildError(6, "User not logged in");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }
        return shouldFinish;
    }

    private void processLOGRQ(int connectionID, Connections<TftpPacket> connections) {
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
        byte[] msg;
        msg = buildAck(0);
        connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
    }

    private void processDELRQ(int connectionID, Connections<TftpPacket> connections) {
        byte[] msg;
        String status = deleteFile(arg);
        if (status != "deleted") {
            if (status == "not exists") {
                userHadError = true;
                msg = buildError(1, "File not found");
                connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
            } else if (status == "failed") {
                userHadError = true;
                msg = buildError(2, "Access violation");
                connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
            }
        }
        else {
            // Send ack to the client
            msg = buildAck(0);
            connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
            // Build and send broadcast message to all connected clients about the deleted file
            msg = buildBcast(0,arg);
            for (Integer id : connections.getConnectedHandlersMap().keySet()) {
                if (NameToIdMap.contains(id)) {
                    connections.send(id, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
                }
            }
        }
    }

    private String deleteFile(String filename) {
        File file = new File("./Files/" + filename);

        if (file.exists()) {
            if ((!notConnected)) {
                if (file.delete()) {
                    return "deleted";
                } else {
                    return "failed";
                }
            } else
                return "disc";
        } else {
            return "not exists";
        }
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

    private byte[] buildBcast(int actionNum,String fileName) {
        byte BCAST_OPCODE = 9;
        // Calculate the length of the byte array
        int length = 2 + 1 + fileName.length() + 1; // 2 bytes for opcode, 1 byte for actionNum, 1 byte for zero terminator
        // Construct the byte array
        byte[] bcastPacket = new byte[length];
        // Encode the opcode
        bcastPacket[0] = 0; // First byte is 0
        bcastPacket[1] = BCAST_OPCODE; // Second byte is the opcode
        // Encode the actionNum
        bcastPacket[2] = (byte) actionNum;
        // Encode the filename as UTF-8 bytes
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(fileNameBytes, 0, bcastPacket, 3, fileNameBytes.length);
        // Add zero terminator
        bcastPacket[length - 1] = 0;
        return bcastPacket;

        }
    }