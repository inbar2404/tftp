package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;

public class TftpClientProtocol implements MessagingProtocol<byte[]> {


    @Override
    public byte[] process(byte[] message) {
        System.out.println("proccc");
        return null;
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }


}
