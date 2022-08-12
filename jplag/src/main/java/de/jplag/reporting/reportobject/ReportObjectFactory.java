package de.jplag.reporting.reportobject;

import static de.jplag.reporting.jsonfactory.DirectoryManager.*;
import static de.jplag.reporting.reportobject.mapper.SubmissionNameToIdMapper.buildSubmissionNameToIdMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.JPlagComparison;
import de.jplag.JPlagResult;
import de.jplag.Language;
import de.jplag.Submission;
import de.jplag.reporting.jsonfactory.ComparisonReportWriter;
import de.jplag.reporting.jsonfactory.FileWriter;
import de.jplag.reporting.reportobject.mapper.ClusteringResultMapper;
import de.jplag.reporting.reportobject.mapper.MetricMapper;
import de.jplag.reporting.reportobject.model.Metric;
import de.jplag.reporting.reportobject.model.OverviewReport;

/**
 * Factory class, responsible for converting a JPlagResult object to Overview and Comparison DTO classes and writing it
 * to the disk.
 */
public class ReportObjectFactory {
    private static final Logger logger = LoggerFactory.getLogger(ReportObjectFactory.class);

    private static final FileWriter fileWriter = new FileWriter();
    public static final String OVERVIEW_FILE_NAME = "overview.json";
    public static final String SUBMISSIONS_FOLDER = "submissions";
    private Map<String, String> submissionNameToIdMap;
    private Function<Submission, String> submissionToIdFunction;
    private Map<String, Map<String, String>> submissionNameToNameToComparisonFileName;

    /**
     * Creates all necessary report viewer files, writes them to the disk as zip.
     * @param result The JPlagResult to be converted into a report.
     * @param path The Path to save the report to
     */
    public void createAndSaveReport(JPlagResult result, String path) {

        try {
            createDirectory(path);
            buildSubmissionToIdMap(result);

            copySubmissionFilesToReport(path, result);

            writeComparisons(result, path);
            writeOverview(result, path);

            zipAndDelete(path);
        } catch (IOException e) {
            logger.error("Could not create directory " + path + " for report viewer generation", e);
        }

    }

    private void zipAndDelete(String path) {
        var zipWasSuccessful = zipDirectory(path);
        if (zipWasSuccessful) {
            deleteDirectory(path);
        } else {
            logger.error("Could not zip results. The results are still available uncompressed at " + path);
        }
    }

    private void buildSubmissionToIdMap(JPlagResult result) {
        submissionNameToIdMap = buildSubmissionNameToIdMap(result);
        submissionToIdFunction = (Submission submission) -> submissionNameToIdMap.get(submission.getName());
    }

    private void copySubmissionFilesToReport(String path, JPlagResult result) {
        List<JPlagComparison> comparisons = result.getComparisons(result.getOptions().getMaximumNumberOfComparisons());
        var submissions = getSubmissions(comparisons);
        var submissionsPath = createSubmissionsDirectory(path);
        if (submissionsPath == null)
            return;
        Language language = result.getOptions().getLanguage();
        for (var submission : submissions) {
            File directory = createSubmissionDirectory(path, submissionsPath, submission);
            if (directory == null)
                continue;
            for (var file : submission.getFiles()) {
                var fileToCopy = language.useViewFiles() ? new File(file.getPath() + language.viewFileSuffix()) : file;
                try {
                    Files.copy(fileToCopy.toPath(), (new File(directory, file.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    logger.error("Could not save submission file " + fileToCopy, e);
                }
            }
        }
    }

    private File createSubmissionDirectory(String path, File submissionsPath, Submission submission) {
        File directory;
        try {
            directory = createDirectory(submissionsPath.getPath(), submissionToIdFunction.apply(submission));
        } catch (IOException e) {
            logger.error("Could not create directory " + path + " for report viewer generation", e);
            return null;
        }
        return directory;
    }

    private File createSubmissionsDirectory(String path) {
        File submissionsPath;
        try {
            submissionsPath = createDirectory(path, SUBMISSIONS_FOLDER);
        } catch (IOException e) {
            logger.error("Could not create directory " + path + " for report viewer generation", e);
            return null;
        }
        return submissionsPath;
    }

    private void writeComparisons(JPlagResult result, String path) {
        ComparisonReportWriter comparisonReportWriter = new ComparisonReportWriter(submissionToIdFunction);
        submissionNameToNameToComparisonFileName = comparisonReportWriter.writeComparisonReports(result, path);
    }

    private void writeOverview(JPlagResult result, String path) {

        List<String> folders = new ArrayList<>();
        folders.addAll(result.getOptions().getSubmissionDirectories());
        folders.addAll(result.getOptions().getOldSubmissionDirectories());

        String baseCodePath = result.getOptions().hasBaseCode() ? result.getOptions().getBaseCodeSubmissionName().orElse("") : "";
        ClusteringResultMapper clusteringResultMapper = new ClusteringResultMapper(submissionToIdFunction);

        OverviewReport overviewReport = new OverviewReport(folders, // submissionFolderPath
                baseCodePath, // baseCodeFolderPath
                result.getOptions().getLanguage().getName(), // language
                List.of(result.getOptions().getFileSuffixes()), // fileExtensions
                submissionNameToIdMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)), // submissionIds
                submissionNameToNameToComparisonFileName, // result.getOptions().getMinimumTokenMatch(),
                List.of(), // failedSubmissionNames
                result.getOptions().getExcludedFiles(), // excludedFiles
                result.getOptions().getMinimumTokenMatch(), // matchSensitivity
                getDate(),// dateOfExecution
                result.getDuration(), // executionTime
                getMetrics(result),// metrics
                clusteringResultMapper.map(result)); // clusters

        fileWriter.saveAsJSON(overviewReport, path, OVERVIEW_FILE_NAME);

    }

    private Set<Submission> getSubmissions(List<JPlagComparison> comparisons) {
        var submissions = comparisons.stream().map(JPlagComparison::getFirstSubmission).collect(Collectors.toSet());
        Set<Submission> secondSubmissions = comparisons.stream().map(JPlagComparison::getSecondSubmission).collect(Collectors.toSet());
        submissions.addAll(secondSubmissions);
        return submissions;
    }

    /**
     * Gets the used metrics in a JPlag comparison. As Max Metric is included in every JPlag run, this always include Max
     * Metric.
     * @return A list contains Metric DTOs.
     */
    private List<Metric> getMetrics(JPlagResult result) {
        MetricMapper metricMapper = new MetricMapper(submissionToIdFunction);
        return List.of(metricMapper.getAverageMetric(result), metricMapper.getMaxMetric(result));
    }

    private String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
