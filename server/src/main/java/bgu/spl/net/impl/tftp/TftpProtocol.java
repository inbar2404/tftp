package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
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
    private boolean serverFinishedSendingData;
    private int seqNumSent;
    private int currentPacketSize;
    private short seqNumReceived;
    private int latestIndexData;
    private LinkedList<String> files;
    private NameToIdMap nameToIdMap;
    private UploadingFiles uploadingFiles;
    private final int MAX_DATA_SIZE = 512;
    private final int DATA_HEADER_SIZE = 6; // 2 bytes opcode + 2 bytes PacketSize + 2 bytes seqNumber
    private String fileName;
    private String currentUploadFile;
    private byte[] data;

    @Override
    public void start(int connectionId, Connections<byte[]> connections, NameToIdMap nameToIdMap, UploadingFiles uploadingFiles) {
        this.nameToIdMap = nameToIdMap;
        this.uploadingFiles = uploadingFiles;
        this.connectionId = connectionId;
        this.connections = connections;
        this.userHadError = false;
        // Initialize data request fields.
        this.serverFinishedSendingData = false;
        this.seqNumSent = 1;
        this.latestIndexData = 0;
        this.currentUploadFile = "";
        this.data = new byte[0];
    }

    @Override
    public void process(byte[] message) {
        // Extract opcode.
        short opcodeShort = (short) (((short) message[0] & 0x00FF) << 8 | (short) (message[1] & 0x00FF));
        opcode = PacketOpcode.fromShort(opcodeShort);
        convertMessage(message);
        // The user is connected only if he is on the mapping, and he is not trying to connect now.
        notConnected = ((opcode != PacketOpcode.LOGRQ) && (!nameToIdMap.contains(connectionId)));
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
            case DATA: {
                processDataPacket();
                break;
            }
            case WRQ: {
                processWRQ();
                break;
            }
            case RRQ: {
                processRRQ();
                break;
            }
        }
        // If the only error the user had, it's that he is not connected, send an error msg.
        if (notConnected && !userHadError) {
            byte[] msg = buildError(6, "User not logged in");
            connections.send(connectionId, msg);
        }
    }

    @Override
    public boolean shouldTerminate() {
        if (shouldTerminate) {
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
            case WRQ:
            case RRQ:
                this.fileName = new String(message, 2, message.length - 2);
                break;
            // In this case arg is the seqNumReceived.
            case ACK:
                this.seqNumReceived = (short) (((short) message[2] & 0x00FF) << 8 | (short) (message[3] & 0x00FF));
                break;
            case DATA:
                this.seqNumReceived = (short) (((short) message[4] & 0x00FF) << 8 | (short) (message[5] & 0x00FF));
                this.currentPacketSize = (short) (((short) message[2] & 0x00FF) << 8 | (short) (message[3] & 0x00FF));
                byte[] currentData = Arrays.copyOfRange(message, 6, message.length);
                if (this.data.length == 0) {
                    this.data = currentData;
                } else {
                    int newDataLength = data.length + currentData.length;
                    byte[] newData = new byte[newDataLength];
                    System.arraycopy(data, 0, newData, 0, data.length);
                    System.arraycopy(currentData, 0, newData, data.length, currentData.length);
                    data = newData;
                }
                // Update latestIndexData for next data packet
                setLatestIndexData(latestIndexData + currentData.length);
                break;
        }
    }

    private void processLOGRQ() {
        byte[] msg;
        if (nameToIdMap.contains(connectionId)) {
            msg = buildError(0, "this user already connected from this socket");
            connections.send(connectionId, msg);
        } else if (!nameToIdMap.contains(this.arg)) {
            // User not exists, connect him.
            nameToIdMap.add(arg, connectionId);
            msg = buildAck(0);
            connections.send(connectionId, msg);
        } else {
            msg = buildError(7, "User already logged in");
            connections.send(connectionId, msg);
        }
    }

    private void processDISC() {
        if (notConnected) {
            // If user not connected - return error.
            userHadError = true;
            connections.send(connectionId, buildError(6, "User not logged in"));
        } else {
            // If user disconnects - change shouldTerminate to true for CH to break his loop, and send ack packet to client.
            shouldTerminate = true;
            connections.send(connectionId, buildAck(0));
            nameToIdMap.remove(connectionId);
            uploadingFiles.removeByConnectionId(connectionId);
        }
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
            for (Integer id : connections.getConnectionIds()) {
                if (nameToIdMap.contains(id)) {
                    connections.send(id, buildBcast(0, arg));
                }
            }
        }
    }

    private String deleteFile(String fileToDelete) {
        // This function tries to delete the file, and returns String representing the status of the file deletion.
        File file = new File("./Files/" + fileToDelete);

        try {
            // Check if file exists case sensitive
            if (file.exists()&& file.getCanonicalFile().getName().equals(file.getName())) {
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
        } catch (IOException ignored) {
        }
        return "failed";
    }

    public void processDIRQ() {
        // Don't operate if user not connected
        if (notConnected)
            return;
        // Build a linked list of file names
        files = getFileList("./Files");
        updateDataAsDirList();
        connections.send(connectionId, buildDataPacket());
    }

    private void updateDataAsDirList() {
        byte[] msg = new byte[0];
        if (files != null) {
            // Make sure there is a '0' in the end of each file name, except the last one
            for (int i = 0; i < files.size(); i++) {
                String name = files.get(i);
                int originalLength = msg.length;
                if (i != files.size() - 1) {
                    msg = Arrays.copyOf(msg, msg.length + name.getBytes().length + 1);
                } else {
                    msg = Arrays.copyOf(msg, msg.length + name.getBytes().length);
                }
                System.arraycopy(name.getBytes(), 0, msg, originalLength, name.getBytes().length);
                if (i != files.size() - 1) {
                    msg[msg.length - 1] = '\0';
                }
            }
        }
        this.data = msg;
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
        // Ack packet received for request data packets - conitinue sending depands on serverFinishedSendingDirData boolean and tcp correct order
        if (seqNumSent == (int) this.seqNumReceived && !serverFinishedSendingData) {
            seqNumSent++;
            setSeqNumSent(seqNumSent);
            byte[] msg = buildDataPacket();
            connections.send(connectionId, msg);
        } else {
            // If finished - reset
            setSeqNumSent(1);
            setLatestIndexData(0);
            setServerFinishedSendingData(false);
            setFiles(null);
            data = new byte[0];
        }
    }

    public void processWRQ() {
        if (!notConnected) {
            File file = new File("./Files/" + fileName);
            try {
                // Check if file exists case-sensitive or if is uploading now
                if ((file.exists() && file.getCanonicalFile().getName().equals(file.getName())) || uploadingFiles.contains(fileName)) {
                    // If file already exists - return an error.
                    userHadError = true;
                    connections.send(connectionId, buildError(5, "File already exists"));
                } else {
                    uploadingFiles.add(fileName, connectionId);
                    currentUploadFile = fileName;
                    connections.send(connectionId, buildAck(0));
                }
            } catch (IOException ignored) {}
        } else {
            // If user not connected - return error.
            userHadError = true;
            connections.send(connectionId, buildError(6, "User not logged in"));
        }
    }

    public void processDataPacket() {
        if (!notConnected) {
            // In case there is more than one packet in the sequence, and it is not the last packet
            if (currentPacketSize == MAX_DATA_SIZE) {
                connections.send(connectionId, buildAck(seqNumSent));
                seqNumSent++;
            } else if (data != null) {
                // In case it is the lase packet in the sequence
                if (currentPacketSize > MAX_DATA_SIZE) {
                    userHadError = true;
                    connections.send(connectionId, buildError(0, "Not defined error"));
                }
                writeData();
                connections.send(connectionId, buildAck(seqNumSent));

                // Build and send a broadcast message to all connected clients about the deleted file.
                for (Integer id : connections.getConnectionIds()) {
                    if (nameToIdMap.contains(id)) {
                        connections.send(id, buildBcast(1, currentUploadFile));
                    }
                }

                // Reset related fields
                seqNumSent = 1;
                currentPacketSize = 0;
                latestIndexData = 0;
                if (uploadingFiles.contains(currentUploadFile)) {
                    uploadingFiles.remove(currentUploadFile);
                }
                data = new byte[0];
                currentUploadFile = "";
            } else {
                userHadError = true;
                connections.send(connectionId, buildError(0, "Not defined error"));
            }
        } else {
            userHadError = true;
            connections.send(connectionId, buildError(6, "User not logged in"));
        }
    }

    private void writeData() {
        try (FileOutputStream out = new FileOutputStream("./Files/" + currentUploadFile)) {
            out.write(data);
        } catch (IOException e) {
            // Reset related fields and send error msg.
            data = new byte[0];
            if (uploadingFiles.contains(currentUploadFile)) {
                uploadingFiles.remove(currentUploadFile);
            }
            currentUploadFile = "";
            userHadError = true;
            connections.send(connectionId, buildError(2, "Access violation"));
        }
    }

    // Downloads the file from the server to the client.
    public void processRRQ() {
        File file = new File("./Files/" + fileName);
        try {
            // Check if file not exists case-sensitive
            if (!file.exists() ||  !file.getCanonicalFile().getName().equals(file.getName())) {
                userHadError = true;
                connections.send(connectionId, buildError(1, "File not found"));
            } else if (!notConnected) {
                try {
                    this.data = Files.readAllBytes(file.toPath());
                    connections.send(connectionId, buildDataPacket());
                } catch (IOException ignored) {
                    userHadError = true;
                    connections.send(connectionId, buildError(2, "Access violation"));
                }
            } else {
                userHadError = true;
                connections.send(connectionId, buildError(6, "User not logged in"));
            }
        } catch (IOException ignored) {
        }
    }

    private byte[] buildDataPacket() {
        int requireNumberOfPackets = data.length / MAX_DATA_SIZE + 1;
        setServerFinishedSendingData(requireNumberOfPackets - seqNumSent < 1);

        int dataSize = Math.min(MAX_DATA_SIZE, data.length - latestIndexData);
        int opcode = 3; // DATA opcode

        // Encode the message
        byte[] encodedMessage = new byte[dataSize + DATA_HEADER_SIZE];
        encodedMessage[0] = (byte) (opcode >> 8);
        encodedMessage[1] = (byte) opcode;
        encodedMessage[2] = (byte) (dataSize >> 8);
        encodedMessage[3] = (byte) dataSize;
        encodedMessage[4] = (byte) (seqNumSent >> 8);
        encodedMessage[5] = (byte) seqNumSent;

        int startInsertDataIndex = 6;
        int dataStartIndex = latestIndexData;
        System.arraycopy(data, dataStartIndex, encodedMessage, startInsertDataIndex, dataSize);

        // Update fields for next DATA packet if needed
        setLatestIndexData(latestIndexData + dataSize);
        return encodedMessage;
    }

    private byte[] buildAck(int seqNum) {
        // If seq number is 0 - return encoded msg
        if (seqNum == 0) {
            return new byte[]{0, 4, 0, 0};
        } else {
            // In case of processing data packets - return encoded msg with the given seqNum
            byte[] encodedMessage = new byte[4];
            encodedMessage[0] = 0;
            encodedMessage[1] = 4;
            encodedMessage[2] = (byte) (seqNum >> 8);
            encodedMessage[3] = (byte) seqNum;
            return encodedMessage;
        }
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
    private byte[] buildBcast(int actionNum, String file) {
        byte BCAST_OPCODE = 9;
        // 2 bytes for opcode, 1 byte for actionNum, 1 byte for zero terminator.
        int length = 2 + 1 + file.length() + 1;

        byte[] bcastPacket = new byte[length];
        // Encode bcast packet
        bcastPacket[0] = 0; // The First byte is 0
        bcastPacket[1] = BCAST_OPCODE; // The Second byte is the opcode
        bcastPacket[2] = (byte) actionNum;

        // Encode the filename as UTF-8 bytes
        byte[] fileNameBytes = file.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(fileNameBytes, 0, bcastPacket, 3, fileNameBytes.length);

        // Add zero terminator
        bcastPacket[length - 1] = 0;
        return bcastPacket;
    }

    private void setServerFinishedSendingData(boolean value) {
        serverFinishedSendingData = value;
    }

    private void setSeqNumSent(int value) {
        seqNumSent = value;
    }

    private void setLatestIndexData(int value) {
        latestIndexData = value;
    }

    private void setFiles(LinkedList<String> value) {
        files = value;
    }
}
