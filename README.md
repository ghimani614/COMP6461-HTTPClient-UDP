# Lab Assignment 2

This Java project implemented all the functionalities of COMP 6461 Lab Assignment 2, including all the optional tasks(bonus marks). It was developed based on the multiplex echo server template.

## Requirement
1. [Oracle JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
2. [Apache Maven](https://maven.apache.org/) 
3. [Intellij Community 2020.2](https://www.jetbrains.com/idea/download/) 

Third-party libraries:
1. JSON.simple 1.1: Used for reading and parsing JSON files.


## Configuration
Use Intellij IDE to open the project, navigate to netsample/src/main/java/ca/concordia/httpc, there are httpcServer class and httpc class. Before running the program, make sure json-simple-1.1.jar is added to dependency. It is already added by default, if it is not, right click netsample/lib folder and select "Add as Library" option in Intellij. The server should be run before the client, so right click httpcServer class and select "Run 'httpcServer.main()'" in the project view, then use the same way to run httpc.


## Implementation
Rules:
1. All reserved command keywords are case sensitive, but JSON data and URLs are not.
2. It is not allowed to have multiple space characters between each term, like "httpc  help   get ". Starting with space is invalid, it must starts with "httpc" without any exception.
3. URL has to be wrapped by a pair of apostrophes, like 'http://httpbin.org/post'.
4. This program has assumed Content-Type to be application/json in any POST operation, changing Content-Type using POST command has no effect.


Syntax parser

We used string manipulation to parse the command line. The entire string will be broken into several chunks, and syntactic correctness will be verified by comparing the content of each chunk. The method parseCommandLine(String commandLineString) in httpcServer class implemented this feature.


GET and POST

We used Java 11 HttpURLConnection to implement the functionalities of GET and POST. The method getHttpResponse(String urlString) and postHttpResponse(String urlString, HashMap<String, String> headerKeyValuePairHashMap, String jsonData) in httpcServer class implemented corresponding features.


Verbose option

We used Java URLConnection to implement it. It is able to get header values by calling connection.getHeaderField(keyString). The method getHeaderValueByKey(String urlString, String keyString) in httpcServer class implemented this feature.


Redirection

Normally, response code starts with 3 indicates redirection, for example: 301(Moved Permanently), 302(Temporary Redirect). Before executing the command, the target URL will be detected if it redirects or not, if yes, obtain the directed URL by calling connection.getHeaderField("Location"). The attribute maximumRedirectionTimes defines the limit of redirection times in order to prevent infinite loop, default value is 5. The method detectRedirection(String urlString) in httpcServer class implemented this feature.


## Examples
All the available command lines are listed here.

Helper
1. httpc help
2. httpc help get
3. httpc help post

GET
1. httpc get url<br />
httpc get 'http://httpbin.org/get?course=networking&assignment=1'
2. httpc get -v url<br />
httpc get -v 'http://httpbin.org/get?course=networking&assignment=1'
3. httpc get -h key:value url<br />
httpc get -h key1:value1 key2:value2 'http://httpbin.org/get?course=networking&assignment=1'

POST
1. httpc post url<br />
httpc post 'http://httpbin.org/post'
2. httpc post -h key:value url<br />
httpc post -h key1:value1 key2:value2 'http://httpbin.org/post'
3. httpc post -h key:value -d "inline data" url<br />
httpc post -h key1:value1 key2:value2 -d '{"Assignment": 1}' 'http://httpbin.org/post'
4. httpc post -h key:value -f "file name" url<br />
httpc post -h key1:value1 key2:value2 -f Data.json 'http://httpbin.org/post'
5. httpc post -v url<br />
httpc post -v 'http://httpbin.org/post'
6. httpc post -v -h key:value url<br />
httpc post -v -h key1:value1 key2:value2 'http://httpbin.org/post'
7. httpc post -v -h key:value -d "inline data" url<br />
httpc post -v -h key1:value1 key2:value2 -d '{"Assignment": 1}' 'http://httpbin.org/post'
8. httpc post -v -h key:value -f "file name" url<br />
httpc post -v -h key1:value1 key2:value2 -f Data.json 'http://httpbin.org/post'


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
1. The string comparision has to be done in a different way, because string converted from the byte array of client/server is in UTF-8 format, but the Java declared string attributes are in UTF-16 format, which means string.equals() or string.compareTo() don't work in this case. Instead, we compare each character of strings by using string.charAt(). The method compareStringsWithChar(String string1, String string2) in httpcServer class implemented this feature.
2. Extra space characters may occur in the JSON string of the command line, which affect the string splitting of syntax parsing. To solve this problem, we preprocess the command line by removing unnecessary space characters. The method preprocessCommandLine(String commandLineString) in httpcServer class implemented this feature.
3. All the key value pairs are stored in a HashMap<String, String> structure when using -h option. In the postHttpResponse method it will loop through the HashMap to set all properties.
4. We assume all the files to be read/written are located in the root folder.
5. On macOS, if user gives the txt file an empty name using command: "httpc -v 'http://httpbin.org/get?course=networking&assignment=1' -o .txt", the output txt file may become a hidden file. It is necessary to press shift + command + . to show and access hidden files. 


## References

1. http://zetcode.com/java/getpostrequest/
2. https://www.tutorialspoint.com/json_simple/json_simple_quick_guide.htm
3. https://stackoverflow.com/questions/20806617/retrieve-the-final-location-of-a-given-url-in-java
4. https://www.codota.com/code/java/methods/java.net.URLConnection/getHeaderField
5. https://medium.com/@liakh.aliaksandr/java-sockets-i-o-blocking-non-blocking-and-asynchronous-fb7f066e4ede
6. https://wiki.eecs.yorku.ca/course_archive/2007-08/F/6490A/readers-writers
