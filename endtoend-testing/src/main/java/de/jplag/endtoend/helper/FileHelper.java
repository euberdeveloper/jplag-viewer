package de.jplag.endtoend.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper class to perform all necessary operations or functions on files or folders.
 */
public class FileHelper {
    private static final int ZIP_THRESHOLD_ENTRIES = 100000;
    private static final int ZIP_THRESHOLD_SIZE = 1000000000;
    private static final double ZIP_THRESHOLD_RATIO = 10;
    private static final String ZIP_BOMB_ERROR_MESSAGE = "Refusing to unzip file (%s), because it seems to be a fork bomb";

    private FileHelper() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns the name of the passed file, trimming its file extension.
     * @param file is the file to obtain the name from
     * @return returns the name of the file without file extension
     */
    public static String getFileNameWithoutFileExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(0, index);
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
     * @param file for which the exception text is to be created
     * @return exception text for the specified file
     */
    private static String createNewIOExceptionStringForFileOrFOlderCreation(File file) {
        return "The file/folder at the location [" + file.toString() + "] could not be created!";
    }

    public static void unzip(File zip, File targetDirectory) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            long totalSizeArchive = 0;
            long totalEntriesArchive = 0;

            while (entries.hasMoreElements()) {
                totalEntriesArchive++;
                long totalSizeEntry = 0;

                ZipEntry entry = entries.nextElement();
                File unzippedFile = new File(targetDirectory, entry.getName()).getCanonicalFile();

                if (unzippedFile.getAbsolutePath().startsWith(targetDirectory.getAbsolutePath())) {
                    if (entry.isDirectory()) {
                        unzippedFile.mkdirs();
                    } else {
                        unzippedFile.getParentFile().mkdirs();

                        InputStream inputStream = zipFile.getInputStream(entry);
                        OutputStream outputStream = new FileOutputStream(unzippedFile);
                        long count = inputStream.transferTo(outputStream);
                        totalSizeArchive += count;
                        totalSizeEntry += count;
                        inputStream.close();
                        outputStream.close();

                        double compressionRate = (double) totalSizeEntry / entry.getCompressedSize();
                        if (compressionRate > ZIP_THRESHOLD_RATIO) {
                            throw new IllegalStateException(ZIP_BOMB_ERROR_MESSAGE);
                        }
                    }
                }

                if (totalSizeArchive > ZIP_THRESHOLD_SIZE) {
                    throw new IllegalStateException(ZIP_BOMB_ERROR_MESSAGE);
                }
                if (totalEntriesArchive > ZIP_THRESHOLD_ENTRIES) {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
