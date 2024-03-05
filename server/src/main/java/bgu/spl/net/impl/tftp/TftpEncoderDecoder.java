package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpPacket> {
    // TODO: I don't sure it is the correct thing to do - I took it from bidiEncoderDecoder.java
    // TODO: Maybe c'tor will be better
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public TftpPacket decodeNextByte(byte nextByte) {
        if (nextByte == 0) {
            return popString();
        }
        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(TftpPacket message) {
        return message.encode();
    }

    private void pushByte(byte nextByte) {
        bytes[len] = nextByte;
        len++;
    }

    private TftpPacket popString() {
        if (len == 0) {
            return null;
        }

        // TODO: Handle here with sending to the function the correct opcode + argument
        PacketOpcode opcode = PacketOpcode.ACK;
        String arg = "";
        return new TftpPacket(opcode, arg);
    }
}