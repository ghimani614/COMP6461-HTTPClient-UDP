package ca.concordia.httpc;

import sun.awt.AWTAccessor;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystem {
    private static String rootDirectoryPath = new File("").getAbsolutePath();

    public static void main(String[] args) {
        System.out.println(Paths.get(rootDirectoryPath));

        System.out.println(Files.exists(Paths.get(rootDirectoryPath + "/Tes.txt")));

        listAllFiles(rootDirectoryPath);

        createFile(rootDirectoryPath, "BBB", ".txt");
        writeFile(rootDirectoryPath, "678", ".txt", "**** ????????", false);

        copyFile(rootDirectoryPath, rootDirectoryPath + "/download", "678", ".txt");
    }

    public static void listAllFiles(String path){
        String[] fileNameArray;

        // Creates a new File instance by converting the given pathname string
        // into an abstract pathname
        File f = new File(path);

        // Populates the array with names of files and directories
        fileNameArray = f.list();

        // For each pathname in the pathnames array
        // Print the names of files and directories
        for (String fileNname : fileNameArray)
            System.out.println(fileNname);
    }

    public static void createFile(String path, String fileName, String format) {
        Path filePath = Paths.get(path + "/" + fileName + format);

        if (Files.notExists(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Successfully created file");
        } else {
            System.out.println("File already exists");
        }
    }

    public static void writeFile(String path, String fileName, String format, String content, boolean overwrite) {
        Path filePath = Paths.get(path + "/" + fileName + format);

        // If it doesn't overwrite, keep both files
        if (Files.exists(filePath) & !overwrite) {
            fileName += " copy";
            filePath = Paths.get(path + "/" + fileName + format);
        }

        try {
            Files.write(filePath, content.getBytes());

            System.out.println("Successfully wrote file");
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("Failed to write file");
        }
    }

    public static void copyFile(String previousPath, String newPath, String fileName, String format) {
        Path originalFilePath = Paths.get(previousPath + "/" + fileName + format);
        Path targetFilePath = Paths.get(newPath + "/" + fileName + format);

        if (Files.exists(originalFilePath) & Files.notExists(targetFilePath)) {
            try {
                Files.copy(originalFilePath, targetFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Successfully copied file");
        } else {
            System.out.println("Failed to copy file: Target file already exists");
        }
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
    public void parseCommandLine(String commandLineString){
        commandLineString = preprocessCommandLine(commandLineString);

        String[] commandLineStringArray = commandLineString.split(" ");
        if (commandLineStringArray.length > 0) {
         //check the starting word, must start with httpfs
            if (commandLineStringArray[0].compareTo("httpfs") != 0) {
                System.out.print("Invalid syntax");
            } else {
                //check if httpfs -v
             if(compareStringsWithChar("-v", commandLineStringArray[1])){
                 //debugging message displayed
                }
                else if(compareStringsWithChar("-p", commandLineStringArray[1])){
                 //port change

                }else if(compareStringsWithChar("-d", commandLineStringArray[1])){
                 //directory mentioned

                }else{
                 System.out.println("Invalid format");
                }
            }
        }else{
            System.out.println("No command");
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
}