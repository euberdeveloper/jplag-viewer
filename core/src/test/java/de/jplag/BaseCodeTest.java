package de.jplag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.jplag.exceptions.BasecodeException;
import de.jplag.exceptions.ExitException;
import de.jplag.exceptions.RootDirectoryException;

public class BaseCodeTest extends TestBase {

    @Test
    void testBasecodeUserSubmissionComparison() throws ExitException {
        JPlagResult result = runJPlag("basecode", it -> it.withBaseCodeSubmissionPath("base"));
        verifyResults(result);
    }

    @Test
    void testTinyBasecode() {
        assertThrows(BasecodeException.class, () -> runJPlag("TinyBasecode", it -> it.withBaseCodeSubmissionPath("base")));
    }

    @Test
    void testEmptySubmission() throws ExitException {
        JPlagResult result = runJPlag("emptysubmission", it -> it.withBaseCodeSubmissionPath("base"));
        verifyResults(result);
    }

    @Test
    void testAutoTrimFileSeparators() throws ExitException {
        JPlagResult result = runJPlag("basecode", it -> it.withBaseCodeSubmissionPath(File.separator + "base" + File.separator));
        verifyResults(result);
    }

    private void verifyResults(JPlagResult result) {
        assertEquals(2, result.getNumberOfSubmissions());
        assertEquals(1, result.getAllComparisons().size());
        assertEquals(1, result.getAllComparisons().get(0).matches().size());
        assertEquals(1, result.getSimilarityDistribution()[1]);
        assertEquals(85, result.getAllComparisons().get(0).similarity(), DELTA);
    }

    @Test
    void testBasecodePathComparison() throws ExitException {
        JPlagResult result = runJPlag("basecode", it -> it.withBaseCodeSubmissionPath(getBasePath("basecode-base")));
        assertEquals(3, result.getNumberOfSubmissions()); // "basecode/base" is now a user submission.
    }

    @Test
    void testInvalidRoot() {
        assertThrows(RootDirectoryException.class, () -> runJPlag("basecode", it -> it.withSubmissionDirectories(List.of("WrongRoot"))));
    }

    @Test
    void testInvalidBasecode() {
        assertThrows(BasecodeException.class, () -> runJPlag("basecode", it -> it.withBaseCodeSubmissionPath("WrongBasecode")));
    }

    @Test
    void testBasecodeUserSubmissionWithDots() {
        assertThrows(BasecodeException.class, () -> runJPlag("basecode", it -> it.withBaseCodeSubmissionPath("base.ext")));
    }

    /**
     * The simple duplicate contains obvious plagiarism.
     */
    @Test
    void testSubdirectoryGlobalBasecode() throws ExitException {
        String basecode = getBasePath("SubdirectoryBase");
        JPlagResult result = runJPlag("SubdirectoryDuplicate", it -> it.withSubdirectoryName("src").withBaseCodeSubmissionPath(basecode));
        verifySimpleDuplicate(result, 3, 3);
    }

    /**
     * The simple duplicate contains obvious plagiarism.
     */
    @Test
    void testSubdirectoryLocalBasecode() throws ExitException {
        JPlagResult result = runJPlag("SubdirectoryDuplicate", it -> it.withSubdirectoryName("src").withBaseCodeSubmissionPath("Base"));
        verifySimpleDuplicate(result, 2, 1);
    }

    private void verifySimpleDuplicate(JPlagResult result, int submissions, int comparisons) {
        assertEquals(submissions, result.getNumberOfSubmissions());
        assertEquals(comparisons, result.getAllComparisons().size());
        assertEquals(1, result.getAllComparisons().get(0).matches().size());
        assertEquals(1, result.getSimilarityDistribution()[3]);
        assertEquals(62.07, result.getAllComparisons().get(0).similarity(), DELTA);
    }
}
