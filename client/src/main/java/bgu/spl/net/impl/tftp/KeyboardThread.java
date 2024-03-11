package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class KeyboardThread implements Runnable {
    private BlockingConnectionHandler<byte[]> handler;
    private boolean shouldTerminate;

    public static int packetsNum = 1;
    public static String downloadFileName;
    public static String suserCommand;

    public KeyboardThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }

    /**
     * Main lifecycle.
     */
    // TODO : CHECK WHEN NEEDS INTERRUPT
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (!shouldTerminate && !Thread.currentThread().isInterrupted()) {
            synchronized (handler) {
                byte[] msg = processUserInput(scanner.nextLine());
                try {
                    if(!shouldTerminate){
                    handler.send(msg);
                    handler.wait();}
                } catch (InterruptedException ignored) {
                    break;
                }
            }

        }
        try {
            handler.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("finished KT ");

    }


    private byte[] processUserInput(String userInput) {
        int spaceIndex = userInput.indexOf(' ');
        String userCommand;
        if (spaceIndex != -1) {
            userCommand = userInput.substring(0, spaceIndex);
        } else {
            userCommand = userInput;
        }

        suserCommand = userCommand;

        switch (userCommand) {
            case "LOGRQ": {
                suserCommand = "LOGRQ";
                packetsNum = 1;
                return buildLOGRQ(userInput.substring(spaceIndex + 1));
            }
            case "DISC": {
                suserCommand = "DISC";
                packetsNum = 1;
                if (!handler.userLoggedIn) {
                    System.out.println("Closing");
                    try {
                        handler.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                shouldTerminate = true;
                return buildDISC();
            }
            case "DELRQ": { // TODO: Check and refactor - Bar says there is a problem here
                suserCommand = "DELRQ";
                packetsNum = 2;
                return buildDELRQ(userInput.substring(spaceIndex + 1));
            }
            case "RRQ": {
                // Handle case file already exists in client side
                if (new File(userInput.substring(spaceIndex + 1)).exists()) {
                    System.out.println("file already exists");
                }
                else {
                    suserCommand = "RRQ";
                    packetsNum = 1;
                    return buildRRQ(userInput.substring(spaceIndex + 1));
                }
            }
        }

        return null;
    }

    private  byte[] buildRRQ(String fileName) {
        downloadFileName = fileName;
        byte[] fileNameBytes = fileName.getBytes();

        // Insert opcode of logrq to the msg , and a 0 terminator
        byte[] fullMsg = new byte[fileNameBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 1;
        fullMsg[fullMsg.length - 1] = 0;

        System.arraycopy(fileNameBytes, 0, fullMsg, 2, fileNameBytes.length);
        return fullMsg;
    }
    private byte[] buildLOGRQ(String userName) {
        byte[] userNameBytes = userName.getBytes();

        // Insert opcode of logrq to the msg , and a 0 terminator
        byte[] fullMsg = new byte[userNameBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 7;
        fullMsg[fullMsg.length - 1] = 0;

        System.arraycopy(userNameBytes, 0, fullMsg, 2, userNameBytes.length);
        return fullMsg;
    }

    private byte[] buildDISC() {
        byte[] fullMsg = new byte[2];
        fullMsg[0] = 0;
        fullMsg[1] = 10;
        return fullMsg;
    }

    private byte[] buildDELRQ(String fileNameToDelete) {
        byte[] fileNameToDeleteBytes = fileNameToDelete.getBytes();

        // Insert opcode of logrq to the msg , andd a 0 terminator
        byte[] fullMsg = new byte[fileNameToDeleteBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 8;
        fullMsg[fullMsg.length - 1] = 0;

        System.arraycopy(fileNameToDeleteBytes, 0, fullMsg, 2, fileNameToDeleteBytes.length);
        return fullMsg;
    }
}