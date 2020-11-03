package ca.concordia.httpc;

import java.io.*;
import java.net.Socket;

public class ServerCommand {
    public static void main(String[] args) throws IOException {
        try {
            Socket socket = new Socket("localhost", 10);

            //DataInputStream is to create an input stream to receive response from the server.
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            //DataOutputStream is to create output stream to send information to the server socket.
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            String requestString = "", responseString = "";

            System.out.println("Server console input, enter httpfs to run the server:");

            while (true) {
                requestString = bufferedReader.readLine();
                dataOutputStream.writeUTF(requestString);
                dataOutputStream.flush();

                responseString = dataInputStream.readUTF();
                System.out.println(responseString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}