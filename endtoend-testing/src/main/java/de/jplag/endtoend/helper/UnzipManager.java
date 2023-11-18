package de.jplag.endtoend.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.SystemUtils;

import de.jplag.endtoend.model.DataSet;

public class UnzipManager {
    private final Map<DataSet, File> unzippedFiles;
    private static UnzipManager instance;
    private final Logger logger = Logger.getLogger("Unzip Manager");

    private synchronized static UnzipManager getInstance() {
        if (instance == null) {
            instance = new UnzipManager();
        }

        return instance;
    }

    public static File unzipOrCache(DataSet dataSet, File zip) throws IOException {
        return getInstance().unzipOrCacheInternal(dataSet, zip);
    }

    private UnzipManager() {
        this.unzippedFiles = new HashMap<>();
    }

    private File unzipOrCacheInternal(DataSet dataSet, File zip) throws IOException {
        if (!unzippedFiles.containsKey(dataSet)) {
            File target;

            if (SystemUtils.IS_OS_UNIX) {
                FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
                target = Files.createTempDirectory(zip.getName(), attr).toFile();
            } else {
                target = Files.createTempDirectory(zip.getName()).toFile();
                if (!(target.setReadable(true, true) && target.setWritable(true, true) && target.setExecutable(true, true))) {
                    logger.warning("Could not set permissions for temp directory (" + target.getAbsolutePath() + ").");
                }
            }

            FileHelper.unzip(zip, target);
            this.unzippedFiles.put(dataSet, target);
        }

        return this.unzippedFiles.get(dataSet);
    }
}
