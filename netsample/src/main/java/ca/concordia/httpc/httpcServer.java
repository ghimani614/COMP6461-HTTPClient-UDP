package ca.concordia.httpc;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ForkJoinPool;

import static java.util.Arrays.asList;

import java.util.HashMap;

import org.json.simple.*;
import org.json.simple.parser.*;

public class httpcServer {

    private static final Logger logger = LoggerFactory.getLogger(ca.concordia.echo.BlockingEchoServer.class);

    private String currentURL = "";
    private String redirectedURL = "", redirectionResultString = "";

    private String verbosityString = "";

    private HashMap<String, String> headerKeyValuePairHashMap;

    private void readEchoAndRepeat(SocketChannel socket) {
        try (SocketChannel client = socket) {
            ByteBuffer buf = ByteBuffer.allocate(2048);
            for (; ; ) {
                int nr = client.read(buf);

                // Convert byte array to string
                String requestString = new String(buf.array(), StandardCharsets.UTF_8);
                System.out.println("Client request: " + requestString);

                buf.clear();

                for (int i = 0; i < buf.array().length; i++)
                    buf.put((byte) 0);

                String responseString = parseCommandLine(requestString);

                if (nr == -1)
                    break;

                if (nr > 0) {

                    buf.clear();
                    buf.position(0);

                    // ByteBuffer is tricky, you have to flip when switch from read to write, or
                    // vice-versa
                    buf.flip();

                    client.write(ByteBuffer.wrap(responseString.getBytes(StandardCharsets.UTF_8)));
                    buf.clear();
                }
            }
        } catch (IOException e) {
            logger.error("Echo error {}", e);
        }
    }

