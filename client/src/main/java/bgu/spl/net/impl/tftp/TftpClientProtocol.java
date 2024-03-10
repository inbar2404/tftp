package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;


public class TftpClientProtocol implements MessagingProtocol<byte[]> {
    // Enum class represent the operation code of the given packet.
    private PacketOpcode opcode;
    private boolean shouldTerminate ;
    private short seqNumReceived;
    private int errorNumber;
    private String errorMsg;
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
        if(KeyboardThread.packetsNum == 1)
            shouldTerminate =true;
        else
            KeyboardThread.packetsNum--;
        // TODO : CHECK HOW TO TELL THC CH TO NOT TERMINATE WHEN RECEIVING DATA PACKETS

    }

    private void processError() {
        System.out.println("Error " + errorNumber + " " + errorMsg);
        shouldTerminate =true;

    }

    public void setShouldTerminate(boolean shouldTer)
    {
        this.shouldTerminate=shouldTer;
    }
    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }


}
