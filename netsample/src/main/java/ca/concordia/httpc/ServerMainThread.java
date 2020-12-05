package ca.concordia.httpc;

import ca.concordia.udp.UDPServer;

import java.io.IOException;

public class ServerMainThread extends Thread {
    UDPServer udpServer = new UDPServer();

    public boolean startRunningServer = false;

    public void run() {
        System.out.println("Waiting for commands to run server");
        while (true) {
            System.out.print("");
            if (startRunningServer) {
                try {
                    udpServer.runServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}