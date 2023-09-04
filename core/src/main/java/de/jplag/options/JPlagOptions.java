package de.jplag.options;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.JPlag;
import de.jplag.JPlagComparison;
import de.jplag.Language;
import de.jplag.Submission;
import de.jplag.clustering.ClusteringOptions;
import de.jplag.exceptions.BasecodeException;
import de.jplag.merging.MergingOptions;
import de.jplag.strategy.SubmissionTuple;
import de.jplag.util.FileUtils;

/**
 * This record defines the options to configure {@link JPlag}.
 * @param language Language to use when parsing the submissions.
 * @param minimumTokenMatch Tunes the comparison sensitivity by adjusting the minimum token required to be counted as
 * matching section. A smaller {@code <n>} increases the sensitivity but might lead to more false-positives.
 * @param submissionDirectories Directories with new submissions. These must be checked for plagiarism.
 * @param oldSubmissionDirectories Directories with old submissions to check against.
 * @param baseCodeSubmissionDirectory Directory containing the base code.
 * @param subdirectoryName Example: If the subdirectoryName is 'src', only the code inside submissionDir/src of each
 * submission will be used for comparison.
 * @param fileSuffixes List of file suffixes that should be included.
 * @param exclusionFileName Name of the file that contains the names of files to exclude from comparison.
 * @param similarityMetric The similarity metric determines how the minimum similarity threshold required for a
 * comparison (of two submissions) is calculated. This affects which comparisons are stored and thus make it into the
 * result object.
 * @param similarityThreshold Similarity value (must be between 0 and 1). Comparisons (of submissions pairs) with a
 * similarity below this threshold will be ignored. The default value of 0 allows all matches to be stored. This affects
 * which comparisons are stored and thus make it into the result object. See also {@link #similarityMetric()}.
 * @param maximumNumberOfComparisons The maximum number of comparisons that will be shown in the generated report. If
 * set to {@link #SHOW_ALL_COMPARISONS} all comparisons will be shown.
 * @param clusteringOptions Clustering options
 * @param debugParser If true, submissions that cannot be parsed will be stored in a separate directory.
 * @param mergingOptions Parameters for match merging.
 * @param preParseHook Hook to be executed before any submission is parsed. The hook is called with the list of all
 * {@link Submission} instances that will be parsed. The default hook does nothing.
 * @param parseHook Hook to be executed after parsing a single submision. The hook is called with the {@link Submission}
 * that has just been parsed. The default hook does nothing.
 * @param preCompareHook Hook to be executed directly before performing submission comparisons. The hook is called with
 * the list of all {@link SubmissionTuple} instances that will be compared. The default hook does nothing.
 * @param compareHook Hook to be executed after comparing two submissions. The hook is called with the
 * {@link JPlagComparison} result. The default hook does nothing.
 */
