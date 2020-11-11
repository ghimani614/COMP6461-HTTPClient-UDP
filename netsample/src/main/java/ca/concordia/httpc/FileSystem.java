package ca.concordia.httpc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystem {
    public static String rootDirectoryAbsolutePath = new File("").getAbsolutePath();

    public String fileContentString = "";

    public boolean directoryExists(String path) {
        return Files.exists(Paths.get(path));
    }

    public int listAllFiles(String path) {
        if (Files.notExists(Paths.get(path)))
            return 11;

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
            return 10;

        File file = new File(path);

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            fileContentString = "";
            String line = "";

            while ((line = bufferedReader.readLine()) != null)
                fileContentString += line + "\n";
        } catch (FileNotFoundException e) {
            return 12;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int writeFile(String path, String fileName, String format, String content, boolean overwrite) {
        if (!Files.exists(Paths.get(path)))
            return 11;

        // If it doesn't overwrite, keep both files
        if (Files.exists(Paths.get(path + fileName + format)) & !overwrite)
            fileName += "Copy";

        try {
            Files.write(Paths.get(path + fileName + format), content.getBytes());

            return 0;
        } catch (IOException e) {
            e.printStackTrace();

            return 7;
        }
    }

    public int copyFile(String previousPath, String newPath, String fileName, String format) {
        Path originalFilePath = Paths.get(previousPath + "/" + fileName + format);
        Path targetFilePath = Paths.get(newPath + "/" + fileName + format);

        if (Files.notExists(originalFilePath))
            return 12;

        if (Files.exists(targetFilePath))
            return 13;

        try {
            Files.copy(originalFilePath, targetFilePath);
        } catch (IOException e) {
            return 8;
        }

        return 0;
    }
}
