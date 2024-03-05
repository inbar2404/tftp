package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    // TODO: I don't sure it is the correct thing to do - I took it from bidiEncoderDecoder.java
    // TODO: Maybe c'tor will be better
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    private PacketOpcode opcode=PacketOpcode.NOT_INIT;
    private String args;


    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (len == 2) {
             opcode = decodeOpcode();
        }
        switch (opcode) {
            case RRQ:
            case WRQ:
            case ERROR:
            case LOGRQ:
            case DELRQ:
            case BCAST:
                if (nextByte == (byte)0) {
                    return bytes;
                }
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

//    private TftpPacket popString() {
 //       // TODO: Handle here with sending to the function the correct opcode + argument
  //      if (len == 0) {
   //         return null;
   //     }
  //      args = new String(bytes, 2, len, StandardCharsets.UTF_8);
  //      len = 0;
  //      return new TftpPacket(opcode, args);
   // }
}