public record JPlagOptions(Language language, Integer minimumTokenMatch, Set<File> submissionDirectories, Set<File> oldSubmissionDirectories,
        File baseCodeSubmissionDirectory, String subdirectoryName, List<String> fileSuffixes, String exclusionFileName,
        SimilarityMetric similarityMetric, double similarityThreshold, int maximumNumberOfComparisons, ClusteringOptions clusteringOptions,
        boolean debugParser, MergingOptions mergingOptions, Consumer<List<Submission>> preParseHook, Consumer<Submission> parseHook,
        Consumer<List<SubmissionTuple>> preCompareHook, Consumer<JPlagComparison> compareHook) {

    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0;
    public static final int DEFAULT_SHOWN_COMPARISONS = 100;
    public static final int SHOW_ALL_COMPARISONS = 0;
    public static final SimilarityMetric DEFAULT_SIMILARITY_METRIC = SimilarityMetric.AVG;
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String ERROR_FOLDER = "errors";

    private static final Logger logger = LoggerFactory.getLogger(JPlagOptions.class);

    public static final Consumer<List<Submission>> DEFAULT_PRE_PARSE_HOOK = submissions -> {
    };

    public static final Consumer<Submission> DEFAULT_PARSE_HOOK = submission -> {
    };

    public static final Consumer<List<SubmissionTuple>> DEFAULT_PRE_COMPARE_HOOK = comparisons -> {
    };

    public static final Consumer<JPlagComparison> DEFAULT_COMPARE_HOOK = comparison -> {
    };

    public JPlagOptions(Language language, Set<File> submissionDirectories, Set<File> oldSubmissionDirectories) {
        this(language, null, submissionDirectories, oldSubmissionDirectories, null, null, null, null, DEFAULT_SIMILARITY_METRIC,
                DEFAULT_SIMILARITY_THRESHOLD, DEFAULT_SHOWN_COMPARISONS, new ClusteringOptions(), false, new MergingOptions(), DEFAULT_PRE_PARSE_HOOK,
                DEFAULT_PARSE_HOOK, DEFAULT_PRE_COMPARE_HOOK, DEFAULT_COMPARE_HOOK);
    }

    public JPlagOptions(Language language, Integer minimumTokenMatch, Set<File> submissionDirectories, Set<File> oldSubmissionDirectories,
            File baseCodeSubmissionDirectory, String subdirectoryName, List<String> fileSuffixes, String exclusionFileName,
            SimilarityMetric similarityMetric, double similarityThreshold, int maximumNumberOfComparisons, ClusteringOptions clusteringOptions,
            boolean debugParser, MergingOptions mergingOptions, Consumer<List<Submission>> preParseHook, Consumer<Submission> parseHook,
            Consumer<List<SubmissionTuple>> preCompareHook, Consumer<JPlagComparison> compareHook) {
        this.language = language;
        this.debugParser = debugParser;
        this.fileSuffixes = fileSuffixes == null || fileSuffixes.isEmpty() ? null : Collections.unmodifiableList(fileSuffixes);
        this.similarityThreshold = normalizeSimilarityThreshold(similarityThreshold);
        this.maximumNumberOfComparisons = normalizeMaximumNumberOfComparisons(maximumNumberOfComparisons);
        this.similarityMetric = similarityMetric;
        this.minimumTokenMatch = normalizeMinimumTokenMatch(minimumTokenMatch);
        this.exclusionFileName = exclusionFileName;
        this.submissionDirectories = submissionDirectories == null ? null : Collections.unmodifiableSet(submissionDirectories);
        this.oldSubmissionDirectories = oldSubmissionDirectories == null ? null : Collections.unmodifiableSet(oldSubmissionDirectories);
        this.baseCodeSubmissionDirectory = baseCodeSubmissionDirectory;
        this.subdirectoryName = subdirectoryName;
        this.clusteringOptions = clusteringOptions;
        this.mergingOptions = mergingOptions;
        this.preParseHook = preParseHook;
        this.parseHook = parseHook;
        this.preCompareHook = preCompareHook;
        this.compareHook = compareHook;
    }

    public JPlagOptions withLanguageOption(Language language) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withDebugParser(boolean debugParser) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withFileSuffixes(List<String> fileSuffixes) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withSimilarityThreshold(double similarityThreshold) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withMaximumNumberOfComparisons(int maximumNumberOfComparisons) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withSimilarityMetric(SimilarityMetric similarityMetric) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withMinimumTokenMatch(Integer minimumTokenMatch) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withExclusionFileName(String exclusionFileName) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withSubmissionDirectories(Set<File> submissionDirectories) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withOldSubmissionDirectories(Set<File> oldSubmissionDirectories) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withBaseCodeSubmissionDirectory(File baseCodeSubmissionDirectory) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withSubdirectoryName(String subdirectoryName) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withClusteringOptions(ClusteringOptions clusteringOptions) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withMergingOptions(MergingOptions mergingOptions) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withPreParseHook(Consumer<List<Submission>> preParseHook) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withParseHook(Consumer<Submission> parseHook) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withPreCompareHook(Consumer<List<SubmissionTuple>> preCompareHook) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public JPlagOptions withCompareHook(Consumer<JPlagComparison> compareHook) {
        return new JPlagOptions(language, minimumTokenMatch, submissionDirectories, oldSubmissionDirectories, baseCodeSubmissionDirectory,
                subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
    }

    public boolean hasBaseCode() {
        return baseCodeSubmissionDirectory != null;
    }

    public Set<String> excludedFiles() {
        return Optional.ofNullable(exclusionFileName()).map(this::readExclusionFile).orElse(Collections.emptySet());
    }

    @Override
    public List<String> fileSuffixes() {
        var language = language();
        if ((fileSuffixes == null || fileSuffixes.isEmpty()) && language != null)
            return Arrays.stream(language.suffixes()).toList();
        return fileSuffixes == null ? null : Collections.unmodifiableList(fileSuffixes);
    }

    @Override
    public Integer minimumTokenMatch() {
        var language = language();
        if (minimumTokenMatch == null && language != null)
            return language.minimumTokenMatch();
        return minimumTokenMatch;
    }

    private Set<String> readExclusionFile(final String exclusionFileName) {
        try (BufferedReader reader = FileUtils.openFileReader(new File(exclusionFileName))) {
            final var excludedFileNames = reader.lines().collect(Collectors.toSet());
            if (logger.isDebugEnabled()) {
                logger.debug("Excluded files:{}{}", System.lineSeparator(), String.join(System.lineSeparator(), excludedFileNames));
            }
            return excludedFileNames;
        } catch (IOException e) {
            logger.error("Could not read exclusion file: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    private static double normalizeSimilarityThreshold(double similarityThreshold) {
        if (similarityThreshold > 1) {
            logger.warn("Maximum threshold of 1 used instead of {}", similarityThreshold);
            return 1;
        } else if (similarityThreshold < 0) {
            logger.warn("Minimum threshold of 0 used instead of {}", similarityThreshold);
            return 0;
        } else {
            return similarityThreshold;
        }
    }

    private Integer normalizeMaximumNumberOfComparisons(Integer maximumNumberOfComparisons) {
        return Math.max(maximumNumberOfComparisons, SHOW_ALL_COMPARISONS);
    }

    private Integer normalizeMinimumTokenMatch(Integer minimumTokenMatch) {
        return (minimumTokenMatch != null && minimumTokenMatch < 1) ? Integer.valueOf(1) : minimumTokenMatch;
    }

    /**
     * Creates new options to configure {@link JPlag}.
     * @param language Language to use when parsing the submissions.
     * @param minimumTokenMatch Tunes the comparison sensitivity by adjusting the minimum token required to be counted as
     * matching section. A smaller {@code <n>} increases the sensitivity but might lead to more false-positives.
     * @param submissionDirectory Directory with new submissions. These must be checked for plagiarism. To check more than
     * one submission directory, use the default initializer.
     * @param oldSubmissionDirectories Directories with old submissions to check against.
     * @param baseCodeSubmissionName Path name of the directory containing the base code.
     * @param subdirectoryName Example: If the subdirectoryName is 'src', only the code inside submissionDir/src of each
     * submission will be used for comparison.
     * @param fileSuffixes List of file suffixes that should be included.
     * @param exclusionFileName Name of the file that contains the names of files to exclude from comparison.
     * @param similarityMetric The similarity metric determines how the minimum similarity threshold required for a
     * comparison (of two submissions) is calculated. This affects which comparisons are stored and thus make it into the
     * result object.
     * @param similarityThreshold Percentage value (must be between 0 and 100). Comparisons (of submissions pairs) with a
     * similarity below this threshold will be ignored. The default value of 0 allows all matches to be stored. This affects
     * which comparisons are stored and thus make it into the result object. See also {@link #similarityMetric()}.
     * @param maximumNumberOfComparisons The maximum number of comparisons that will be shown in the generated report. If
     * set to {@link #SHOW_ALL_COMPARISONS} all comparisons will be shown.
     * @param clusteringOptions Clustering options
     * @param debugParser If true, submissions that cannot be parsed will be stored in a separate directory.
     * @deprecated Use the default initializer with @{{@link #baseCodeSubmissionDirectory} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public JPlagOptions(Language language, Integer minimumTokenMatch, File submissionDirectory, Set<File> oldSubmissionDirectories,
            String baseCodeSubmissionName, String subdirectoryName, List<String> fileSuffixes, String exclusionFileName,
            SimilarityMetric similarityMetric, double similarityThreshold, int maximumNumberOfComparisons, ClusteringOptions clusteringOptions,
            boolean debugParser, MergingOptions mergingOptions, Consumer<List<Submission>> preParseHook, Consumer<Submission> parseHook,
            Consumer<List<SubmissionTuple>> preCompareHook, Consumer<JPlagComparison> compareHook) throws BasecodeException {
        this(language, minimumTokenMatch, Set.of(submissionDirectory), oldSubmissionDirectories,
                convertLegacyBaseCodeToFile(baseCodeSubmissionName, submissionDirectory), subdirectoryName, fileSuffixes, exclusionFileName,
                similarityMetric, similarityThreshold, maximumNumberOfComparisons, clusteringOptions, debugParser, mergingOptions, preParseHook,
                parseHook, preCompareHook, compareHook);
    }

    /**
     * Creates a new options instance with the provided base code submission name
     * @param baseCodeSubmissionName the path or name of the base code submission
     * @return a new options instance with the provided base code submission name
     * @deprecated Use @{{@link #withBaseCodeSubmissionDirectory} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    public JPlagOptions withBaseCodeSubmissionName(String baseCodeSubmissionName) {
        File baseCodeDirectory = new File(baseCodeSubmissionName);
        if (baseCodeDirectory.exists()) {
            return this.withBaseCodeSubmissionDirectory(baseCodeDirectory);
        }

        if (submissionDirectories.size() != 1) {
            throw new IllegalArgumentException("Partial path based base code requires exactly one submission directory");
        }
        File submissionDirectory = submissionDirectories.iterator().next();
        try {
            return new JPlagOptions(language, minimumTokenMatch, submissionDirectory, oldSubmissionDirectories, baseCodeSubmissionName,
                    subdirectoryName, fileSuffixes, exclusionFileName, similarityMetric, similarityThreshold, maximumNumberOfComparisons,
                    clusteringOptions, debugParser, mergingOptions, preParseHook, parseHook, preCompareHook, compareHook);
        } catch (BasecodeException e) {
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Converts a legacy base code submission name to a directory path.
     * @deprecated Use the default initializer with @{{@link #baseCodeSubmissionDirectory} instead.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    private static File convertLegacyBaseCodeToFile(String baseCodeSubmissionName, File submissionDirectory) throws BasecodeException {
        if (baseCodeSubmissionName == null) {
            return null;
        }
        File baseCodeAsAbsolutePath = new File(baseCodeSubmissionName);
        if (baseCodeAsAbsolutePath.exists()) {
            return baseCodeAsAbsolutePath;
        } else {
            String normalizedName = baseCodeSubmissionName;
            while (normalizedName.startsWith(File.separator)) {
                normalizedName = normalizedName.substring(1);
            }
            while (normalizedName.endsWith(File.separator)) {
                normalizedName = normalizedName.substring(0, normalizedName.length() - 1);
            }
            if (normalizedName.isEmpty() || normalizedName.contains(File.separator) || normalizedName.contains(".")) {
                throw new BasecodeException(
                        "The basecode directory name \"" + normalizedName + "\" cannot contain dots! Please migrate to the path-based API.");
            }
            return new File(submissionDirectory, baseCodeSubmissionName);
        }
    }
}
