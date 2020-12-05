package ca.concordia.udp;

import java.util.ArrayList;
import java.util.List;

public class ReceiverVariableSet {
    public boolean receivedConnectionRequest = false, receivedCommandRequest = false;

    public int windowStartIndex = 0, windowEndIndex = Attributes.windowSize - 1;

    public boolean[] stateArray = new boolean[Attributes.windowSize];

    public boolean allPacketsReceived = false;

    public boolean endPacketReceived = false;

    public boolean[] packetBufferStateArray = new boolean[Attributes.windowSize];

    public String[] packetBufferArray = new String[Attributes.windowSize];

    public List<String> requestBufferList = new ArrayList<String>();

    public boolean generatedCompleteCommand = false;

    public boolean startParsingCommendLine = false;

    public String commandString = "";

    public boolean receivedResponse = false;

    public void initializeAttributes() {
        windowStartIndex = 0;

        windowEndIndex = Attributes.windowSize - 1;

        receivedConnectionRequest = false;

        receivedCommandRequest = false;

        stateArray = new boolean[Attributes.windowSize];

        allPacketsReceived = false;

        endPacketReceived = false;

        packetBufferStateArray = new boolean[Attributes.windowSize];

        packetBufferArray = new String[Attributes.windowSize];

        requestBufferList = new ArrayList<String>();

        startParsingCommendLine = false;

        generatedCompleteCommand = false;

        receivedResponse = false;
    }
}
