package ca.concordia.httpc;

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

        createFile(rootDirectoryPath, "AAAAAA", ".txt");
        writeFile(rootDirectoryPath, "123", ".txt", "$$$$$$$$$ ????????", false);

        copyFile(rootDirectoryPath, rootDirectoryPath + "/download", "123", ".txt");
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
}