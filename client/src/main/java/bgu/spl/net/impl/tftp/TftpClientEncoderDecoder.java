package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

// TODO : update class from server when Inbar finished
public class TftpClientEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private PacketOpcode opcode = PacketOpcode.NOT_INIT;
    private final int OPCODE_LEN = 2;
    private final byte FINISH_BYTE = (byte) 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // Reset fields if its new msg
        if (len == 0) {
            reset();
        }
        // Find opcode when received 2 bytes
        if (len == OPCODE_LEN) {
            opcode = decodeOpcode();
        }
        // DISC OR DIRQ ARE SPECIAL WITH ONLY 2 BYTES
        else if (len == 1 && (nextByte == (byte) 0x000A || nextByte == (byte) 6)) {
            pushByte(nextByte);
            opcode = decodeOpcode();
        }

        if (opcode == PacketOpcode.ACK) {
            if (len == 3) {
                pushByte(nextByte);
                return finishDecoding();
            }
        } else if (opcode == PacketOpcode.DISC || opcode == PacketOpcode.DIRQ) {
            return finishDecoding();
        } else if ((opcode == PacketOpcode.RRQ || opcode == PacketOpcode.WRQ ||
                opcode == PacketOpcode.DELRQ || opcode == PacketOpcode.BCAST || opcode == PacketOpcode.LOGRQ)
                && nextByte == FINISH_BYTE) {
            return finishDecoding();
        }
        else if (opcode == PacketOpcode.ERROR && nextByte == FINISH_BYTE && len > 3) {
            return finishDecoding();
        }

        pushByte(nextByte);
        // Not a line yet
        return null;
    }

    // Decodes the first 2 bytes to find the opcode
    private PacketOpcode decodeOpcode() {
        short opcode = (short) (((short) bytes[0]) << 8 | (short) (bytes[1]) & 0x00FF);
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

    private byte[] finishDecoding() {
        byte[] copyBytes = Arrays.copyOfRange(bytes, 0, len);
        len = 0;
        return copyBytes;
    }
}