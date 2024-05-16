package de.jplag.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.JPlag;
import de.jplag.JPlagResult;
import de.jplag.cli.logger.CliProgressBarProvider;
import de.jplag.cli.logger.CollectedLoggerFactory;
import de.jplag.cli.logger.JPlagLoggerBase;
import de.jplag.cli.picocli.CliInputHandler;
import de.jplag.exceptions.ExitException;
import de.jplag.logging.ProgressBarLogger;
import de.jplag.options.JPlagOptions;

/**
 * Command line interface class, allows using via command line.
 * @see CLI#main(String[])
 */
public final class CLI {
    private static final Logger logger = LoggerFactory.getLogger(CLI.class);

    private static final String DEFAULT_FILE_ENDING = ".zip";

    private final CliInputHandler inputHandler;

    /**
     * Creates a cli.
     * @param args The command line arguments
     */
    public CLI(String[] args) {
        this.inputHandler = new CliInputHandler(args);
    }

    /**
     * Executes the cli
     * @throws ExitException If anything on the side of JPlag goes wrong
     * @throws IOException If any files did not work
     */
    public void executeCli() throws ExitException, IOException {
        logger.debug("Your version of JPlag is {}", JPlag.JPLAG_VERSION);

        if (!this.inputHandler.parse()) {
            JPlagLoggerBase.currentLogLevel = this.inputHandler.getCliOptions().advanced.logLevel;
            ProgressBarLogger.setProgressBarProvider(new CliProgressBarProvider());

            switch (this.inputHandler.getCliOptions().mode) {
                case RUN -> runJPlag();
                case VIEW -> runViewer(null);
                case RUN_AND_VIEW -> runViewer(runJPlag());
            }
        }
    }

    /**
     * Executes the cli and handles the exceptions that might occur.
     * @return true, if an exception has been caught.
     */
    public boolean executeCliAndHandleErrors() {
        boolean hadErrors = false;

        try {
            this.executeCli();
        } catch (IOException | ExitException exception) {
            if (exception.getCause() != null) {
                logger.error("{} - {}", exception.getMessage(), exception.getCause().getMessage());
            } else {
                logger.error(exception.getMessage());
            }
            hadErrors = true;
        } finally {
            finalizeLogger();
        }

        return hadErrors;
    }

    /**
     * Runs JPlag and returns the file the result has been written to
     * @return The file containing the result
     * @throws ExitException If JPlag threw an exception
     * @throws FileNotFoundException If the file could not be written
     */
    public File runJPlag() throws ExitException, FileNotFoundException {
        JPlagOptionsBuilder optionsBuilder = new JPlagOptionsBuilder(this.inputHandler);
        JPlagOptions options = optionsBuilder.buildOptions();
        JPlagResult result = JPlagRunner.runJPlag(options);

        File target = new File(getResultFilePath());
        OutputFileGenerator.generateJPlagResultZip(result, target);
        OutputFileGenerator.generateCsvOutput(result, new File(getResultFileBaseName()), this.inputHandler.getCliOptions());

        return target;
    }

    /**
     * Runs the report viewer using the given file as the default result.zip
     * @param zipFile The zip file to pass to the viewer. Can be null, if no result should be opened by default
     * @throws IOException If something went wrong with the internal server
     */
    public void runViewer(File zipFile) throws IOException {
        JPlagRunner.runInternalServer(zipFile, this.inputHandler.getCliOptions().advanced.port);
    }

    private void finalizeLogger() {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof CollectedLoggerFactory collectedLoggerFactory)) {
            return;
        }
        collectedLoggerFactory.finalizeInstances();
    }

    private String getResultFilePath() {
        String optionValue = this.inputHandler.getCliOptions().resultFile;
        if (optionValue.endsWith(DEFAULT_FILE_ENDING)) {
            return optionValue;
        }
        return optionValue + DEFAULT_FILE_ENDING;
    }

    private String getResultFileBaseName() {
        String defaultOutputFile = getResultFilePath();
        return defaultOutputFile.substring(0, defaultOutputFile.length() - DEFAULT_FILE_ENDING.length());
    }

    public static void main(String[] args) {
        CLI cli = new CLI(args);
        if (cli.executeCliAndHandleErrors()) {
            System.exit(1);
        }
    }
}
