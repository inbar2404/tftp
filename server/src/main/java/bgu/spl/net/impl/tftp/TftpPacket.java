package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class TftpPacket {

    private PacketOpcode opcode;
    private byte[] message;
    private int len;
    private String arg;
    private boolean notConnected;
    public boolean userHadError;
    private short seqNumReceived;

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
            case ACK:
                this.seqNumReceived = (short) (((short) message[2]) << 8 | (short) (message[3]));
                break;

        }
    }

    /*
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
            case DELRQ: {
                processDELRQ(connectionID, connections);
                break;
            }
            case DIRQ: {
                processDIRQ(connectionID, connections);
                break;
            }
            case ACK: {
                processAck(connectionID, connections);
                break;
            }
        }
        if (notConnected && !userHadError) {
            byte[] msg = buildError(6, "User not logged in");
            connections.send(connectionID, new TftpPacket(PacketOpcode.ERROR, msg, msg.length));
        }
        return shouldFinish;
    }
    */

    public void processLOGRQ(int connectionID, Connections<TftpPacket> connections) {
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

    public void processDISC(int connectionID, Connections<TftpPacket> connections) {
        byte[] msg;
        msg = buildAck(0);
        connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
    }

    public void processDELRQ(int connectionID, Connections<TftpPacket> connections) {
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
        } else {
            // Send ack to the client
            msg = buildAck(0);
            connections.send(connectionID, new TftpPacket(PacketOpcode.ACK, msg, msg.length));
            // Build and send broadcast message to all connected clients about the deleted file
            msg = buildBcast(0, arg);
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

    public void processDIRQ(int connectionID, Connections<TftpPacket> connections, boolean serverFinisheSendingDirData ,int seqNumSent,int lastFileStartIndex, LinkedList<String> files) {
        if (notConnected)
            return;
        files = getFileList("./Files");
        TftpProtocol.setFiles(files);
        byte[] msg = buildDirDataPacket(seqNumSent, files,lastFileStartIndex);
        connections.send(connectionID, new TftpPacket(PacketOpcode.DATA, msg, msg.length));

    }

    private LinkedList<String> getFileList(String directoryPath) {
        LinkedList<String> fileList = new LinkedList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            for (String file : files) {
                fileList.add(file);
            }
        } else {
            System.err.println("Directory does not exist or is not a directory.");
        }

        return fileList;
    }

    public void processAck(int connectionID, Connections<TftpPacket> connections, boolean serverFinisheSendingDirData ,int seqNumSent,int lastFileStartIndex, LinkedList<String> files) {
        if (seqNumSent == (int) this.seqNumReceived && !serverFinisheSendingDirData) {
            seqNumSent++;
            TftpProtocol.setSeqNumSent(seqNumSent);
            byte[] msg = buildDirDataPacket(seqNumSent,files,lastFileStartIndex);
            connections.send(connectionID, new TftpPacket(PacketOpcode.DATA, msg, msg.length));
        }
        else {
            TftpProtocol.setSeqNumSent(1);
            TftpProtocol.setLastFileStartIndex(0);

        }
    }


    private byte[] buildDirDataPacket(int seqNumber, LinkedList<String> files,int lastFileStartIndex) {
        int totalCharacterCount = 0;
        for (String fileName : files) {
            totalCharacterCount += fileName.length();
        }
        System.out.println("count "+ totalCharacterCount);
        int maxSize = 512;
        int dataSize = Math.min(maxSize, totalCharacterCount + files.size() - 1);
        // If the packet date size is less than 512, it's the last one
        TftpProtocol.setServerFinisheSendingDirData(dataSize < 512);
        int opcode = 3;
        int headerSize = 6;
        int length = headerSize + dataSize; // 2 bytes opcode + 2 bytes PacketSize + 2 BYTES seqNumber  + dataMsgBytes
        byte[] encodedMessage = new byte[length];
        // Encode opcode
        encodedMessage[0] = (byte) (opcode >> 8);
        encodedMessage[1] = (byte) opcode;
        // Encode dataMsgLength
        encodedMessage[2] = (byte) (dataSize >> 8);
        encodedMessage[3] = (byte) dataSize;
        // Encode dataMsgSeqNum
        encodedMessage[4] = (byte) (seqNumber >> 8);
        encodedMessage[5] = (byte) seqNumber;
        int destPos = 6;
        int srcPos = lastFileStartIndex;
        int copySize = 0;
        int startFileSize = files.size();
        // Encode dataMsg
        for (int ind = 0; ind < startFileSize; ind++) {
            String file = files.getFirst();
            byte[] fileName = file.substring(lastFileStartIndex).getBytes(StandardCharsets.UTF_8);
            // Copy until finished file name or finished packet
            copySize = Math.min(file.length(), dataSize + headerSize - destPos);
            if (copySize <= 0)
                break;
            // Copy to the array
            System.arraycopy(fileName, srcPos, encodedMessage, destPos, copySize);
            destPos += copySize;

            // Add 0 tp separate between file names and remove the finished file
            if (destPos < (encodedMessage.length - headerSize)) {
                destPos++;
                encodedMessage[destPos] = 0;
                files.removeFirst();
            }
        }
        TftpProtocol.setLastFileStartIndex ( Math.max(copySize, 0));
        TftpProtocol.setFiles(files);
        return encodedMessage;
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
    public  byte[] buildError(int errorCode, String errMsg) {
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

    private byte[] buildBcast(int actionNum, String fileName) {
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