package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    // TODO: Maybe c'tor will be better
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    // Copy of a byte array, but until bytes actual len.
    private byte[] copyBytes;
    private int len = 0;
    private PacketOpcode opcode = PacketOpcode.NOT_INIT;
    private final int OPCODE_LEN = 2;
    private final byte finishingByte = (byte) 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // Reset fields if its new msg
        if (len == 0) {
            reset();
        }
        // Find opcode when received 2 bytes
        if (len == OPCODE_LEN) {
            this.opcode = decodeOpcode();
        }
        // DISC OR DIRQ ARE SPECIAL WITH ONLY 2 BYTES
        else if (len == 1 && (nextByte == (byte) 0x000A || nextByte == (byte) 6)) {
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
                    // Those opcodes finish with byte 0
                    copyBytes = Arrays.copyOfRange(bytes, 0, Math.min(bytes.length, len));
                    len = 0;
                    return copyBytes;
                }
                break;
            case ACK:
                if (len == 3) {
                    // Those opcodes are exactly 4 bytes
                    pushByte(nextByte);
                    copyBytes = Arrays.copyOfRange(bytes, 0, Math.min(bytes.length, len));
                    len = 0;
                    return copyBytes;
                }
                break;
            case DISC:
            case DIRQ:
                // Those opcodes are exactly 2 bytes - only opcode
                copyBytes = Arrays.copyOfRange(bytes, 0, Math.min(bytes.length, len));
                len = 0;
                return copyBytes;
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
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(byte nextByte) {
        bytes[len] = nextByte;
        len++;
    }

    // Reset fields for next decoding
    private void reset() {
        bytes = new byte[1 << 10]; //start with 1k
        len = 0;
        opcode = PacketOpcode.NOT_INIT;
    }
}