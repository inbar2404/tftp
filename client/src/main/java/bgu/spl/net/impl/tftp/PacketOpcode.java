package bgu.spl.net.impl.tftp;

// Enum class representing the different opcodes
public enum PacketOpcode {
    NOT_INIT((short) 99),
    RRQ((short) 1),
    WRQ((short) 2),
    DATA((short) 3),
    ACK((short) 4),
    ERROR((short) 5),
    DIRQ((short) 6),
    LOGRQ((short) 7),
    DELRQ((short) 8),
    BCAST((short) 9),
    DISC((short) 10);

    private final short shortValue;

    PacketOpcode(short shortValue) {
        this.shortValue = shortValue;
    }

    public static PacketOpcode fromShort(short shortValue) {
        // Converts opcode from short to PacketOpcode
        for (PacketOpcode opcode : values()) {
            if (opcode.shortValue == shortValue) {
                return opcode;
            }
        }
        throw new IllegalArgumentException("Invalid short value: " + shortValue);
    }
}
