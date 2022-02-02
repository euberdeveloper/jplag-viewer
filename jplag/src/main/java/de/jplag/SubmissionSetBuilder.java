package de.jplag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import de.jplag.exceptions.BasecodeException;
import de.jplag.exceptions.ExitException;
import de.jplag.exceptions.RootDirectoryException;
import de.jplag.exceptions.SubmissionException;
import de.jplag.options.JPlagOptions;

/**
 * Builder class for the creation of a {@link SubmissionSet}.
 * @author Timur Saglam
 */
public class SubmissionSetBuilder {

    private final Language language;
    private final JPlagOptions options;
    private final ErrorCollector errorCollector;
    private final Set<String> excludedFileNames; // Set of file names to be excluded in comparison.

    /**
     * Creates a builder for submission sets.
     * @param language is the language of the submissions.
     * @param options are the configured options.
     * @param errorCollector is the interface for error reporting.
     * @param excludedFileNames
     */
    public SubmissionSetBuilder(Language language, JPlagOptions options, ErrorCollector errorCollector, Set<String> excludedFileNames) {
        this.language = language;
        this.options = options;
        this.errorCollector = errorCollector;
        this.excludedFileNames = excludedFileNames;
    }

    /**
     * Builds a submission set for all submissions of a specific directory.
     * @return the newly built submission set.
     * @throws ExitException if the directory cannot be read.
     */
    public SubmissionSet buildSubmissionSet() throws ExitException {
        verifyRootDirectories();

        List<String> rootDirectoryNames = options.getRootDirectoryNames();

        // Find unique short names in case of multiple root directories, since submission names
        // may not be unique between different roots.
        Map<File, List<String>> shortRootDirectoryNames;
        if (rootDirectoryNames.size() == 1) {
            // For backward compatibility, use an empty short name to skip prepending it to submission names.
            File rootDirectory = new File(rootDirectoryNames.get(0));
            shortRootDirectoryNames = Map.of(rootDirectory, List.of(""));
        } else {
            NameProvider<File> shortRootDirectoryNameProvider = new NameProvider<>();
            for (String rootDirectoryName : rootDirectoryNames) {
                File rootDirectory = new File(rootDirectoryName);
                shortRootDirectoryNameProvider.storeElement(rootDirectory, rootDirectory.toString(), File.separator);
            }
            shortRootDirectoryNames = shortRootDirectoryNameProvider.collectNamedElements();
        }
        // Since verifyRootDirectories() checked uniqueness, we should have a short name for all roots.
        if (shortRootDirectoryNames.size() != rootDirectoryNames.size()) {
            throw new AssertionError("Failed to find unique names for all root directories.");
        }

        // Collect valid looking entries from the root directories.
        Map<File, Submission> foundSubmissions = new HashMap<>();
        for (Entry<File, List<String>> shortRootDirectoryNameEntry : shortRootDirectoryNames.entrySet()) {
            File rootDirectory = shortRootDirectoryNameEntry.getKey();
            String shortRootDirectoryName = join(shortRootDirectoryNameEntry.getValue());
            processRootDirEntries(rootDirectory, shortRootDirectoryName, foundSubmissions);
        }

        // Extract the basecode submission if necessary.
        Optional<Submission> baseCodeSubmission = Optional.empty();
        if (options.hasBaseCode()) {
            Submission baseCode = tryLoadBaseCodeAsPath();
            if (baseCode == null) {
                if (rootDirectoryNames.size() > 1) {
                    throw new BasecodeException("Basecode name cannot be specified in a submission set with more than one root directory.");
                }

                // Single root-directory, try the deprecated basecode specification.
                baseCode = tryLoadBaseCodeAsRootSubDirectory(foundSubmissions);

                String baseCodeName = options.getBaseCodeSubmissionName().get();
                if (baseCode == null) {
                    // No base code found at all, report an error.
                    throw new BasecodeException(
                            String.format("Basecode path \"%s\" relative to the working directory could not be found.", baseCodeName));
                } else {
                    // Found a base code as a submission, report about obsolete usage.
                    System.out.printf("Deprecated use of the -bc option found, please specify the basecode as \"%s%s%s\" instead.\n",
                            rootDirectoryNames.get(0), File.separator, baseCodeName);
                }
            }
            baseCodeSubmission = Optional.of(baseCode);
            System.out.println(String.format("Basecode directory \"%s\" will be used.", baseCode.getRoot().toString()));

            // Basecode may also be registered as a user submission. If so, remove the latter.
            Submission removed = foundSubmissions.remove(baseCode.getCanonicalRoot());
            if (removed != null) {
                System.out.println(String.format("Skipping \"%s\" as user submission.", removed.getRoot().toString()));
            }
        }

        // Merge everything in a submission set.
        List<Submission> submissions = new ArrayList<>(foundSubmissions.values());
        return new SubmissionSet(submissions, baseCodeSubmission, errorCollector, options);
    }

