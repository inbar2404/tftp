package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate = false;
    // Enum class represent the operation code of the given packet.
    private PacketOpcode opcode;
    // Indicates if the user already had an error - and we should consider it when processing the request.
    public boolean userHadError;
    private String arg;
    private boolean notConnected;

    // Fields for DIRQ request.
    private boolean serverFinishedSendingDirData;
    private int seqNumSent;
    private short seqNumReceived;
    private int lastFileStartIndex;
    private LinkedList<String> files;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.userHadError = false;
        // Initialize dir request fields.
        this.serverFinishedSendingDirData = false;
        this.seqNumSent = 1;
        this.lastFileStartIndex = 0;
    }

    @Override
    public void process(byte[] message) {
        // Get the opcode.
        short opcodeShort = (short) (((short) message[0]) << 8 | (short) (message[1]));
        opcode = PacketOpcode.fromShort(opcodeShort);
        // Read massage data.
        convertMessage(message);
        // the user is connected only if he his on the mapping and he is not trying to connect now.
        notConnected = opcode != PacketOpcode.LOGRQ && !NameToIdMap.contains(connectionId);
        // For our checks after on client - if unrecognized opcode return error.
        if (opcode == PacketOpcode.NOT_INIT) {
            byte[] msg = buildError(4, "Illegal TFTP operation");
            connections.send(connectionId, msg);
        }
        // Process the msg depends on given opcode.
        switch (opcode) {
            case LOGRQ: {
                processLOGRQ();
                break;
            }
            case DISC: {
                processDISC();
                break;
            }
            case DELRQ: {
                processDELRQ();
                break;
            }
            case DIRQ: {
                processDIRQ();
                break;
            }
            case ACK: {
                processAck();
                break;
            }
        }
        // If the only error the user had, it's that he his not connected, send an error msg.
        if (notConnected && !userHadError) {
            byte[] msg = buildError(6, "User not logged in");
            connections.send(connectionId, msg);
        }
    }

    @Override
    public boolean shouldTerminate() {
        if (shouldTerminate) {
            // Remove user from mapping and connections.
            NameToIdMap.remove(connectionId);
            this.connections.disconnect(this.connectionId);
        }
        return shouldTerminate;
    }

    private void convertMessage(byte[] message) {
        switch (opcode) {
            // In this case arg is the user-name/filename.
            case LOGRQ:
            case DELRQ:
                this.arg = new String(message, 2, message.length - 2);
                break;
            // In this case arg is the seqNumReceived.
            case ACK:
                this.seqNumReceived = (short) (((short) message[2]) << 8 | (short) (message[3]));
                break;

        }
    }

    private void processLOGRQ() {
        byte[] msg;
        if (NameToIdMap.contains(connectionId)) {
            msg = buildError(0, "this user already connected from this socket");
            connections.send(connectionId, msg);
        } else if (!NameToIdMap.contains(this.arg)) {
            // User not exists, connect him.
            NameToIdMap.add(arg, connectionId);
            msg = buildAck(0);
            connections.send(connectionId, msg);
        } else {
            msg = buildError(7, "User already logged in");
            connections.send(connectionId, msg);
        }
    }

    private void processDISC() {
        // If user disconnects - change shouldTerminate to true for CH to break his loop, and send ack packet to client.
        shouldTerminate = true;
        connections.send(connectionId, buildAck(0));
    }

    private void processDELRQ() {
        String status = deleteFile(arg);
        if (status != "deleted") {
            // Handle errors.
            if (status == "not exists") {
                userHadError = true;
                connections.send(connectionId, buildError(1, "File not found"));
            } else if (status == "failed") {
                userHadError = true;
                connections.send(connectionId, buildError(2, "Access violation"));
            }
            // Else - user is not connected.
        } else {
            // Send ack to the client , successful delete.
            connections.send(connectionId, buildAck(0));
            // Build and send a broadcast message to all connected clients about the deleted file.
            for (Integer id : connections.getConnectedHandlersMap().keySet()) {
                if (NameToIdMap.contains(id)) {
                    connections.send(id, buildBcast(0, arg));
                }
            }
        }
    }

    private String deleteFile(String filename) {
        // This function tries to delete the file, and returns String representing the status of the file deletion.
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

    public void processDIRQ() {
        // Don't operate if user not connected
        if (notConnected)
            return;
        // Build a linked list of file names
        files = getFileList("./Files");
        connections.send(connectionId,  buildDirDataPacket());
    }

    private LinkedList<String> getFileList(String directoryPath) {
        // Returns a linked list of file names
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

    public void processAck() {

        // Ack packet received for dir request data packets - conitinue sending depands on serverFinishedSendingDirData boolean and tcp correct order
        if (seqNumSent == (int) this.seqNumReceived && !serverFinishedSendingDirData) {
            seqNumSent++;
            setSeqNumSent(seqNumSent);
            byte[] msg = buildDirDataPacket();
            connections.send(connectionId, msg);
        } else {
            // If finished - reset
            setSeqNumSent(1);
            setLastFileStartIndex(0);
            setServerFinishedSendingDirData(false);
            setFiles(null);
        }
    }

    private byte[] buildDirDataPacket() {
        int totalCharacterCount = 0;
        for (String fileName : files) {
            totalCharacterCount += fileName.length();
        }
        int maxSize = 512;
        // The data size of the packet is minimum between 512 max size , or  total file names + 0 separators(files.size() - 1 from this).
        int dataSize = Math.min(maxSize, totalCharacterCount + files.size() - 1);
        // If the packet date size is less than 512, it's the last one.
        setServerFinishedSendingDirData(dataSize < 512);
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
        encodedMessage[4] = (byte) (seqNumSent >> 8);
        encodedMessage[5] = (byte) seqNumSent;
        // The position to start inserting data from
        int destPos = 6;
        // Start from 0/last character index of file already partly processed file
        int srcPos = lastFileStartIndex;
        // How many bytes to copy from the file name
        int copySize = 0;
        int startFileSize = files.size();
        // Encode dataMsg
        for (int ind = 0; ind < startFileSize; ind++) {
            String file = files.getFirst();
            // Convert file name to a byte array.
            byte[] fileName = file.substring(lastFileStartIndex).getBytes(StandardCharsets.UTF_8);
            // Copy until finished file name or finished packet.
            copySize = Math.min(file.length(), dataSize + headerSize - destPos);
            // Break if finished packet size
            if (copySize <= 0)
                break;
            // Copy to the array
            System.arraycopy(fileName, srcPos, encodedMessage, destPos, copySize);
            destPos += copySize;

            // Add 0  separators between file names and remove the finished file
            if (destPos < (encodedMessage.length - headerSize)) {
                destPos++;
                encodedMessage[destPos] = 0;
                files.removeFirst();
            }
        }
        // Update fields for next DATA packet if needed
        setLastFileStartIndex(Math.max(copySize, 0));
        setFiles(files);
        return encodedMessage;
    }

    private byte[] buildAck(int seqNum) {
        // If seq number is 0 - return encoded msg
        if (seqNum == 0) {
            return new byte[]{0, 4, 0, 0};
        }
        return null;
    }

    // Builds byte array representing the error packet, with corresponding code and message.
    public byte[] buildError(int errorCode, String errMsg) {
        // Error opcode.
        int opcode = 5;
        byte[] errMsgBytes = errMsg.getBytes(StandardCharsets.UTF_8);
        int length = 2 + 2 + errMsgBytes.length + 1; // 2 bytes opcode + 2 bytes errorCode + errMsgBytes + 1 byte zero terminator
        byte[] encodedMessage = new byte[length];
        // Encode opcode.
        encodedMessage[0] = (byte) (opcode >> 8);
        encodedMessage[1] = (byte) opcode;
        // Encode errorCode.
        encodedMessage[2] = (byte) (errorCode >> 8);
        encodedMessage[3] = (byte) errorCode;
        // Encode errMsg.
        System.arraycopy(errMsgBytes, 0, encodedMessage, 4, errMsgBytes.length);
        // Add zero terminator.
        encodedMessage[length - 1] = 0;

        return encodedMessage;
    }

    // Builds byte array representing the BCAST packet, with corresponding code and message.
    private byte[] buildBcast(int actionNum, String fileName) {
        byte BCAST_OPCODE = 9;
        // Calculate the length of the byte array
        int length = 2 + 1 + fileName.length() + 1; // 2 bytes for opcode, 1 byte for actionNum, 1 byte for zero terminator.
        // Construct the byte array
        byte[] bcastPacket = new byte[length];
        // Encode the opcode
        bcastPacket[0] = 0; // The First byte is 0
        bcastPacket[1] = BCAST_OPCODE; // The Second byte is the opcode
        // Encode the actionNum
        bcastPacket[2] = (byte) actionNum;
        // Encode the filename as UTF-8 bytes
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(fileNameBytes, 0, bcastPacket, 3, fileNameBytes.length);
        // Add zero terminator
        bcastPacket[length - 1] = 0;
        return bcastPacket;
    }

    // Setter for serverFinishedSendingDirData
    private void setServerFinishedSendingDirData(boolean value) {
        serverFinishedSendingDirData = value;
    }

    // Setter for seqNumSent
    private void setSeqNumSent(int value) {
        seqNumSent = value;
    }

    // Setter for lastFileStartIndex
    private void setLastFileStartIndex(int value) {
        lastFileStartIndex = value;
    }

    // Setter for files
    private void setFiles(LinkedList<String> value) {
        files = value;
    }

}
