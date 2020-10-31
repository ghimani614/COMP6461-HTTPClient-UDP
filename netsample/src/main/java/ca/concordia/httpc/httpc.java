package ca.concordia.httpc;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.util.Arrays.asList;

public class httpc {

    // readFully reads until the request is fulfilled or the socket is closed
    private static void readFully(SocketChannel socket, ByteBuffer buf, int size) throws IOException {
        socket.read(buf);
    }

    private static void readEchoAndRepeat(SocketChannel socket) throws IOException {
        Charset utf8 = StandardCharsets.UTF_8;
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ByteBuffer buf = ByteBuffer.allocate(2048);

            int n = socket.write(ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)));
            buf.clear();

            // Receive all what we have sent
            readFully(socket, buf, n);
            buf.flip();
            System.out.println("Response:\n" + utf8.decode(buf));
        }
    }

    private static void runClient(SocketAddress endpoint) throws IOException {
        try (SocketChannel socket = SocketChannel.open()) {

            socket.connect(endpoint);

            System.out.println("httfs command: Type any thing then ENTER.");
            readEchoAndRepeat(socket);
        }
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("host", "h"), "EchoServer hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.acceptsAll(asList("port", "p"), "EchoServer listening port")
                .withOptionalArg()
                .defaultsTo("8010");

        OptionSet opts = parser.parse(args);

        String host = (String) opts.valueOf("host");
        int port = Integer.parseInt((String) opts.valueOf("port"));

        SocketAddress endpoint = new InetSocketAddress(host, port);
        runClient(endpoint);
    }
}

