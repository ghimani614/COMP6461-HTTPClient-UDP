package ca.concordia.httpc;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerCommandThread extends Thread {
    MultiplexServer multiplexServer = new MultiplexServer();
    ServerMainThread serverMainThread = new ServerMainThread();

    public boolean confirmedToRunServer = false;

    public void run() {
        serverMainThread.start();

        try {
            ServerSocket serverSocket = new ServerSocket(10);
            Socket socket = serverSocket.accept();

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String receivedString = "", responseString = "Bais?";

            while (true) {
                receivedString = dataInputStream.readUTF();

                responseString = multiplexServer.parseServerCommandLine(receivedString);

                if (confirmedToRunServer) {
                    serverMainThread.startRunningServer = true;
                    confirmedToRunServer = false;
                }

                dout.writeUTF(responseString);
                dout.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}