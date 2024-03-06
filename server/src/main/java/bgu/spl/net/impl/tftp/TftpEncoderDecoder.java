package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpPacket> {
    // TODO: I don't sure it is the correct thing to do - I took it from bidiEncoderDecoder.java
    // TODO: Maybe c'tor will be better
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    private PacketOpcode opcode = PacketOpcode.NOT_INIT;


    @Override
    public TftpPacket decodeNextByte(byte nextByte) {
        // TODO: Remove the use of magic numbers
        pushByte(nextByte);
        this.opcode = decodeOpcode();
        if (len == 2) {
            return popPacket();
        }
        else if (len > 2) {
            if (nextByte == '\0') {
                return popPacket();
            }
            // TODO: Make sure handeling the other cases here
            if(len>4){
                // TODO: Handle OPCODE_DATA
            }
        }
//        switch (opcode) {
//            case RRQ:
//            case WRQ:
//            case ERROR:
//            case LOGRQ:
//            case DELRQ:
//            case BCAST:
//                if (nextByte == (byte) 0) {
//                    return new TftpPacket(opcode, this.bytes);
//                }
//            default:
//        }
        // Not a line yet
        return null;
    }

    // Decodes the first 2 bytes to find the opcode
    private PacketOpcode decodeOpcode() {
        short opcode = (short) (((short) bytes[0]) << 8 | (short) (bytes[1]));
        return PacketOpcode.fromShort(opcode);
    }

    @Override
    public byte[] encode(TftpPacket message) {
        return message.encode();
    }

    private void pushByte(byte nextByte) {
        bytes[len] = nextByte;
        len++;
    }

    private TftpPacket popPacket() {
        // TODO: Handle other opcodes
        switch (opcode) {
            case LOGRQ:
                return new TftpPacket(opcode, bytes, len);
            // TODO: Do we need another way to handle the default later?
            default: {
                this.opcode = PacketOpcode.NOT_INIT;
                return null;
            }
        }
        // TODO: Reset after?

    }
}