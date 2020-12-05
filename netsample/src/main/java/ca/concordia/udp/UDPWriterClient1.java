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

public class UDPWriterClient1 {
    private static SenderVariableSet senderVariableSet = new SenderVariableSet();

    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);

    private static void runClient(SocketAddress routerAddress, InetSocketAddress serverAddress) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);

            Selector selector = Selector.open();

            channel.register(selector, OP_READ);

            while (true) {
                System.out.println("----------");
                Scanner scanner = new Scanner(System.in);

                String commandLineString = scanner.nextLine();

                for (int i = 0; i < 1013; i++)
                    commandLineString += "b";

                for (int i = 0; i < 1013; i++)
                    commandLineString += "a";

                for (int i = 0; i < 1013; i++)
                    commandLineString += "i";

                for (int i = 0; i < 1013; i++)
                    commandLineString += "s";

                for (int i = 0; i < 1013; i++)
                    commandLineString += "e";

                for (int i = 0; i < 1013; i++)
                    commandLineString += "r";

                senderVariableSet.commandLineByteArray = commandLineString.getBytes();

                senderVariableSet.initializeAttributes();

                senderVariableSet.totalNumberOfPackets = (int) Math.ceil((double) senderVariableSet.commandLineByteArray.length / 1013) + 1;

                // Three-way handshake process
                while (!senderVariableSet.receivedConnectionRequest | !senderVariableSet.receivedCommandRequest) {
                    if (!senderVariableSet.receivedConnectionRequest) {
                        System.out.println("\nSend connection request");

                        sendConnectionRequest(channel, routerAddress, serverAddress, false);
                    } else {
                        if (!senderVariableSet.receivedCommandRequest) {
                            System.out.println("\nSend commend request");

                            sendCommandRequest(channel, routerAddress, serverAddress, false);
                        }
                    }

                    // Start the timer
                    selector.select(Attributes.timeout);

                    Set<SelectionKey> keys = selector.selectedKeys();

                    // Time out
                    if (keys.isEmpty()) {
                        System.out.println("\nTime out");

                        if (!senderVariableSet.receivedConnectionRequest) {
                            System.out.println("\nResend connection request");

                            sendConnectionRequest(channel, routerAddress, serverAddress, true);
                        } else {
                            if (!senderVariableSet.receivedCommandRequest) {
                                System.out.println("\nResend commend request");

                                sendCommandRequest(channel, routerAddress, serverAddress, true);
                            }
                        }
                    } else {
                        // Receives packet from server
                        ByteBuffer buffer = ByteBuffer.allocate(Packet.MAX_LEN);
                        SocketAddress router = channel.receive(buffer);
                        buffer.flip();

                        Packet responsePacket = Packet.fromBuffer(buffer);

                        System.out.println("\nReceived packet: " + responsePacket);
                        System.out.println("Packet type: " + responsePacket.getType());
                        System.out.println("Sequence number: " + responsePacket.getSequenceNumber());
                        System.out.println("Peer address: " + responsePacket.getPeerAddress());
                        System.out.println("Peer port number: " + responsePacket.getPeerPort());

                        String payload = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);

                        // Remove empty bytes from the string
                        payload = payload.replaceAll("\u0000.*", "");

                        System.out.println("Payload: " + payload);

                        if (responsePacket.getType() == 1 & payload.compareTo("connectionrequest") == 0 & !senderVariableSet.receivedConnectionRequest)
                            senderVariableSet.receivedConnectionRequest = true;

                        if (senderVariableSet.receivedConnectionRequest & !senderVariableSet.receivedCommandRequest)
                            if (responsePacket.getType() == 1 & payload.compareTo("commandrequest") == 0)
                                senderVariableSet.receivedCommandRequest = true;
                    }

                    keys.clear();
                }

                // Packet sending process
                if (senderVariableSet.receivedConnectionRequest & senderVariableSet.receivedCommandRequest) {
                    // Keep sending packets until all received
                    while (!senderVariableSet.allPacketsReceived) {
                        sendAllPacketsInWindow(channel, routerAddress, serverAddress, false);

                        if (!senderVariableSet.sentAllPacketsWithinWindow)
                            System.out.println("\nSent all packets within the window to server");

                        // Start the timer
                        selector.select(Attributes.timeout);

                        Set<SelectionKey> keys = selector.selectedKeys();

                        // Time out
                        if (keys.isEmpty()) {
                            System.out.println("\nTime out");

                            System.out.println(senderVariableSet.allPacketsSent);
                            System.out.println(senderVariableSet.allPacketsReceived);

                            resetStateArray();

                            System.out.println("Selectively resend packets within the window to server");

                            sendAllPacketsInWindow(channel, routerAddress, serverAddress, true);
                        } else {
                            // Receives packet from server
                            ByteBuffer buffer = ByteBuffer.allocate(Packet.MAX_LEN);
                            SocketAddress router = channel.receive(buffer);
                            buffer.flip();

                            Packet responsePacket = Packet.fromBuffer(buffer);

                            int sequenceNumber = (int) responsePacket.getSequenceNumber();

                            System.out.println("\nReceived packet: " + responsePacket);
                            System.out.println("Packet type: " + responsePacket.getType());
                            System.out.println("Sequence number: " + sequenceNumber);
                            System.out.println("Peer address: " + responsePacket.getPeerAddress());
                            System.out.println("Peer port number: " + responsePacket.getPeerPort());

                            String payload = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);

                            System.out.println("Payload: " + payload);

                            boolean validPacket = verifyPacket(sequenceNumber);

                            if (validPacket) {
                                // Check if it is an ACK
                                if (responsePacket.getType() == 2) {
                                    setAcknowledgementArray(sequenceNumber);

                                    // Slide the window as much as possible
                                    for (int indedx = 0; indedx < Attributes.windowSize; indedx++)
                                        slideWindow();

                                    // Check if all packets have been received
                                    if (senderVariableSet.allPacketsSent) {
                                        senderVariableSet.allPacketsReceived = true;

                                        // The window should be completely empty if all packets have been sent
                                        for (int windowIndex = 0; windowIndex < Attributes.windowSize; windowIndex++) {
                                            if (senderVariableSet.stateArray[windowIndex] | senderVariableSet.acknowledgementArray[windowIndex]) {
                                                senderVariableSet.allPacketsReceived = false;
                                                break;
                                            }
                                        }

                                        if (senderVariableSet.allPacketsReceived)
                                            selector.close();
                                    }
                                }

                                for (int windowIndex = 0; windowIndex < Attributes.windowSize; windowIndex++)
                                    System.out.println(senderVariableSet.stateArray[windowIndex] + " $$");

                                System.out.println("");

                                for (int windowIndex = 0; windowIndex < Attributes.windowSize; windowIndex++)
                                    System.out.println(senderVariableSet.acknowledgementArray[windowIndex] + " @@");
                            }
                        }

                        keys.clear();
                    }
                }

                System.out.println("Merde");
            }
        }
    }

    private static void sendConnectionRequest(DatagramChannel channel, SocketAddress routerAddress, InetSocketAddress serverAddress, boolean resendPacket) throws IOException {
        Packet packet = createPacket(0, 0, serverAddress.getAddress(), serverAddress.getPort(), "connectionrequest".getBytes());

        channel.send(packet.toBuffer(), routerAddress);

        if (!resendPacket)
            System.out.println("\nSend packet: " + packet);
        else
            System.out.println("\nResend packet: " + packet);

        System.out.println("Packet type: " + packet.getType());
        System.out.println("Sequence number: 0");
        System.out.println("Peer address: " + packet.getPeerAddress());
        System.out.println("Peer port number: " + packet.getPeerPort());
        System.out.println("Payload: " + new String(packet.getPayload(), StandardCharsets.UTF_8));
    }

    private static void sendCommandRequest(DatagramChannel channel, SocketAddress routerAddress, InetSocketAddress serverAddress, boolean resendPacket) throws IOException {
        Packet packet = createPacket(0, 0, serverAddress.getAddress(), serverAddress.getPort(), "commandrequest".getBytes());

        channel.send(packet.toBuffer(), routerAddress);

        if (!resendPacket)
            System.out.println("\nSend packet: " + packet);
        else
            System.out.println("\nResend packet: " + packet);

        System.out.println("Packet type: " + packet.getType());
        System.out.println("Sequence number: 0");
        System.out.println("Peer address: " + packet.getPeerAddress());
        System.out.println("Peer port number: " + packet.getPeerPort());
        System.out.println("Payload: " + new String(packet.getPayload(), StandardCharsets.UTF_8));
    }

    private static void sendAllPacketsInWindow(DatagramChannel channel, SocketAddress routerAddress, InetSocketAddress serverAddress, boolean resendPacket) throws IOException {
        senderVariableSet.sentAllPacketsWithinWindow = false;

        for (int windowIndex = 0; windowIndex < Attributes.windowSize; windowIndex++) {
            if (!senderVariableSet.stateArray[windowIndex] & !senderVariableSet.acknowledgementArray[windowIndex]) {
                int sequenceNumber = senderVariableSet.windowStartIndex + windowIndex;

                // If sequence number exceeds the double window size, clamp it back to the valid range
                if (sequenceNumber >= Attributes.windowSize * 2)
                    sequenceNumber %= Attributes.windowSize;

                int packetId = 0;

                // Check if the window covers two sequence number sections
                if (senderVariableSet.windowEndIndex < senderVariableSet.windowStartIndex) {
                    if (sequenceNumber > senderVariableSet.windowEndIndex)
                        packetId = (senderVariableSet.n - 1) * Attributes.windowSize * 2 + sequenceNumber;
                    else
                        packetId = senderVariableSet.n * Attributes.windowSize * 2 + sequenceNumber;
                } else {
                    packetId = senderVariableSet.n * Attributes.windowSize * 2 + sequenceNumber;
                }

                if (packetId >= senderVariableSet.totalNumberOfPackets)
                    break;

                byte[] payloadByteArray = new byte[1013];

                // The last one is the end packet, not a data packet
                if (packetId == senderVariableSet.totalNumberOfPackets - 1) {
                    // Send the end packet to indicate this is the last packet
                    Packet packet = createPacket(4, sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), "udppacketend".getBytes());

                    channel.send(packet.toBuffer(), routerAddress);

                    if (!resendPacket)
                        System.out.println("\nSend packet: " + packet);
                    else
                        System.out.println("\nResend packet: " + packet);

                    System.out.println("Packet type: " + packet.getType());
                    System.out.println("Sequence number: " + sequenceNumber);
                    System.out.println("Peer address: " + packet.getPeerAddress());
                    System.out.println("Peer port number: " + packet.getPeerPort());
                    System.out.println("Payload: " + new String(packet.getPayload(), StandardCharsets.UTF_8));

                    System.out.println("SSSSSSSSSS " + sequenceNumber + ", " + packetId + ", " + windowIndex + ", " + senderVariableSet.n);

                    senderVariableSet.allPacketsSent = true;
                } else {
                    // Check if the corresponding byte section has length of 1013
                    if (packetId * 1013 + 1013 > senderVariableSet.commandLineByteArray.length) {
                        // If the length of remaining part is less than 1013, extract the rest of it
                        for (int index = 0; index < senderVariableSet.commandLineByteArray.length % 1013; index++)
                            payloadByteArray[index] = senderVariableSet.commandLineByteArray[packetId * 1013 + index];

                        System.out.println("\n### " + new String(payloadByteArray, StandardCharsets.UTF_8));
                    } else {
                        // Extract the substring with 1013 bytes of length
                        for (int index = 0; index < 1013; index++)
                            payloadByteArray[index] = senderVariableSet.commandLineByteArray[packetId * 1013 + index];

                        System.out.println("\n&&& " + new String(payloadByteArray, StandardCharsets.UTF_8));
                    }

                    Packet packet = createPacket(4, sequenceNumber, serverAddress.getAddress(), serverAddress.getPort(), payloadByteArray);

                    channel.send(packet.toBuffer(), routerAddress);

                    if (!resendPacket)
                        System.out.println("\nSend packet: " + packet);
                    else
                        System.out.println("\nResend packet: " + packet);

                    System.out.println("Packet type: " + packet.getType());
                    System.out.println("Sequence number: " + sequenceNumber);
                    System.out.println("Peer address: " + packet.getPeerAddress());
                    System.out.println("Peer port number: " + packet.getPeerPort());
                    System.out.println("Payload: " + new String(packet.getPayload(), StandardCharsets.UTF_8));

                    System.out.println("SSSSSSSSSS " + sequenceNumber + ", " + packetId + ", " + windowIndex + ", " + senderVariableSet.n);
                }

                senderVariableSet.stateArray[windowIndex] = true;
            }
        }

        System.out.println("Index: " + senderVariableSet.windowStartIndex + ", " + senderVariableSet.windowEndIndex);

        senderVariableSet.sentAllPacketsWithinWindow = true;
    }

    private static void setAcknowledgementArray(int sequenceNumber) {
        int index = 0;

        // Convert sequence number to window index
        if (sequenceNumber > senderVariableSet.windowEndIndex) {
            index = sequenceNumber - senderVariableSet.windowStartIndex;
            System.out.println(index + " >>>>>>>>> " + sequenceNumber);
        } else {
            index = Attributes.windowSize - 1 - (senderVariableSet.windowEndIndex - sequenceNumber);
            System.out.println(index + " <<<<<<<<<<<<< " + sequenceNumber);
        }

        if (senderVariableSet.stateArray[index] & !senderVariableSet.acknowledgementArray[index]) {
            senderVariableSet.acknowledgementArray[index] = true;
            System.out.println(senderVariableSet.acknowledgementArray[index]);
        }
    }

    private static void resetStateArray() {
        senderVariableSet.allPacketsReceived = false;

        for (int index = 0; index < senderVariableSet.stateArray.length; index++)
            senderVariableSet.stateArray[index] = false;
    }

    // Slide the window to the right by 1 step
    private static void slideWindow() {
        // First element in the window has to be true, otherwise cannot slide the window
        if (senderVariableSet.acknowledgementArray[0]) {
            senderVariableSet.windowStartIndex += 1;
            senderVariableSet.windowEndIndex += 1;

            // If the index reaches the maximum of sequence number, reset it
            if (senderVariableSet.windowStartIndex == Attributes.windowSize * 2) {
                senderVariableSet.windowStartIndex = 0;
            } else if (senderVariableSet.windowEndIndex == Attributes.windowSize * 2) {
                senderVariableSet.windowEndIndex = 0;

                senderVariableSet.n += 1;
            }

            shiftStateArray();
        }
    }

    // Shift all the elements in the window to the left by 1 step
    private static void shiftStateArray() {
        for (int index = 1; index < senderVariableSet.stateArray.length; index++) {
            senderVariableSet.stateArray[index - 1] = senderVariableSet.stateArray[index];
            senderVariableSet.acknowledgementArray[index - 1] = senderVariableSet.acknowledgementArray[index];
        }

        senderVariableSet.stateArray[senderVariableSet.stateArray.length - 1] = false;
        senderVariableSet.acknowledgementArray[senderVariableSet.acknowledgementArray.length - 1] = false;
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

    private static boolean verifyPacket(int sequenceNumber) {
        if (senderVariableSet.windowEndIndex < senderVariableSet.windowStartIndex) {
            if (sequenceNumber > senderVariableSet.windowEndIndex & sequenceNumber < senderVariableSet.windowStartIndex)
                return false;
        } else {
            if (sequenceNumber < senderVariableSet.windowStartIndex | sequenceNumber > senderVariableSet.windowEndIndex)
                return false;
        }

        return true;
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
                .defaultsTo("8080");

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