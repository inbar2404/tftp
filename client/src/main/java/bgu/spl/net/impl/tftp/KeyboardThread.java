package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class KeyboardThread implements Runnable {
    BlockingConnectionHandler<byte[]> handler;
    boolean shouldTerminate;

    public KeyboardThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }

    /**
     * Main lifecycle.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (!shouldTerminate) {
            byte[] msg = processUserInput(scanner.nextLine());

            handler.send(msg);

            // Wait for the Listening thread, to notify he has finished processing the packets sent from the server.
        }
    }


    private byte[] processUserInput(String userInput) {
        int spaceIndex = userInput.indexOf(' ');
        String userCommand = userInput.substring(0, spaceIndex);

        switch (userCommand) {
            case "LOGRQ":
                // build LOGRQ msg
                return buildLOGRQ(userInput.substring(spaceIndex + 1));
            case "DISC":

        }

        return null;
    }

    private byte[] buildLOGRQ(String userName) {
        byte[] userNameBytes = userName.getBytes();

        // Insert opcode of logrq to the msg , andd a 0 terminator
        byte[] fullMsg = new byte[userNameBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 7;
        fullMsg[fullMsg.length - 1] = 0;

        System.arraycopy(userNameBytes, 0, fullMsg, 2, userNameBytes.length);
        return fullMsg;
    }


}
