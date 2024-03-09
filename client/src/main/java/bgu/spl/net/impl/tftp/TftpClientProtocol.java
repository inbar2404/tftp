package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.util.LinkedList;

public class TftpClientProtocol implements MessagingProtocol<byte[]> {
    // Enum class represent the operation code of the given packet.
    private PacketOpcode opcode;
    private boolean shouldTerminate = false;
    private short seqNumReceived;
    private int errorNumber;
    private String errorMsg;

    @Override
    public void process(byte[] message) {
        // Extract opcode.
        short opcodeShort = (short) (((short) message[0]) << 8 | (short) (message[1]) & 0x00FF);
        opcode = PacketOpcode.fromShort(opcodeShort);
        convertMessage(message);
        switch (opcode) {
            case ACK: {
                processAck();
                break;
            }
            case ERROR: {
                processError();
                break;
            }
        }

    }

    private void convertMessage(byte[] message) {
        switch (opcode) {
            case ACK:
                this.seqNumReceived = (short) (((short) message[2]) << 8 | (short) (message[3]));
                break;
            case ERROR:
                this.errorNumber = (short) (((short) message[2]) << 8 | (short) (message[3]));
                this.errorMsg = new String(message, 4, message.length - 4);
                break;
        }
    }

    private void processAck() {
        System.out.println("ACK " + seqNumReceived);
    }

    private void processError() {
        System.out.println("Error " + errorNumber + " " + errorMsg);
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }


}
