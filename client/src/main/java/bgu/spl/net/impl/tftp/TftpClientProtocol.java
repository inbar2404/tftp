package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;


public class TftpClientProtocol implements MessagingProtocol<byte[]> {
    // Enum class represent the operation code of the given packet.
    private PacketOpcode opcode;
    private boolean shouldTerminate;
    private short seqNumReceived;
    private int seqNumSent = 1;
    private int currentPacketSize;
    private byte[] data = new byte[0];
    private int errorNumber;
    private String errorMsg;
    public final int MAX_DATA_SIZE = 512;
    public final int DATA_HEADER_SIZE = 6;
    private BufferedOutputStream out;

    public TftpClientProtocol(BufferedOutputStream out) {
        this.out = out;
    }


    @Override
    public boolean process(byte[] message) {
        // Extract opcode.
        short opcodeShort = (short) (((short) message[0]) << 8 | (short) (message[1]) & 0x00FF);
        opcode = PacketOpcode.fromShort(opcodeShort);
        convertMessage(message);
        switch (opcode) {
            case ACK: {
                processAck();
                break;
            }
            case DATA: {
                processData();
                break;
            }
            case ERROR: {
                processError();
                break;
            }
        }
        return true;
    }

    private void convertMessage(byte[] message) {
        switch (opcode) {
            case ACK:
                this.seqNumReceived = (short) (((short) message[2] & 0x00FF) << 8 | (short) (message[3] & 0x00FF));
                break;
            case DATA:
                this.seqNumReceived = (short) (((short) message[4] & 0x00FF) << 8 | (short) (message[5] & 0x00FF));
                this.currentPacketSize = (short) (((short) message[2] & 0x00FF) << 8 | (short) (message[3] & 0x00FF));
                byte[] currentData = Arrays.copyOfRange(message, 6, message.length);
                extractData(currentData);
                break;
            case ERROR:
                this.errorNumber = (short) (((short) message[2] & 0x00FF) << 8 | (short) (message[3] & 0x00FF));
                this.errorMsg = new String(message, 4, message.length - 4);
                break;
        }
    }

    private void extractData(byte[] currentData) {
        if (this.data.length == 0) {
            this.data = currentData;
        } else {
            int newDataLength = data.length + currentData.length;
            byte[] newData = new byte[newDataLength];
            System.arraycopy(data, 0, newData, 0, data.length);
            System.arraycopy(currentData, 0, newData, data.length, currentData.length);
            data = newData;
        }
    }

    private void processAck() {
        System.out.println("ACK " + seqNumReceived);
        if (KeyboardThread.suserCommand.equals("WRQ")) {
            processWRQACK();
        } else {
            if (KeyboardThread.packetsNum == 1)
                shouldTerminate = true;
            else
                KeyboardThread.packetsNum--;
            // TODO : CHECK HOW TO TELL THC CH TO NOT TERMINATE WHEN RECEIVING DATA PACKETS

            if ((int) seqNumReceived == seqNumSent)
                seqNumSent++;
            else
                seqNumSent = 1;
        }
    }

    private void processWRQACK() {
        // In case of last packet
        if (data.length == 0) {
            File file = new File(KeyboardThread.uploadFileName);
            try {
                this.data = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                // TODO: Handle exception differently?
                e.printStackTrace();
            }
        }
        // In case of  last packet
        if (data.length < MAX_DATA_SIZE) {
            sendData(data);
            System.out.println("WRQ " + KeyboardThread.uploadFileName + " complete");
            data = new byte[0];
            KeyboardThread.uploadFileName = "";
            KeyboardThread.suserCommand = "";
        } else {
            byte[] currentData = Arrays.copyOf(data, MAX_DATA_SIZE);
            // Remove the extracted bytes from the data array
            byte[] remainingData = new byte[data.length - MAX_DATA_SIZE];
            System.arraycopy(data, MAX_DATA_SIZE, remainingData, 0, remainingData.length);
            data = remainingData;
            sendData(currentData);
        }
    }

    private void processData() {
        System.out.println("DATA " + seqNumReceived);
        if (KeyboardThread.suserCommand.equals("RRQ")) {
            processRRQData();
        }
        if (KeyboardThread.suserCommand.equals("DIRQ")) {
            processDIRQData();
        }
    }

    private  void processDIRQData() {
        // In case is the last packet
        if (currentPacketSize < MAX_DATA_SIZE) {
            printNames(); // TODO: Make sure I print as required
        } else {
            seqNumSent++;
        }
        sendAck(seqNumReceived);
    }

    private void printNames() {
        StringBuilder builder = new StringBuilder();
        int startIndex = 0;
        while (startIndex < data.length) {
            int endIndex = startIndex;
            // Find the end of the null-terminated string
            while (endIndex < data.length && data[endIndex] != 0) {
                endIndex++;
            }
            // Convert the bytes to a string and append it to the StringBuilder
            builder.append(new String(data, startIndex, endIndex - startIndex));
            // Append a space after each name, except for the last one
            if (endIndex < data.length - 1) {
                builder.append('\n');
            }
            // Move to the next string (skip the null byte)
            startIndex = endIndex + 1;
        }
        System.out.println(builder.toString());
    }

    private void processRRQData() {
        // TODO: Add check - ACK order mismatch
        // In case is the last packet
        if (currentPacketSize < MAX_DATA_SIZE) {
            try (FileOutputStream out = new FileOutputStream(KeyboardThread.downloadFileName)) {
                out.write(data);
                System.out.println("RRQ " + KeyboardThread.downloadFileName + " complete");

                // Reset related fields
                seqNumSent = 0;
                data = new byte[0];
                KeyboardThread.downloadFileName = "";
            } catch (IOException e) {
                // TODO: Handle exception differently?
                e.printStackTrace();
            }
        } else {
            seqNumSent++;
        }
        sendAck(seqNumReceived);
    }

    private void processError() {
        System.out.println("Error " + errorNumber + " " + errorMsg);
        shouldTerminate = true;

    }

    public void setShouldTerminate(boolean shouldTer) {
        this.shouldTerminate = shouldTer;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void sendAck(int blockNumber) {
        short a = (short) blockNumber;
        try {
            out.write(new byte[]{0, 4, (byte) (a >> 8), (byte) (a & 0xff)});
            out.flush();
        } catch (Exception ignored) {
        }
    }

    public void sendData(byte[] data) {
        // Encode the message
        byte[] encodedMessage = new byte[data.length + DATA_HEADER_SIZE];
        int opcode = 3; // DATA opcode

        encodedMessage[0] = (byte) (opcode >> 8);
        encodedMessage[1] = (byte) opcode;
        encodedMessage[2] = (byte) (data.length >> 8);
        encodedMessage[3] = (byte) data.length;
        encodedMessage[4] = (byte) (seqNumSent >> 8);
        encodedMessage[5] = (byte) seqNumSent;

        System.arraycopy(data, 0, encodedMessage, DATA_HEADER_SIZE, data.length);

        try {
            out.write(encodedMessage);
            out.flush();
        } catch (Exception ignored) {
        }
    }
}