    /**
     * Verify that the given root directories exist and have no duplicate entries.
     */
    private void verifyRootDirectories() throws ExitException {
        List<String> rootDirectoryNames = options.getRootDirectoryNames();
        Set<File> canonicalRootDirs = new HashSet<>(rootDirectoryNames.size());
        for (String rootDirectoryName : rootDirectoryNames) {
            File rootDir = new File(rootDirectoryName);

            if (!rootDir.exists()) {
                throw new RootDirectoryException(String.format("Root directory \"%s\" does not exist!", rootDirectoryName));
            }
            if (!rootDir.isDirectory()) {
                throw new RootDirectoryException(String.format("Root directory \"%s\" is not a directory!", rootDirectoryName));
            }

            boolean added;
            try {
                added = canonicalRootDirs.add(rootDir.getCanonicalFile());
            } catch (IOException e) {
                throw new RootDirectoryException(String.format("Cannot read root directory \"%s\".", rootDirectoryName));
            }
            if (!added) {
                throw new RootDirectoryException(
                        String.format("Root directory \"%s\" has already been added as submission root!", rootDirectoryName));
            }
        }
    }

    /**
     * Try to load the basecode under the assumption of being a path.
     * @return Base code submission if the option value can be interpreted as global path, else {@code null}.
     * @throws ExitException when the option value is a path with errors.
     */
    private Submission tryLoadBaseCodeAsPath() throws ExitException {
        String baseCodeName = options.getBaseCodeSubmissionName().get();
        File basecodeSubmission = new File(baseCodeName);
        if (!basecodeSubmission.exists()) {
            return null;
        }

        String errorMessage = isExcludedEntry(basecodeSubmission);
        if (errorMessage != null) {
            throw new BasecodeException(errorMessage); // Stating an excluded path as basecode isn't very useful.
        }

        try {
            // Use an unlikely short name for the base code. If all is well, this name should not appear
            // in the output since basecode matches are removed from it.
            return processDirEntry(basecodeSubmission, "**basecode**");

        } catch (SubmissionException exception) {
            throw new BasecodeException(exception.getMessage(), exception); // Change thrown exception to basecode exception.

        } catch (ExitException ex) {
            throw ex;
        }
    }

    /**
     * Try to load the basecode under the assumption of being a sub-directory in the user submissions directory.
     * @return Base code submission if the option value can be interpreted as a sub-directory, else {@code null}.
     * @throws ExitException when the option value is a sub-directory with errors.
     */
    private Submission tryLoadBaseCodeAsRootSubDirectory(Map<File, Submission> foundSubmissions) throws ExitException {
        String baseCodeName = options.getBaseCodeSubmissionName().get();

        // Is the option value a single name after trimming spurious separators?
        String name = baseCodeName;
        while (name.startsWith(File.separator)) {
            name = name.substring(1);
        }
        while (name.endsWith(File.separator)) {
            name = name.substring(0, name.length() - 1);
        }

        // If it is not a name of a single sub-directory, bail out.
        if (name.isEmpty() || name.contains(File.separator))
            return null;

        if (name.contains(".")) {
            throw new BasecodeException("The basecode directory name \"" + name + "\" cannot contain dots!");
        }

        // Grab the basecode submission from the regular submissions.
        File rootDirectory = new File(options.getRootDirectoryNames().get(0));
        File basecodePath = new File(rootDirectory, baseCodeName);
        try {
            basecodePath = basecodePath.getCanonicalFile();
        } catch (IOException e) {
            throw new BasecodeException(String.format("Cannot compute canonical file path of \"%s\".", basecodePath.toString()));
        }
        return foundSubmissions.get(basecodePath);
    }

    /**
     * Read entries in the given root directory.
     */
    private String[] readSubmissionRootNames(File rootDirectory) throws ExitException {
        if (!rootDirectory.isDirectory()) {
            throw new AssertionError("Given root is not a directory.");
        }

        String[] fileNames;

        try {
            fileNames = rootDirectory.list();
        } catch (SecurityException exception) {
            throw new RootDirectoryException("Cannot list files of the root directory! " + exception.getMessage(), exception);
        }

        if (fileNames == null) {
            throw new RootDirectoryException("Cannot list files of the root directory!");
        }

        Arrays.sort(fileNames);
        return fileNames;
    }

