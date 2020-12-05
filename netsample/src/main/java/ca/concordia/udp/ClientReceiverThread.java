package ca.concordia.udp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ClientReceiverThread extends Thread {
    private SocketAddress routerAddress = null;

    public int port = 0;

    public ReceiverVariableSet receiverVariableSet1 = new ReceiverVariableSet();

    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);

    private void listenAndServe(int port) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));

            logger.info("Client receiver is listening at {}", channel.getLocalAddress());

            ByteBuffer buffer = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            receiverVariableSet1.initializeAttributes();

            while (true) {
                buffer.clear();
                routerAddress = channel.receive(buffer);

                // Parse a packet from the received raw data.
                buffer.flip();
                Packet packet = Packet.fromBuffer(buffer);
                buffer.flip();

                int sequenceNumber = (int) packet.getSequenceNumber();

                String peerAddress = packet.getPeerAddress().toString().replaceAll("\u0000.*", "");
                peerAddress += ":" + packet.getPeerPort();

                String payload = new String(packet.getPayload(), UTF_8);

                // Remove empty bytes from the string
                payload = payload.replaceAll("\u0000.*", "");

                if (!receiverVariableSet1.allPacketsReceived) {
                    System.out.println("\nReceived packet: " + packet);
                    System.out.println("Packet type: " + packet.getType());
                    System.out.println("Sequence number: " + sequenceNumber);
                    System.out.println("Peer address: " + peerAddress);
                    System.out.println("Peer port number: " + packet.getPeerPort());
                    System.out.println("Payload: " + payload);
                } else {
                    System.out.println("\nIgnore duplicated or seriously delayed packets");
                }

                // Three-way handshake process
                if (!receiverVariableSet1.receivedConnectionRequest | !receiverVariableSet1.receivedCommandRequest) {
                    if (packet.getType() == 0 & payload.compareTo("connectionrequest") == 0 & !receiverVariableSet1.receivedConnectionRequest)
                        receiverVariableSet1.receivedConnectionRequest = true;

                    if (receiverVariableSet1.receivedConnectionRequest & !receiverVariableSet1.receivedCommandRequest)
                        if (packet.getType() == 0 & payload.compareTo("commandrequest") == 0)
                            receiverVariableSet1.receivedCommandRequest = true;

                    Packet responsePacket = packet.toBuilder()
                            .setType(1)
                            .setPayload(payload.getBytes())
                            .create();

                    channel.send(responsePacket.toBuffer(), routerAddress);
                } else {
                    // Packet receiving process(after three-way handshake process)
                    if (!receiverVariableSet1.generatedCompleteCommand) {
                        boolean validSequenceNumber = verifySequenceNumber(sequenceNumber);

                        if (packet.getType() == 4 & validSequenceNumber) {
                            int windowIndex = 0;

                            // Convert sequence number to window index
                            if (sequenceNumber > receiverVariableSet1.windowEndIndex)
                                windowIndex = sequenceNumber - receiverVariableSet1.windowStartIndex;
                            else
                                windowIndex = Attributes.windowSize - 1 - (receiverVariableSet1.windowEndIndex - sequenceNumber);

                            if (payload.compareTo("udppacketend") == 0) {
                                receiverVariableSet1.endPacketReceived = true;

                                if (!receiverVariableSet1.stateArray[windowIndex])
                                    receiverVariableSet1.stateArray[windowIndex] = true;

                                insertPacketToBufferArray(windowIndex, "");
                            } else {
                                if (!receiverVariableSet1.stateArray[windowIndex])
                                    receiverVariableSet1.stateArray[windowIndex] = true;

                                insertPacketToBufferArray(windowIndex, payload);
                            }

                            // Slide the window as much as possible
                            for (int index = 0; index < Attributes.windowSize; index++)
                                slideWindow();


                            if (receiverVariableSet1.endPacketReceived) {
                                // Check if all packets have been received
                                receiverVariableSet1.allPacketsReceived = true;

                                // The window should be completely empty if all packets have been sent
                                for (int index = 0; index < Attributes.windowSize; index++) {
                                    if (receiverVariableSet1.stateArray[index] | receiverVariableSet1.packetBufferStateArray[index]) {
                                        receiverVariableSet1.allPacketsReceived = false;

                                        break;
                                    }
                                }

                                if (receiverVariableSet1.allPacketsReceived)
                                    receiverVariableSet1.commandString = generateCompleteCommand();
                            }

                            Packet responsePacket = packet.toBuilder()
                                    .setType(2)
                                    .setPayload(payload.getBytes())
                                    .create();

                            channel.send(responsePacket.toBuffer(), routerAddress);
                        } else {
                            int packetType = 0;

                            if (packet.getType() == 0)
                                packetType = 1;
                            else
                                packetType = 2;

                            Packet responsePacket = packet.toBuilder()
                                    .setType(packetType)
                                    .setPayload(payload.getBytes())
                                    .create();

                            channel.send(responsePacket.toBuffer(), routerAddress);
                        }
                    }

                    if (receiverVariableSet1.allPacketsReceived) {
                        if (!receiverVariableSet1.startParsingCommendLine) {
                            System.out.println("\nReceived all packets");

                            System.out.println("\nResponse:");

                            System.out.println("---------------------------------------");

                            System.out.println(receiverVariableSet1.commandString);

                            receiverVariableSet1.startParsingCommendLine = true;
                        } else {
                            int packetType = 0;

                            if (packet.getType() == 0)
                                packetType = 1;
                            else
                                packetType = 2;

                            Packet responsePacket = packet.toBuilder()
                                    .setType(packetType)
                                    .setPayload(payload.getBytes())
                                    .create();

                            channel.send(responsePacket.toBuffer(), routerAddress);
                        }
                    }
                }
            }

        }
    }

    private void insertPacketToBufferArray(int windowIndex, String payload) {
        // Only insert received and not buffered packets
        if (receiverVariableSet1.stateArray[windowIndex] & !receiverVariableSet1.packetBufferStateArray[windowIndex]) {
            receiverVariableSet1.packetBufferArray[windowIndex] = payload.replaceAll("\u0000.*", "");

            receiverVariableSet1.packetBufferStateArray[windowIndex] = true;
        }
    }

    // Slide the window to the right by 1 step
    private void slideWindow() {
        // First element in the window has to be true, otherwise cannot slide the window
        if (receiverVariableSet1.stateArray[0]) {
            receiverVariableSet1.windowStartIndex += 1;
            receiverVariableSet1.windowEndIndex += 1;

            // If the index reaches the maximum of sequence number, reset it
            if (receiverVariableSet1.windowStartIndex == Attributes.windowSize * 2)
                receiverVariableSet1.windowStartIndex = 0;
            else if (receiverVariableSet1.windowEndIndex == Attributes.windowSize * 2)
                receiverVariableSet1.windowEndIndex = 0;

            receiverVariableSet1.requestBufferList.add(receiverVariableSet1.packetBufferArray[0]);

            shiftStateArray();
        }
    }

    // Shift all the elements in the window to the left by 1 step
    private void shiftStateArray() {
        for (int index = 1; index < receiverVariableSet1.stateArray.length; index++) {
            receiverVariableSet1.stateArray[index - 1] = receiverVariableSet1.stateArray[index];
            receiverVariableSet1.packetBufferStateArray[index - 1] = receiverVariableSet1.packetBufferStateArray[index];

            receiverVariableSet1.packetBufferArray[index - 1] = receiverVariableSet1.packetBufferArray[index];
        }

        receiverVariableSet1.stateArray[receiverVariableSet1.stateArray.length - 1] = false;
        receiverVariableSet1.packetBufferStateArray[receiverVariableSet1.packetBufferStateArray.length - 1] = false;
        receiverVariableSet1.packetBufferArray[receiverVariableSet1.packetBufferArray.length - 1] = "";
    }

    private boolean verifySequenceNumber(int sequenceNumber) {
        if (receiverVariableSet1.windowEndIndex < receiverVariableSet1.windowStartIndex) {
            if (sequenceNumber > receiverVariableSet1.windowEndIndex & sequenceNumber < receiverVariableSet1.windowStartIndex)
                return false;
        } else {
            if (sequenceNumber < receiverVariableSet1.windowStartIndex | sequenceNumber > receiverVariableSet1.windowEndIndex)
                return false;
        }

        return true;
    }

    private String generateCompleteCommand() {
        receiverVariableSet1.commandString = "";

        for (int index = 0; index < receiverVariableSet1.requestBufferList.size(); index++)
            receiverVariableSet1.commandString += receiverVariableSet1.requestBufferList.get(index);

        receiverVariableSet1.generatedCompleteCommand = true;

        return receiverVariableSet1.commandString;
    }

    public void run() {
        try {
            listenAndServe(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
