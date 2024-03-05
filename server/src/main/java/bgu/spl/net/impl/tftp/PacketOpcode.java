package bgu.spl.net.impl.tftp;

// TODO: Complete enum, maybe should be in a diffrent file
public enum PacketOpcode {
    RRQ,
    WRQ,
    DATA,
    ACK,
    ERROR,
    DIRQ,
    LOGRQ,
    DELRQ,
    BCAST,
    DISC
}
