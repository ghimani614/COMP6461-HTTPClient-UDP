# Lab Assignment 3

This Java project implemented all the functionalities of COMP 6461 Lab Assignment 3. It was developed based on the UDP template and multiplex echo server template.

## Requirement
1. [Oracle JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
2. [Apache Maven](https://maven.apache.org/) 
3. [Intellij Community 2020.2](https://www.jetbrains.com/idea/download/) 

Third-party libraries:
1. JSON.simple 1.1: Used for reading and parsing JSON files.


## Configuration
Use Intellij IDE to open the project, navigate to netsample/src/main/java/ca/concordia/httpc, there are MultiplexServer class and httpc class. Before running the program, make sure json-simple-1.1.jar is added to dependency. It is already added by default, if it is not, right click netsample/lib folder and select "Add as Library" option in Intellij.


## Execution
1. Open terminal, use "cd" to navigate inside the router source folder that contains router.go
2. Enter "./router --port=3000 --drop-rate=0.5 --max-delay=10ms --seed=1" to run the router
3. Run UDPServer.java in udp package
4. Run ServerCommand.java in httpc package, and enter one of the server commands from the example below to run the server
5. Run UDPClient.java in udp package, enter the port number that matches the server
6. Try any client commands from the example below to see the result

## Implementation
Rules:
1. All reserved command keywords are case-sensitive, but JSON data and URLs are not.
2. It is not allowed to have multiple space characters between each term, like "httpfs  &nbsp; get    &nbsp;". Starting with space is invalid, it must starts with "httpfs" without any exception.
3. The port number in URL should match the one that is currently used, otherwise it will cause errors. If port 9000 is being used, the URL should start with http://localhost:9000/
4. Only files with postfix are considered as valid, like Abc.txt or 123.html. Counterexamples: Abc or 123.
5. Currently the entire file path, file name and content to be written in POST command are not allowed to have space characters.

Syntax parser

We used string manipulation to parse the command line. The entire string will be broken into several chunks, and syntactic correctness will be verified by comparing the content of each chunk. The method parseHttpfsClientCommandLine(String commandLineString) and parseServerCommandLine(String commandLineString) in UDPServer class implemented this feature.


File system

We used Java NIO2 APIs to implement the file reading/writing/copying.


Packet Type

We use numbers to represent different packet types, but in this project, we didn't use NAK. The NAK slot is kept for maintaining the format.

0: SYN<br />
1: SYN-ACK<br />
2: ACK<br />
3: NAK<br />
4: DATA<br />


Selective Repeat ARQ

We simply used two indexes to represent a sliding window. The length of sequence number can be two times of window size. And we use total number of packets to indicate the minimum packets that are required for an entire command line. The window size variable in Attributes.java is adjustable, as long as it is the power of 2.

Data consistency

We simulated a scenario with 0.95 drop-rate and 90ms of delay, which was close to the worst case. Despite the frequent occurrence of dropping packets(only 6 packets were sent successfully within 40 minutes), the correctness of the result still could be guaranteed.


## Examples
All the available command lines are listed here. For the assignment 2 example, you should replace workingDirectoryAbsolutePath with the actual path on your computer.

### Assignment 1

1. httpc post url<br />
   httpc post 'http://httpbin.org/post'
2. httpc post -h key:value url<br />
   httpc post -h key:value 'http://httpbin.org/post'
3. httpc post -h key:value -d "inline data" url<br />
   httpc post -h key:value -d '{"Assignment": 1}' 'http://httpbin.org/post'
4. httpc post -h key:value -f "file name" url<br />
   httpc post -h key:value -f Data.json 'http://httpbin.org/post'
5. httpc post -v url<br />
   httpc post -v 'http://httpbin.org/post'
6. httpc post -v -h key:value url<br />
   Example: httpc post -v -h key:value 'http://httpbin.org/post'
7. httpc post -v -h key:value -d "inline data" url<br />
   httpc post -v -h key:value -d '{"Assignment": 1}' 'http://httpbin.org/post'
8. httpc post -v -h key:value -f "file name" url<br />
   httpc post -v -h key:value -f Data.json 'http://httpbin.org/post'

### Assignment 2

Client
1. GET /<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath
2. GET /foo<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo.txt
3. POST /bar -d inline-data overwrite=true/false<br />
httpfs post http://localhost:8080/workingDirectoryAbsolutePath/bar.txt -d 'network' overwrite=true/false
4. GET /foo Content-Type:type/subtype<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo Content-Type:text/plain 
5. GET /foo Content-Disposition:inline/attachment<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo.txt Content-Disposition:inline<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo.txt Content-Disposition:attachment
6. GET /foo Content-Type:type/subtype Content-Disposition<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo Content-Type:text/plain Content-Disposition:inline<br />
httpfs get http://localhost:8080/workingDirectoryAbsolutePath/foo Content-Type:text/plain Content-Disposition:attachment

Server
1. httpfs [-v]<br />
httpfs -v
2. httpfs [-v] [-p PORT]<br />
httpfs -v -p 8080
3. httpfs [-v] [-d PATH-TO-DIR]<br />
httpfs -v -d workingDirectoryAbsolutePath
4. httpfs [-v] [-p PORT] [-d PATH-TO-DIR]<br />
httpfs -v -p 8080 -d workingDirectoryAbsolutePath
5. httpfs [-p PORT]<br />
httpfs -p 8080
6. httpfs [-d PATH-TO-DIR]<br />
httpfs -d workingDirectoryAbsolutePath
7. httpfs [-p PORT] [-d PATH-TO-DIR]<br />
httpfs -p 8080 -d workingDirectoryAbsolutePath


## Status Codes

0: No errors<br />
1: Server is running<br />
2: Server is running already<br />
3: Incorrect request URL<br />
4: Not allowed to change port number after server establishment<br />
5: Access restricted<br />
6: Failed to read file<br />
7: Failed to write file<br />
8: Failed to copy file<br />
9: It's a regular file, not a directory<br />
10: It's a directory, not a regular file<br />
11: Directory doesn't exist<br />
12: File doesn't exist<br />
13: Failed to copy file: Target file already exists<br />
14: Invalid Content-Type<br />
15: Invalid Content-Disposition<br />
99: Invalid syntax<br />

## Details
1. The string comparision has to be done in a different way, because string converted from the byte array of client/server is in UTF-8 format, but the Java declared string attributes are in UTF-16 format, which means string.equals() or string.compareTo() don't work in this case. Instead, we compare each character of strings by using string.charAt(). The method compareStringsWithChar(String string1, String string2) in MultiplexServer class implemented this feature.
2. Extra space characters may occur in the JSON string of the command line, which affect the string splitting of syntax parsing. To solve this problem, we preprocess the command line by removing unnecessary space characters. The method preprocessCommandLine(String commandLineString) in MultiplexServer class implemented this feature.


## References

1. http://zetcode.com/java/getpostrequest/
2. https://www.tutorialspoint.com/json_simple/json_simple_quick_guide.htm
3. https://stackoverflow.com/questions/20806617/retrieve-the-final-location-of-a-given-url-in-java
4. https://www.codota.com/code/java/methods/java.net.URLConnection/getHeaderField
5. https://www.baeldung.com/java-nio-2-file-api
6. https://medium.com/@liakh.aliaksandr/java-sockets-i-o-blocking-non-blocking-and-asynchronous-fb7f066e4ede
7. https://wiki.eecs.yorku.ca/course_archive/2007-08/F/6490A/readers-writers
8. http://tutorials.jenkov.com/java-nio/selectors.html
9. https://stackoverflow.com/questions/10055913/set-timeout-for-socket-receive
10. https://www.tutorialspoint.com/java_nio/java_nio_datagram_channel.htm
11. https://www.baeldung.com/java-nio-selector
12. https://www.waitingforcode.com/java-i-o/handling-multiple-io-one-thread-nio-selector/read