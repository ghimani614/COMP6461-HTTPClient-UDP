package ca.concordia.httpc;

import java.io.IOException;

public class ServerMainThread extends Thread {
    MultiplexServer multiplexServer = new MultiplexServer();

    public boolean startRunningServer = false;

    public void run() {
        System.out.println("Waiting for commands to run server");
        while (true) {
            System.out.print("");
            if (startRunningServer) {
                try {
                    multiplexServer.runServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}