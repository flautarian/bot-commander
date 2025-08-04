package com.giacconidev.botcommander.backend.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StreamUtils;

public class Utils {

    /**
     * Generates a random name by combining a random adjective and a random noun.
     * 
     * @return a randomly generated name
     */
    public static String generateRandomName() {
        String[] adjectives = { "Quick", "Lazy", "Happy", "Sad", "Brave", "Clever", "Silly", "Wise" };
        String[] nouns = { "Fox", "Dog", "Cat", "Bear", "Lion", "Tiger", "Elephant", "Monkey" };

        int randomAdjectiveIndex = (int) (Math.random() * adjectives.length);
        int randomNounIndex = (int) (Math.random() * nouns.length);

        return adjectives[randomAdjectiveIndex] + nouns[randomNounIndex];
    }

    /**
     * Copies the contents of a folder to a ZIP output stream, excluding banned
     * files.
     * 
     * @param folderPath  the path to the folder to copy
     * @param bannedFiles a list of file names to exclude from the ZIP
     * @param fos         the ZipOutputStream to write the ZIP file
     * @throws IOException if an I/O error occurs
     */
    public static void copyFolderToZip(String folderPath, ArrayList<String> bannedFiles, ZipOutputStream fos)
            throws IOException {
        // Create a resolver to find resources matching a pattern
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // Use the resolver to get resources from the specified folder
        Resource[] resources = resolver.getResources("classpath:" + folderPath + "/*");

        // Create a ZipOutputStream to write the ZIP file
        for (Resource resource : resources) {
            // Skip banned files
            if (bannedFiles.contains(resource.getFilename())) {
                continue;
            }
            // Create a new ZipEntry for each file
            String fileName = resource.getFilename();
            ZipEntry zipEntry = new ZipEntry(fileName);
            fos.putNextEntry(zipEntry);

            // Copy the content of the resource to the ZIP file
            InputStream is = resource.getInputStream();
            StreamUtils.copy(is, fos);
            is.close();

            fos.closeEntry();
        }
    }
}
