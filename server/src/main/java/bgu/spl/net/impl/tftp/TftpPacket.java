package bgu.spl.net.impl.tftp;

public class TftpPacket {

    protected PacketOpcode opcode;
    protected byte[] message;

    public TftpPacket(byte[] message) {
        this.opcode = opcode;
        this.message = message;
    }

    public byte[] encode() {
        // TODO: Implement
        return null;
    }
}