    private String parseCommandLine(String commandLineString) {
        commandLineString = preprocessCommandLine(commandLineString);

        String[] commandLineStringArray = commandLineString.split(" ");

        if (commandLineStringArray.length > 0) {
            // Check the starting word, must start with httpc
            if (commandLineStringArray[0].compareTo("httpc") != 0) {
                return "Invalid syntax";
            } else {
                String urlString;

                if (commandLineStringArray.length == 2) {
                    if (compareStringsWithChar("help", commandLineStringArray[1]))
                        return "httpc is a curl-like application but supports HTTP protocol only.\nUsage:\n    httpc command [arguments]\nThe commands are:\n    get executes a HTTP GET request and prints the response.\n    post executes a HTTP POST request and prints the response.\n    help prints this screen.\n\n" +
                                "Use \"httpc help [command]\" for more information about a command.";
                } else if (commandLineStringArray.length > 2) {
                    if (compareStringsWithChar("help", commandLineStringArray[1])) {
                        if (compareStringsWithChar("get", commandLineStringArray[2]))
                            return "usage: httpc get [-v] [-h key:value] URL \n\n" + "Get executes a HTTP GET request for a given URL.\n\n   -v             Prints the detail of the response such as protocol, status, and headers.\n   -h key:value   Associates headers to HTTP Request with the format 'key:value'.";
                        else if (compareStringsWithChar("post", commandLineStringArray[2]))
                            return "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n\nPost executes a HTTP POST request for a given URL with inline data or from file.\n\n   -v             Prints the detail of the response such as protocol, status, and headers.\n   -h key:value   Associates headers to HTTP Request with the format 'key:value'.\n   -d string      Associates an inline data to the body HTTP POST request.\n   -f file        Associates the content of a file to the body HTTP POST request.\n\nEither [-d] or [-f] can be used but not both.";

                        return "Invalid syntax";
                    } else if (compareStringsWithChar("get", commandLineStringArray[1])) {
                        if (compareStringsWithChar("-v", commandLineStringArray[2])) {
                            // httpc get -v url
                            if (!verifyURL(commandLineStringArray[3]))
                                return "Invalid syntax";

                            urlString = currentURL;

                            // Test the URL to redirect or not
                            int redirectionResultCode = detectRedirection(urlString);

                            if (redirectionResultCode == -1)
                                return "Redirection errors.";
                            else if (redirectionResultCode == 0)
                                redirectionResultString = "No redirection detected\n";
                            else if (redirectionResultCode == 1)
                                urlString = redirectedURL;

                            return redirectionResultString + "\n" + getHeaderValueByKey(urlString, null) + "\nServer: " + getHeaderValueByKey(urlString, "Server") + "\nDate: " + getHeaderValueByKey(urlString, "Date") + "\nContent-Type: " + getHeaderValueByKey(urlString, "Content-Type") + "\nContent-Length: " + getHeaderValueByKey(urlString, "Content-Length") + "\nConnection: " + getHeaderValueByKey(urlString, "Connection") + "\nAccess-Control-Allow-Origin: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Origin") + "\nAccess-Control-Allow-Credentials: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Credentials") + "\n\n" + getHttpResponse(urlString);
                        } else if (compareStringsWithChar("-h", commandLineStringArray[2])) {
                            // httpc get -h key:value url
                            if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                return "Invalid syntax";

                            urlString = currentURL;

                            // Test the URL to redirect or not
                            int redirectionResultCode = detectRedirection(urlString);

                            if (redirectionResultCode == -1)
                                return "Redirection errors.";
                            else if (redirectionResultCode == 0)
                                redirectionResultString = "No redirection detected\n";
                            else if (redirectionResultCode == 1)
                                urlString = redirectedURL;

                            verbosityString = "";

                            // There could be multiple header parameters
                            if (!extractHeaderParameters(commandLineStringArray, 3, commandLineStringArray.length - 1))
                                return "Invalid syntax";

                            int index = 0;

                            for (String keyString : headerKeyValuePairHashMap.keySet()) {
                                // Append each key search to the return string
                                verbosityString += keyString + ": " + getHeaderValueByKey(urlString, keyString);

                                // Append a new line except for the last line
                                if (index != headerKeyValuePairHashMap.size() - 1)
                                    verbosityString += "\n";

                                index += 1;
                            }

                            return redirectionResultString + "\n" + verbosityString;
                        } else {
                            // httpc get url

                            if (!verifyURL(commandLineStringArray[2]))
                                return "Invalid syntax";

                            urlString = currentURL;

                            // Test the URL to redirect or not
                            int redirectionResultCode = detectRedirection(urlString);

                            if (redirectionResultCode == -1)
                                return "Redirection errors.";
                            else if (redirectionResultCode == 0)
                                redirectionResultString = "No redirection detected\n";
                            else if (redirectionResultCode == 1)
                                urlString = redirectedURL;

                            return redirectionResultString + "\n" + getHttpResponse(urlString);
                        }
                    } else if (compareStringsWithChar("post", commandLineStringArray[1])) {
                        if (compareStringsWithChar("-v", commandLineStringArray[2])) {
                            if (compareStringsWithChar("-h", commandLineStringArray[3])) {
                                if (commandLineStringArray.length >= 6 & !compareStringsWithChar("-d", commandLineStringArray[commandLineStringArray.length - 3]) & !compareStringsWithChar("-f", commandLineStringArray[commandLineStringArray.length - 3])) {
                                    // httpc post -v -h key:value url

                                    if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                        return "Invalid syntax";

                                    urlString = currentURL;

                                    // Test the URL to redirect or not
                                    int redirectionResultCode = detectRedirection(urlString);

                                    if (redirectionResultCode == -1)
                                        return "Redirection errors.";
                                    else if (redirectionResultCode == 0)
                                        redirectionResultString = "No redirection detected\n";
                                    else if (redirectionResultCode == 1)
                                        urlString = redirectedURL;

                                    // There could be multiple header parameters
                                    if (!extractHeaderParameters(commandLineStringArray, 4, commandLineStringArray.length - 1))
                                        return "Invalid syntax";

                                    // Provided data
                                    // header parameter list: headerKeyValuePairHashMap
                                    // url: urlString

                                    // For debugging
                                    for (String keyString : headerKeyValuePairHashMap.keySet())
                                        System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                    verbosityString = getHeaderValueByKey(urlString, null) + "\nServer: " + getHeaderValueByKey(urlString, "Server") + "\nDate: " + getHeaderValueByKey(urlString, "Date") + "\nContent-Type: " + getHeaderValueByKey(urlString, "Content-Type") + "\nContent-Length: " + getHeaderValueByKey(urlString, "Content-Length") + "\nConnection: " + getHeaderValueByKey(urlString, "Connection") + "\nAccess-Control-Allow-Origin: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Origin") + "\nAccess-Control-Allow-Credentials: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Credentials") + "\n";

                                    return redirectionResultString + "\n" + verbosityString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, null);
                                } else if (compareStringsWithChar("-d", commandLineStringArray[commandLineStringArray.length - 3])) {
                                    // httpc post -v -h key:value -d "inline data" url

                                    // Verify the format of inline data

                                    // Remove empty bytes from the string
                                    String inlineDataString = commandLineStringArray[commandLineStringArray.length - 2].replaceAll("\u0000.*", "");

                                    // Check if it is empty
                                    if (!compareStringsWithChar("", inlineDataString)) {
                                        // Check the inline data format, it should be wrapped by a pair of apostrophes
                                        if (inlineDataString.charAt(0) == 39 & inlineDataString.charAt(inlineDataString.length() - 1) == 39) {
                                            // Remove the apostrophes around the url
                                            inlineDataString = inlineDataString.replaceAll("'", "");

                                            // Inside the the pair of apostrophes, it should be wrapped by a pair of curly brackets
                                            if (inlineDataString.charAt(0) == 123 & inlineDataString.charAt(inlineDataString.length() - 1) == 125) {

                                                boolean hasOneLeftCurlyBracket = false;
                                                boolean hasOneRightCurlyBracket = false;

                                                // Check if there is only one left and right curly bracket, otherwise it is invalid syntax
                                                for (int characterIndex = 0; characterIndex < inlineDataString.length() - 1; characterIndex++) {
                                                    if (inlineDataString.charAt(characterIndex) == 123) {
                                                        if (!hasOneLeftCurlyBracket)
                                                            hasOneLeftCurlyBracket = true;
                                                        else
                                                            return "Invalid syntax";
                                                    }

                                                    if (inlineDataString.charAt(characterIndex) == 125) {
                                                        if (!hasOneRightCurlyBracket)
                                                            hasOneRightCurlyBracket = true;
                                                        else
                                                            return "Invalid syntax";
                                                    }
                                                }

                                                if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                                    return "Invalid syntax";

                                                urlString = currentURL;

                                                // Test the URL to redirect or not
                                                int redirectionResultCode = detectRedirection(urlString);

                                                if (redirectionResultCode == -1)
                                                    return "Redirection errors.";
                                                else if (redirectionResultCode == 0)
                                                    redirectionResultString = "No redirection detected\n";
                                                else if (redirectionResultCode == 1)
                                                    urlString = redirectedURL;

                                                // There could be multiple header parameters
                                                if (!extractHeaderParameters(commandLineStringArray, 4, commandLineStringArray.length - 3))
                                                    return "Invalid syntax";

                                                // Provided data
                                                // header parameter list: headerKeyValuePairHashMap
                                                // inline data: inlineDataString
                                                // url: urlString

                                                // For debugging
                                                for (String keyString : headerKeyValuePairHashMap.keySet())
                                                    System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                                verbosityString = getHeaderValueByKey(urlString, null) + "\nServer: " + getHeaderValueByKey(urlString, "Server") + "\nDate: " + getHeaderValueByKey(urlString, "Date") + "\nContent-Type: " + getHeaderValueByKey(urlString, "Content-Type") + "\nContent-Length: " + getHeaderValueByKey(urlString, "Content-Length") + "\nConnection: " + getHeaderValueByKey(urlString, "Connection") + "\nAccess-Control-Allow-Origin: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Origin") + "\nAccess-Control-Allow-Credentials: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Credentials") + "\n";

                                                return redirectionResultString + "\n" + verbosityString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, inlineDataString);
                                            } else {
                                                return "Invalid syntax";
                                            }
                                        } else {
                                            return "Invalid syntax";
                                        }
                                    } else {
                                        return "Invalid syntax";
                                    }
                                } else if (compareStringsWithChar("-f", commandLineStringArray[commandLineStringArray.length - 3])) {
                                    // httpc post -v -h key:value -f "file name" url

                                    String jsonFileContentString = readJSONFile(commandLineStringArray[commandLineStringArray.length - 2]);

                                    if (jsonFileContentString == "Failed")
                                        return "Failed to read JSON file.";

                                    if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                        return "Invalid syntax";

                                    urlString = currentURL;

                                    // Test the URL to redirect or not
                                    int redirectionResultCode = detectRedirection(urlString);

                                    if (redirectionResultCode == -1)
                                        return "Redirection errors.";
                                    else if (redirectionResultCode == 0)
                                        redirectionResultString = "No redirection detected\n";
                                    else if (redirectionResultCode == 1)
                                        urlString = redirectedURL;

                                    // There could be multiple header parameters
                                    if (!extractHeaderParameters(commandLineStringArray, 4, commandLineStringArray.length - 3))
                                        return "Invalid syntax";

                                    // Provided data
                                    // header parameter list: headerKeyValuePairHashMap
                                    // JSON file data: jsonFileContentString
                                    // url: urlString

                                    // For debugging
                                    for (String keyString : headerKeyValuePairHashMap.keySet())
                                        System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                    verbosityString = getHeaderValueByKey(urlString, null) + "\nServer: " + getHeaderValueByKey(urlString, "Server") + "\nDate: " + getHeaderValueByKey(urlString, "Date") + "\nContent-Type: " + getHeaderValueByKey(urlString, "Content-Type") + "\nContent-Length: " + getHeaderValueByKey(urlString, "Content-Length") + "\nConnection: " + getHeaderValueByKey(urlString, "Connection") + "\nAccess-Control-Allow-Origin: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Origin") + "\nAccess-Control-Allow-Credentials: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Credentials") + "\n";

                                    return redirectionResultString + "\n" + verbosityString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, jsonFileContentString);
                                } else {
                                    return "Invalid syntax";
                                }
                            } else {
                                // httpc post -v url

                                // Check if it contains the exact number of terms
                                if (commandLineStringArray.length != 4)
                                    return "Invalid syntax";

                                if (!verifyURL(commandLineStringArray[3]))
                                    return "Invalid syntax";

                                urlString = currentURL;

                                // Test the URL to redirect or not
                                int redirectionResultCode = detectRedirection(urlString);

                                if (redirectionResultCode == -1)
                                    return "Redirection errors.";
                                else if (redirectionResultCode == 0)
                                    redirectionResultString = "No redirection detected\n";
                                else if (redirectionResultCode == 1)
                                    urlString = redirectedURL;

                                // There could be multiple header parameters
                                if (!extractHeaderParameters(commandLineStringArray, 4, commandLineStringArray.length - 3))
                                    return "Invalid syntax";

                                // Provided data
                                // verbose output: hasVerbosityString
                                // url: urlString

                                verbosityString = getHeaderValueByKey(urlString, null) + "\nServer: " + getHeaderValueByKey(urlString, "Server") + "\nDate: " + getHeaderValueByKey(urlString, "Date") + "\nContent-Type: " + getHeaderValueByKey(urlString, "Content-Type") + "\nContent-Length: " + getHeaderValueByKey(urlString, "Content-Length") + "\nConnection: " + getHeaderValueByKey(urlString, "Connection") + "\nAccess-Control-Allow-Origin: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Origin") + "\nAccess-Control-Allow-Credentials: " + getHeaderValueByKey(urlString, "Access-Control-Allow-Credentials") + "\n";

                                return redirectionResultString + "\n" + verbosityString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, null);
                            }
                        } else if (compareStringsWithChar("-h", commandLineStringArray[2])) {
                            if (commandLineStringArray.length >= 5 & !compareStringsWithChar("-d", commandLineStringArray[commandLineStringArray.length - 3]) & !compareStringsWithChar("-f", commandLineStringArray[commandLineStringArray.length - 3])) {
                                // httpc post -h key:value url
                                if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                    return "Invalid syntax";

                                urlString = currentURL;

                                // Test the URL to redirect or not
                                int redirectionResultCode = detectRedirection(urlString);

                                if (redirectionResultCode == -1)
                                    return "Redirection errors.";
                                else if (redirectionResultCode == 0)
                                    redirectionResultString = "No redirection detected\n";
                                else if (redirectionResultCode == 1)
                                    urlString = redirectedURL;

                                // There could be multiple header parameters
                                if (!extractHeaderParameters(commandLineStringArray, 3, commandLineStringArray.length - 1))
                                    return "Invalid syntax";

                                // Provided data
                                // verbose output: hasVerbosityString
                                // header parameter list: headerKeyValuePairHashMap
                                // url: urlString

                                // For debugging
                                for (String keyString : headerKeyValuePairHashMap.keySet())
                                    System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                return redirectionResultString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, null);
                            } else if (compareStringsWithChar("-d", commandLineStringArray[commandLineStringArray.length - 3])) {
                                // httpc post -h key:value -d "inline data" url

                                // Verify the format of inline data

                                // Remove empty bytes from the string
                                String inlineDataString = commandLineStringArray[commandLineStringArray.length - 2].replaceAll("\u0000.*", "");

                                // Check if it is empty
                                if (!compareStringsWithChar("", inlineDataString)) {
                                    // Check the inline data format, it should be wrapped by a pair of apostrophes
                                    if (inlineDataString.charAt(0) == 39 & inlineDataString.charAt(inlineDataString.length() - 1) == 39) {
                                        // Remove the apostrophes around the url
                                        inlineDataString = inlineDataString.replaceAll("'", "");

                                        // Inside the the pair of apostrophes, it should be wrapped by a pair of curly brackets
                                        if (inlineDataString.charAt(0) == 123 & inlineDataString.charAt(inlineDataString.length() - 1) == 125) {

                                            boolean hasOneLeftCurlyBracket = false;
                                            boolean hasOneRightCurlyBracket = false;

                                            // Check if there is only one left and right curly bracket, otherwise it is invalid syntax
                                            for (int characterIndex = 0; characterIndex < inlineDataString.length() - 1; characterIndex++) {
                                                if (inlineDataString.charAt(characterIndex) == 123) {
                                                    if (!hasOneLeftCurlyBracket)
                                                        hasOneLeftCurlyBracket = true;
                                                    else
                                                        return "Invalid syntax";
                                                }

                                                if (inlineDataString.charAt(characterIndex) == 125) {
                                                    if (!hasOneRightCurlyBracket)
                                                        hasOneRightCurlyBracket = true;
                                                    else
                                                        return "Invalid syntax";
                                                }
                                            }

                                            if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                                return "Invalid syntax";

                                            urlString = currentURL;

                                            // Test the URL to redirect or not
                                            int redirectionResultCode = detectRedirection(urlString);

                                            if (redirectionResultCode == -1)
                                                return "Redirection errors.";
                                            else if (redirectionResultCode == 0)
                                                redirectionResultString = "No redirection detected\n";
                                            else if (redirectionResultCode == 1)
                                                urlString = redirectedURL;

                                            // There could be multiple header parameters
                                            if (!extractHeaderParameters(commandLineStringArray, 3, commandLineStringArray.length - 3))
                                                return "Invalid syntax";

                                            // Provided data
                                            // verbose output: hasVerbosityString
                                            // header parameter list: headerKeyValuePairHashMap
                                            // inline data: inlineDataString
                                            // url: urlString

                                            // For debugging
                                            for (String keyString : headerKeyValuePairHashMap.keySet())
                                                System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                            return redirectionResultString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, inlineDataString);
                                        } else {
                                            return "Invalid syntax";
                                        }
                                    } else {
                                        return "Invalid syntax";
                                    }
                                } else {
                                    return "Invalid syntax";
                                }
                            } else if (compareStringsWithChar("-f", commandLineStringArray[commandLineStringArray.length - 3])) {
                                // httpc post -h key:value -f "file name" url

                                String jsonFileContentString = readJSONFile(commandLineStringArray[commandLineStringArray.length - 2]);

                                if (jsonFileContentString == "Failed")
                                    return "Failed to read JSON file.";

                                if (!verifyURL(commandLineStringArray[commandLineStringArray.length - 1]))
                                    return "Invalid syntax";

                                urlString = currentURL;

                                // Test the URL to redirect or not
                                int redirectionResultCode = detectRedirection(urlString);

                                if (redirectionResultCode == -1)
                                    return "Redirection errors.";
                                else if (redirectionResultCode == 0)
                                    redirectionResultString = "No redirection detected\n";
                                else if (redirectionResultCode == 1)
                                    urlString = redirectedURL;

                                // There could be multiple header parameters
                                if (!extractHeaderParameters(commandLineStringArray, 3, commandLineStringArray.length - 3))
                                    return "Invalid syntax";

                                // Provided data
                                // verbose output: hasVerbosityString
                                // header parameter list: headerKeyValuePairHashMap
                                // JSON file data: jsonFileContentString
                                // url: urlString

                                // For debugging
                                for (String keyString : headerKeyValuePairHashMap.keySet())
                                    System.out.println("key: " + keyString + " value: " + headerKeyValuePairHashMap.get(keyString));

                                return redirectionResultString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, jsonFileContentString);
                            } else {
                                return "Invalid syntax";
                            }
                        } else {
                            // httpc post url

                            // Check if it contains the exact number of terms
                            if (commandLineStringArray.length != 3)
                                return "Invalid syntax";
                            //to check and accept JSON format
                            if (!verifyURL(commandLineStringArray[2]))
                                return "Invalid syntax";

                            urlString = currentURL;

                            // Test the URL to redirect or not
                            int redirectionResultCode = detectRedirection(urlString);

                            if (redirectionResultCode == -1)
                                return "Redirection errors.";
                            else if (redirectionResultCode == 0)
                                redirectionResultString = "No redirection detected\n";
                            else if (redirectionResultCode == 1)
                                urlString = redirectedURL;

                            // Provided data
                            // url: urlString

                            return redirectionResultString + "\n" + postHttpResponse(urlString, headerKeyValuePairHashMap, null);
                        }
                    } else if (compareStringsWithChar("-v", commandLineStringArray[1])) {
                        // httpc -v url -o file.txt

                        // Check if it contains the exact number of terms
                        if (commandLineStringArray.length != 5)
                            return "Invalid syntax";

                        if (!verifyURL(commandLineStringArray[2]))
                            return "Invalid syntax";

                        urlString = currentURL;

                        // Test the URL to redirect or not
                        int redirectionResultCode = detectRedirection(urlString);

                        if (redirectionResultCode == -1)
                            return "Redirection errors.";
                        else if (redirectionResultCode == 0)
                            redirectionResultString = "No redirection detected\n";
                        else if (redirectionResultCode == 1)
                            urlString = redirectedURL;

                        // The fourth term should be -o without any exception
                        if (!compareStringsWithChar("-o", commandLineStringArray[3]))
                            return "Invalid syntax";

                        // Remove empty bytes from the file name string
                        String fileName = commandLineStringArray[4].replaceAll("\u0000.*", "");

                        // Write to file
                        boolean result = writeToTextFile(fileName, getHttpResponse(urlString));

                        if (result)
                            return redirectionResultString + "Successfully wrote response to the file.";
                        else
                            return redirectionResultString + "Failed to write the file.";
                    }
                }
            }

            return "Invalid syntax";
        } else {
            return "Invalid syntax";
        }
    }

    private String preprocessCommandLine(String commandLineString) {
        boolean repeat = true;

        while (repeat) {
            repeat = false;

            for (int characterIndex = 0; characterIndex < commandLineString.length() - 1; characterIndex++) {
                if (commandLineString.charAt(characterIndex) == ':' | commandLineString.charAt(characterIndex) == ',') {
                    if (commandLineString.charAt(characterIndex + 1) == ' ') {
                        commandLineString = commandLineString.substring(0, characterIndex + 1) + commandLineString.substring(characterIndex + 2, commandLineString.length());
                        repeat = true;

                        break;
                    }
                }
            }
        }

        return commandLineString;
    }

    private int detectRedirection(String urlString) {
        // Result code
        // -1: Redirection failed
        // 0: No redirection detected
        // 1: Redirection succeeded

        // Initialize attributes
        boolean continueRedirection = true;
        boolean redirectionDetected = false;
        int maximumRedirectionTimes = 5;
        int currentRedirectionTimes = 0;

        redirectionResultString = "";

        redirectedURL = urlString;

        // Redirection loop
        while (continueRedirection & currentRedirectionTimes < maximumRedirectionTimes) {
            continueRedirection = false;

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(redirectedURL).openConnection();
                connection.setReadTimeout(5000);

                boolean redirected = false;

                int status = connection.getResponseCode();

                // Response code starts with 3 is redirection
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirected = true;
                        redirectionDetected = true;
                        continueRedirection = true;

                        redirectionResultString += "Response code: " + status + "\n";

                        currentRedirectionTimes += 1;
                    }
                }

                if (redirected) {
                    // Get redirected url from "location" header field
                    redirectedURL = connection.getHeaderField("Location");

                    // Open the new connection again
                    connection = (HttpURLConnection) new URL(redirectedURL).openConnection();

                    redirectionResultString += "Redirect to URL: " + redirectedURL + "\n";
                }
            } catch (Exception e) {
                e.printStackTrace();

                return -1;
            }
        }

        if (redirectionDetected)
            redirectionResultString += "Total redirection times: " + currentRedirectionTimes + "\n";

        if (!redirectionDetected)
            return 0;
        else
            return 1;
    }

    private boolean extractHeaderParameters(String[] commandLineStringArray, int startingIndex, int endingIndex) {
        headerKeyValuePairHashMap = new HashMap<String, String>();

        for (int index = startingIndex; index < endingIndex; index++) {
            boolean hasOneColon = false;

            // Check if there is exactly one colon in each pair, otherwise it is invalid syntax
            for (int characterIndex = 0; characterIndex < commandLineStringArray[index].length() - 1; characterIndex++) {
                if (commandLineStringArray[index].charAt(characterIndex) == ':') {
                    if (!hasOneColon)
                        hasOneColon = true;
                    else
                        return false;
                }
            }

            // It is invalid if it doesn't contain a colon
            if (!hasOneColon)
                return false;

            String[] keyValueString = commandLineStringArray[index].split(":");

            headerKeyValuePairHashMap.put(keyValueString[0], keyValueString[1]);
        }

        return true;
    }

    private boolean verifyURL(String urlString) {
        // Remove empty bytes from the string
        urlString = urlString.replaceAll("\u0000.*", "");

        // Check it is an empty url
        if (!compareStringsWithChar("", urlString)) {
            // Check the url format, it should be wrapped by a pair of apostrophes
            if (urlString.charAt(0) == 39 & urlString.charAt(urlString.length() - 1) == 39) {
                // Remove the apostrophes around the url
                currentURL = urlString.replaceAll("'", "");

                return true;
            }
        }

        return false;
    }

    private String getHttpResponse(String urlString) {
        StringBuilder stringBuilder;

        try {
            stringBuilder = new StringBuilder();
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line + "\n");

            bufferedReader.close();
        } catch (Exception e) {
            return "Get Http response error";
        }

        return stringBuilder.toString();
    }

    private String getHeaderValueByKey(String urlString, String keyString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            String headerValueString = connection.getHeaderField(keyString);

            if (headerValueString == null)
                System.out.println("Key '" + keyString + "' not found");
            else
                return headerValueString;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Not found";
    }

    private String readJSONFile(String jsonFileName) {
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = null;

        try {
            Object obj = parser.parse(new FileReader(jsonFileName));
            jsonObject = (JSONObject) obj;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }

        return jsonObject.toString();
    }

    private boolean writeToTextFile(String fileNameString, String contentString) {
        try {
            FileWriter myWriter = new FileWriter(fileNameString);
            myWriter.write(contentString);
            myWriter.close();
            System.out.println("Successfully wrote response to the file.");
        } catch (IOException e) {
            System.out.println("Failed to write the file.");
            e.printStackTrace();

            return false;
        }

        return true;
    }

    private boolean compareStringsWithChar(String string1, String string2) {
        // Remove empty bytes from the string
        string1 = string1.replaceAll("\u0000.*", "");
        string2 = string2.replaceAll("\u0000.*", "");

        if (string1.length() != string2.length())
            return false;

        for (int index = 0; index < string1.length(); index++)
            if (Character.compare(string1.charAt(index), string2.charAt(index)) != 0)
                return false;

        return true;
    }

    private void listenAndServe(int port) throws IOException {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", server.getLocalAddress());
            for (; ; ) {
                SocketChannel client = server.accept();
                logger.info("New client from {}", client.getRemoteAddress());
                // We may use a custom Executor instead of ForkJoinPool in a real-world
                // application
                ForkJoinPool.commonPool().submit(() -> readEchoAndRepeat(client));
            }
        }
    }

    private String postHttpResponse(String urlString, HashMap<String, String> headerKeyValuePairHashMap, String jsonData) {
        StringBuilder stringBuilder;

        try {
            stringBuilder = new StringBuilder();
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            // Set all the header parameters
            for (String keyString : headerKeyValuePairHashMap.keySet())
                connection.setRequestProperty(keyString, headerKeyValuePairHashMap.get(keyString));

            // The Content-Type is fixed
            connection.setRequestProperty("Content-Type", "application/json");

            if (jsonData != null) {
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = jsonData.getBytes("utf-8");
                    wr.write(input, 0, input.length);
                }
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }

            bufferedReader.close();
        } catch (Exception e) {
            System.out.println(e);
            return "Post Http response error";
        }

        return stringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port").withOptionalArg().defaultsTo("8007");

        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        httpcServer server = new httpcServer();
        server.listenAndServe(port);
    }
}