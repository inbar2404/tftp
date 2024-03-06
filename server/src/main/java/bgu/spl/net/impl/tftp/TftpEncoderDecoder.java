package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpPacket> {
    // TODO: I don't sure it is the correct thing to do - I took it from bidiEncoderDecoder.java
    // TODO: Maybe c'tor will be better
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private PacketOpcode opcode = PacketOpcode.NOT_INIT;
    private final int OPCODE_LEN = 2;
    private final byte finishingByte = (byte) 0;

    @Override
    public TftpPacket decodeNextByte(byte nextByte) {
        // TODO: Remove the use of magic numbers
        if (len == OPCODE_LEN) {
            this.opcode = decodeOpcode();
        } else if (len == 1 && nextByte == (byte) 0x000A) {
            // DISCONNECT
            pushByte(nextByte);
            this.opcode = decodeOpcode();
        }

        switch (opcode) {
            case RRQ:
            case WRQ:
            case ERROR:
            case DELRQ:
            case BCAST:
            case LOGRQ:
                if (nextByte == finishingByte) {
                    return popPacket();
                }
                break;
            case DISC:
                return popPacket();
            default:
        }
        pushByte(nextByte);
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
        TftpPacket p = null;
        switch (opcode) {
            case LOGRQ:
            case DISC:
            case DELRQ:
                p = new TftpPacket(opcode, bytes, len);
                // TODO: Do we need another way to handle the default later?
            default: {
                this.opcode = PacketOpcode.NOT_INIT;
            }
        }
        reset();
        return p;
    }

    // Reset fields for next decoding
    private void reset() {
        bytes = new byte[1 << 10]; //start with 1k
        len = 0;
        opcode = PacketOpcode.NOT_INIT;
    }
}