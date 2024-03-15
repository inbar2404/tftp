package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class KeyboardThread implements Runnable {
    private BlockingConnectionHandler<byte[]> handler;
    private boolean shouldTerminate;

    public static int packetsNum = 1;
    public static String downloadFileName;
    public static String uploadFileName;
    public static String deleteFileName;
    public static String suserCommand;

    public KeyboardThread(BlockingConnectionHandler<byte[]> handler) {
        this.handler = handler;
        this.shouldTerminate = false;
    }

    /**
     * Main lifecycle.
     */
    // TODO : Check when needs to interrupt
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (!shouldTerminate && !Thread.currentThread().isInterrupted()) {
            synchronized (handler) {
                byte[] msg = processUserInput(scanner.nextLine());
                try {
                    if (!shouldTerminate && msg != null) {
                        handler.send(msg);
                        handler.wait();
                    }
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
            case "DELRQ": {
                suserCommand = "DELRQ";
                packetsNum = 1;
                return buildDELRQ(userInput.substring(spaceIndex + 1));
            }
            case "DIRQ": {
                suserCommand = "DIRQ";
                packetsNum = 1;
                return new byte[]{0, 6};
            }
            case "RRQ": {
                // Handle case file already exists in client side
                if (new File(userInput.substring(spaceIndex + 1)).exists()) {
                    System.out.println("file already exists");
                } else {
                    suserCommand = "RRQ";
                    packetsNum = 1;
                    return buildRRQ(userInput.substring(spaceIndex + 1));
                }
                break;
            }
            case "WRQ": {
                // Handle case file does not exist in client side
                if (!new File(userInput.substring(spaceIndex + 1)).exists()) {
                    System.out.println("file does not exist");
                } else {
                    suserCommand = "WRQ";
                    packetsNum = 1;
                    return buildWRQ(userInput.substring(spaceIndex + 1));
                }
                break;
            }
        }

        return null;
    }

    private byte[] buildRRQ(String fileName) {
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

    private byte[] buildWRQ(String fileName) {
        uploadFileName = fileName;
        byte[] fileNameBytes = fileName.getBytes();

        // Insert opcode of logrq to the msg , and a 0 terminator
        byte[] fullMsg = new byte[fileNameBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 2;
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
        deleteFileName = fileNameToDelete;
        byte[] fileNameBytes = fileNameToDelete.getBytes();

        // Insert opcode of logrq to the msg , and a 0 terminator
        byte[] fullMsg = new byte[fileNameBytes.length + 3];
        fullMsg[0] = 0;
        fullMsg[1] = 8;
        fullMsg[fullMsg.length - 1] = 0;

        System.arraycopy(fileNameBytes, 0, fullMsg, 2, fileNameBytes.length);
        return fullMsg;
    }
}