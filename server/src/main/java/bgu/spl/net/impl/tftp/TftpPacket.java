package bgu.spl.net.impl.tftp;

public class TftpPacket {

    protected PacketOpcode opcode;
    protected String arg;

    public TftpPacket(PacketOpcode opcode, String arg) {
        this.opcode = opcode;
        this.arg = arg;
    }

    public byte[] encode() {
        // TODO: Implement
        return null;
    }
}