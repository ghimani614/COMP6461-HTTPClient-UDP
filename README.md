# Lab Assignment 2

This Java project implemented all the functionalities of COMP 6461 Lab Assignment 2, including all the optional tasks(bonus marks). It was developed based on the multiplex echo server template.

## Requirement
1. [Oracle JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
2. [Apache Maven](https://maven.apache.org/) 
3. [Intellij Community 2020.2](https://www.jetbrains.com/idea/download/) 

Third-party libraries:
1. JSON.simple 1.1: Used for reading and parsing JSON files.


## Configuration
Use Intellij IDE to open the project, navigate to netsample/src/main/java/ca/concordia/httpc, there are MultiplexServer class and httpc class. Before running the program, make sure json-simple-1.1.jar is added to dependency. It is already added by default, if it is not, right click netsample/lib folder and select "Add as Library" option in Intellij.


## Execution
1. Run MultiplexServer.java
2. Run ServerCommand.java, and enter one of the server commands from the example below to run the server
3. Run httpc.java, enter the port number that matches the server
4. Try any client commands from the example below to see the result

To reproduce Optional Task(Bonus Marks) of Multi-Requests Support, follow step 1 and 2 above, after that selectively run ReaderClient1, ReaderClient2, WriterClient1 or WriterClient2 depending on the situation you want to reproduce.

1. Two clients are writing to the same file: Run WriterClient1 and WriterClient2
2. One client is reading, while another is writing to the same file: Run ReaderClient1 or ReaderClient2 and WriterClient1 or WriterClient2<br />
3. Two clients are reading the same file: Run ReaderClient1 and ReaderClient2

## Implementation
Rules:
1. All reserved command keywords are case-sensitive, but JSON data and URLs are not.
2. It is not allowed to have multiple space characters between each term, like "httpfs  &nbsp; get    &nbsp;". Starting with space is invalid, it must starts with "httpfs" without any exception.
3. The port number in URL should match the one that is currently used, otherwise it will cause errors. If port 9000 is being used, the URL should start with http://localhost:9000/
4. Only files with postfix are considered as valid, like Abc.txt or 123.html. Counterexamples: Abc or 123.
5. Currently the entire file path, file name and content to be written in POST command are not allowed to have space characters.

Syntax parser

We used string manipulation to parse the command line. The entire string will be broken into several chunks, and syntactic correctness will be verified by comparing the content of each chunk. The method parseHttpfsClientCommandLine(String commandLineString) and parseServerCommandLine(String commandLineString) in MultiplexServer class implemented this feature.


File system

We used Java NIO2 APIs to implement the file reading/writing/copying.

Multi-Requests Support

It is a classic readers-writers problem. Multiple readers can read simultaneously, but only one writer is allowed to write at a time. In this assignment, we used I/O multiplexing model to make it possible to perform I/O operations on multiple descriptors in one thread. To guarantee the data consistency, we used synchronized keyword to make sure a file is edited by at most one process at the same time. 

## Examples
All the available command lines are listed here. You should replace workingDirectoryAbsolutePath with the actual path on your computer.

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
