package ca.concordia.httpc;

import sun.awt.AWTAccessor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystem {
    public static String rootDirectoryAbsolutePath = new File("").getAbsolutePath();

    public String fileContentString = "";

//    public static void main(String[] args) {
//        System.out.println(Paths.get(rootDirectoryRelativePath));

//        System.out.println(Files.exists(Paths.get(rootDirectoryPath +"/Tes.txt")));
//
//        listAllFiles(rootDirectoryPath);
//
//        createFile(rootDirectoryPath, "BBB", ".txt");
//        System.out.println(Files.exists(Paths.get("/Users/hongyushen/Documents/IntelliJProject/COMP6461-httpServerApplication/working-directory1/")));
//        System.out.println(writeFile(rootDirectoryPath, "67", ".txt", "**** ????????", false));
//        System.out.println(writeFile("/Users/hongyushen/Documents/IntelliJProject/COMP6461-httpServerApplication/working-directory1/", "bar", ".txt", "**** ????????", false));
//
//        copyFile(rootDirectoryPath, rootDirectoryPath + "/download", "678", ".txt");
//    }

    public boolean directoryExists(String path) {
        return Files.exists(Paths.get(path));
    }

    public int listAllFiles(String path) {
        if (Files.notExists(Paths.get(path)))
            return 9;

        String[] fileNameArray;

        // Populates the array with names of files and directories
        fileNameArray = new File(path).list();

        fileContentString = "";

        // Print the names of files and directories
        for (String fileNname : fileNameArray)
            fileContentString += fileNname + "\n";

        return 0;
    }

    public int readFile(String path) {
        if (!Files.isRegularFile(Paths.get(path)))
            return 8;

        File file = new File(path);

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            fileContentString = "";
            String line = "";

            while ((line = bufferedReader.readLine()) != null)
                fileContentString += line + "\n";
        } catch (FileNotFoundException e) {
            return 10;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int createFile(String path, String fileName, String format) {
        Path filePath = Paths.get(path + "/" + fileName + format);

        if (Files.notExists(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 0;
        } else {
            return 9;
        }
    }

    public int writeFile(String path, String fileName, String format, String content, boolean overwrite) {
        if (!Files.exists(Paths.get(path)))
            return 9;

        // If it doesn't overwrite, keep both files
        if (Files.exists(Paths.get(path + fileName + format)) & !overwrite)
            fileName += "Copy";

        try {
            Files.write(Paths.get(path + fileName + format), content.getBytes());

            return 0;
        } catch (IOException e) {
            e.printStackTrace();

            return 5;
        }
    }

    public int copyFile(String previousPath, String newPath, String fileName, String format) {
        Path originalFilePath = Paths.get(previousPath + "/" + fileName + format);
        Path targetFilePath = Paths.get(newPath + "/" + fileName + format);

        if (Files.notExists(originalFilePath))
            return 9;

        if (Files.exists(targetFilePath))
            return 11;

        try {
            Files.copy(originalFilePath, targetFilePath);
        } catch (IOException e) {
            return 5;
        }

        return 0;
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

    public void parseCommandLine(String commandLineString) {
        String[] commandLineStringArray = commandLineString.split(" ");
        if (commandLineStringArray.length > 0) {
            //check the starting word, must start with httpfs
            if (commandLineStringArray[0].compareTo("httpfs") != 0) {
                System.out.print("Invalid syntax");
            } else {
                //check if httpfs -v
                if (compareStringsWithChar("-v", commandLineStringArray[1])) {
                    //debugging message displayed
                } else if (compareStringsWithChar("-p", commandLineStringArray[1])) {
                    //port change

                } else if (compareStringsWithChar("-d", commandLineStringArray[1])) {
                    //directory mentioned

                } else {
                    System.out.println("Invalid format");
                }
            }
        } else {
            System.out.println("No command");
        }
    }

    /* getting list of files in the directory */
    public void getFilesList(String pathRoute) {
        String[] fileList;
        File f = new File(pathRoute);
        if (f.isDirectory()) {

            if (pathRoute.contains("..")) {
                System.out.println("HTTP Error 403: Access Denied");
            }
        } else {
            System.out.println("Directory doesn't exist");
        }
        //getting list of all files in the directory
        fileList = f.list();

        //printing names of files
        for (String files : fileList) System.out.println(files);
    }
}
