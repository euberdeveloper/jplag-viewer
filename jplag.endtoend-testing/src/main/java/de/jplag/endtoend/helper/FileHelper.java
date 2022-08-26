package de.jplag.endtoend.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.jplag.endtoend.constants.TestDirectoryConstants;

/**
 * Helper class to perform all necessary operations or functions on files or folders.
 */
public class FileHelper {

    private FileHelper() {
        // private constructor to prevent instantiation
    }

    /**
     * Merges all contained filenames together without extension
     * @param files whose names are to be merged
     * @return merged filenames
     */
    public static String getEnclosedFileNamesFromCollection(Collection<File> files) {

        return files.stream().map(File::getName).map(fileName -> fileName.substring(0, fileName.lastIndexOf('.'))).collect(Collectors.joining());
    }

    /**
     * Load all possible languages in resource path
     * @param directoryNames folder names for which the language options should be listed.
     * @return list of all LanguageOptions included in the resource path
     */
    public static List<String> getLanguageOptionsFromPath(String[] directoryNames) {
        return Arrays.stream(directoryNames).map(language -> language).filter(Objects::nonNull).toList();
    }

    /**
     * @param directorieRoot path from which all folders should be loaded
     * @return all folders found in the specified path
     */
    public static String[] getAllDirectoriesInPath(Path directorieRoot) {
        return directorieRoot.toFile().list((dir, name) -> new File(dir, name).isDirectory());
    }

    /**
     * Copies the passed filenames to a temporary path to use them in the tests
     * @param classNames for which the test case is to be created
     * @return paths created to the test submissions
     * @throws IOException Exception can be thrown in cases that involve reading, copying or locating files.
     */
    public static String[] createNewTestCaseDirectory(String[] classNames) throws IOException {
        // Copy the resources data to the temporary path
        String[] returnSubmissionPath = new String[classNames.length];
        for (int counter = 0; counter < classNames.length; counter++) {
            Path originalPath = Path.of(classNames[counter]);
            returnSubmissionPath[counter] = Path
                    .of(TestDirectoryConstants.TEMPORARY_SUBMISSION_DIRECTORY_NAME.toString(), "submission" + (counter + 1)).toAbsolutePath()
                    .toString();
            Path copyPath = Path.of(TestDirectoryConstants.TEMPORARY_SUBMISSION_DIRECTORY_NAME.toString(), "submission" + (counter + 1),
                    originalPath.getFileName().toString());

            File directory = new File(copyPath.toString());
            if (!directory.exists()) {
                directory.mkdirs();
            }
            Files.copy(originalPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return returnSubmissionPath;
    }

    /**
     * Delete directory with including files
     * @param folder Path to a folder or file to be deleted. This happens recursively to the path
     * @throws IOException if an I/O error occurs
     */
    public static void deleteCopiedFiles(File folder) throws IOException {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) { // some JVMs return null for empty dirs
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteCopiedFiles(file);
            } else {
                Files.delete(file.toPath());
            }
        }
        Files.delete(folder.toPath());
    }

    /**
     * Creates directory if it dose not exist
     * @param directory to be created
     * @throws IOException if the directory could not be created
     */
    public static void createDirectoryIfItDoesNotExist(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(createNewIOExceptionStringForFileOrFOlderCreation(directory));
        }
    }

    /**
     * Creates file if it dose not exist
     * @param file to be created
     * @throws IOException if the file could not be created
     */
    public static void createFileIfItDoesNotExist(File file) throws IOException {
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException(createNewIOExceptionStringForFileOrFOlderCreation(file));
        }
    }

    /**
     * @param resourcenPaths list of paths that lead to test resources
     * @return all filenames contained in the paths
     */
    public static String[] loadAllTestFileNames(Path resourcenPaths) {
        var files = resourcenPaths.toFile().listFiles();
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        return fileNames;
    }

    /**
     * @param file for which the exception text is to be created
     * @return exception text for the specified file
     */
    private static String createNewIOExceptionStringForFileOrFOlderCreation(File file) {
        return "The file/folder at the location [" + file.toString() + "] could not be created!";
    }
}
