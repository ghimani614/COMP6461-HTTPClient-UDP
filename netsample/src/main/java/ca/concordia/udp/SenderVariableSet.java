package ca.concordia.udp;

public class SenderVariableSet {
    public boolean keepSending = false;

    public boolean receivedConnectionRequest = false, receivedCommandRequest = false;

    public int windowStartIndex = 0, windowEndIndex = Attributes.windowSize - 1;

    public boolean[] stateArray = new boolean[Attributes.windowSize], acknowledgementArray = new boolean[Attributes.windowSize];

    public boolean sentAllPacketsWithinWindow = false;

    public boolean allPacketsSent = false, allPacketsReceived = false;

    public int totalNumberOfPackets = 1;

    public int n = 0;

    public byte[] commandLineByteArray;

    public void initializeAttributes() {
        keepSending = false;

        receivedConnectionRequest = false;

        receivedCommandRequest = false;

        windowStartIndex = 0;

        windowEndIndex = Attributes.windowSize - 1;

        stateArray = new boolean[Attributes.windowSize];

        acknowledgementArray = new boolean[Attributes.windowSize];

        sentAllPacketsWithinWindow = false;

        allPacketsSent = false;

        allPacketsReceived = false;

        n = 0;
    }
}
