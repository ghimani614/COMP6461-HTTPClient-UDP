package ca.concordia.udp;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);

    private static void runClient(SocketAddress routerAddress, InetSocketAddress serverAddress) throws IOException {
        byte b = 55;

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);

            Selector selector = Selector.open();

            channel.register(selector, OP_READ);

            while (true) {
                Scanner scanner = new Scanner(System.in);

                String commandLineString = scanner.nextLine();


                Packet packet = createPacket(0, 11, serverAddress.getAddress(), serverAddress.getPort(), commandLineString.getBytes());

                channel.send(packet.toBuffer(), routerAddress);

                logger.info("Sending \"{}\" to router at {}", commandLineString, routerAddress);

                // Start the timer
                selector.select(5000);

                Set<SelectionKey> keys = selector.selectedKeys();

                // Time out
                if (keys.isEmpty()) {
                    logger.error("No response after timeout");
                    return;
                }

                // Receives packet from server
                ByteBuffer buffer = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = channel.receive(buffer);
                buffer.flip();

                Packet resp = Packet.fromBuffer(buffer);

                logger.info("Packet: {}", resp);
                logger.info("Router: {}", router);

                String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);

                logger.info("Payload: {}", payload);

                keys.clear();
            }
        }
    }

    private static Packet createPacket(int packetType, int sequenceNumber, InetAddress peerAddress, int peerPortNumber, byte[] payload) {
        return new Packet.Builder()
                .setType(packetType)
                .setSequenceNumber(sequenceNumber)
                .setPeerAddress(peerAddress)
                .setPortNumber(peerPortNumber)
                .setPayload(payload)
                .create();
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("router-host", "Router hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("router-port", "Router port number")
                .withOptionalArg()
                .defaultsTo("3000");

        parser.accepts("server-host", "EchoServer hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("server-port", "EchoServer listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);

        // Router address
        String routerHost = (String) opts.valueOf("router-host");
        int routerPort = Integer.parseInt((String) opts.valueOf("router-port"));

        // Server address
        String serverHost = (String) opts.valueOf("server-host");
        int serverPort = Integer.parseInt((String) opts.valueOf("server-port"));

        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        runClient(routerAddress, serverAddress);
    }
}