    /**
     * Check that the given submission entry is not invalid due to exclusion names or bad suffix.
     * @param submissionEntry Entry to check.
     * @return Error message if the entry should be ignored.
     */
    private String isExcludedEntry(File submissionEntry) {
        if (isFileExcluded(submissionEntry)) {
            return "Exclude submission: " + submissionEntry.getName();
        }

        if (submissionEntry.isFile() && !hasValidSuffix(submissionEntry)) {
            return "Ignore submission with invalid suffix: " + submissionEntry.getName();
        }
        return null;
    }

    /**
     * Process the given directory entry as a submission root, the path MUST not be excluded.
     * @param submissionEntry Entry to process.
     * @param shortRootDirectoryName Short unique name of the directory containing the entry, may be empty.
     * @return The entry converted to a submission.
     * @throws ExitException when an error has been found with the entry.
     */
    private Submission processDirEntry(File submissionEntry, String shortRootDirectoryName) throws ExitException {
        if (isExcludedEntry(submissionEntry) != null) {
            throw new AssertionError("Pre-condition of non-exclusion is violated.");
        }

        String fileName = submissionEntry.getName();
        if (submissionEntry.isDirectory() && options.getSubdirectoryName() != null) {
            // Use subdirectory instead
            submissionEntry = new File(submissionEntry, options.getSubdirectoryName());

            if (!submissionEntry.exists()) {
                throw new SubmissionException(
                        String.format("Submission %s does not contain the given subdirectory '%s'", fileName, options.getSubdirectoryName()));
            }

            if (!submissionEntry.isDirectory()) {
                throw new SubmissionException(String.format("The given subdirectory '%s' is not a directory!", options.getSubdirectoryName()));
            }
        }

        String submissionName = shortRootDirectoryName.isEmpty() ? fileName : (shortRootDirectoryName + "::" + fileName);
        return new Submission(submissionName, submissionEntry, parseFilesRecursively(submissionEntry), language, errorCollector);
    }

    /**
     * Process entries in the root directory to check whether they qualify as submissions.
     * @param rootDirectory Root directory being examined.
     * @param shortRootDirectoryName Short unique name of the root directory, may be empty.
     * @param submissions Submissions found so far, is updated in-place.
     */
    private void processRootDirEntries(File rootDirectory, String shortRootDirectoryName, Map<File, Submission> submissions) throws ExitException {
        String[] fileNames = readSubmissionRootNames(rootDirectory);
        for (String fileName : fileNames) {
            File submissionFile = new File(rootDirectory, fileName);

            String errorMessage = isExcludedEntry(submissionFile);
            if (errorMessage != null) {
                System.out.println(errorMessage);
                continue;
            }

            Submission submission = processDirEntry(submissionFile, shortRootDirectoryName);
            submissions.put(submission.getCanonicalRoot(), submission);
        }
    }

    /**
     * Checks if a file has a valid suffix for the current language.
     * @param file is the file to check.
     * @return true if the file suffix matches the language.
     */
    private boolean hasValidSuffix(File file) {
        String[] validSuffixes = options.getFileSuffixes();

        // This is the case if either the language frontends or the CLI did not set the valid suffixes array in options
        if (validSuffixes == null || validSuffixes.length == 0) {
            return true;
        }
        return Arrays.stream(validSuffixes).anyMatch(suffix -> file.getName().endsWith(suffix));
    }

    /**
     * Checks if a file is excluded or not.
     */
    private boolean isFileExcluded(File file) {
        return excludedFileNames.stream().anyMatch(excludedName -> file.getName().endsWith(excludedName));
    }

    /**
     * Recursively scan the given directory for nested files. Excluded files and files with an invalid suffix are ignored.
     * <p>
     * If the given file is not a directory, the input will be returned as a singleton list.
     * @param file - File to start the scan from.
     * @return a list of nested files.
     */
    private Collection<File> parseFilesRecursively(File file) {
        if (isFileExcluded(file)) {
            return Collections.emptyList();
        }

        if (file.isFile() && hasValidSuffix(file)) {
            return Collections.singletonList(file);
        }

        String[] nestedFileNames = file.list();

        if (nestedFileNames == null) {
            return Collections.emptyList();
        }

        Collection<File> files = new ArrayList<>();

        for (String fileName : nestedFileNames) {
            files.addAll(parseFilesRecursively(new File(file, fileName)));
        }

        return files;
    }

    /**
     * Construct a string by concatenating the parts separated by {@link File#separator}.
     * @param nameParts Parts to join.
     * @return The constructed string.
     */
    private static String join(List<String> nameParts) {
        if (nameParts.size() == 1) {
            return nameParts.get(0);
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < nameParts.size(); i++) {
            if (i > 0) {
                stringBuilder.append(File.separator);
            }
            stringBuilder.append(nameParts.get(i));
        }
        return stringBuilder.toString();
    }
}
