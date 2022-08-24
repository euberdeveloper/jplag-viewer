package de.jplag.emf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.TokenList;
import de.jplag.TokenPrinter;
import de.jplag.testutils.FileUtil;
import de.jplag.testutils.TokenUtils;

class MinimalMetamodelTest {
    private final Logger logger = LoggerFactory.getLogger("JPlag-Test");

    private static final Path BASE_PATH = Path.of("src", "test", "resources", "de", "jplag", "models");
    private static final String[] TEST_SUBJECTS = {"bookStore.ecore", "bookStoreExtended.ecore", "bookStoreRenamed.ecore"};

    private de.jplag.Language frontend;
    private File baseDirectory;

    @BeforeEach
    public void setUp() {
        frontend = new Language();
        baseDirectory = BASE_PATH.toFile();
        FileUtil.assertDirectory(baseDirectory, TEST_SUBJECTS);
    }

    @Test
    void testBookstoreMetamodels() {
        TokenList result = frontend.parse(baseDirectory, TEST_SUBJECTS);

        logger.debug(TokenPrinter.printTokens(result, baseDirectory, Optional.of(Language.VIEW_FILE_SUFFIX)));
        Field[] fields = MetamodelTokenConstants.class.getFields();
        var constants = Arrays.stream(fields).map(Field::getName).filter(it -> !it.equals("NUM_DIFF_TOKENS")).toList();
        logger.info(("Handcrafted token set: " + constants));
        logger.info("Parsed tokens: " + result.allTokens().toString());
        assertEquals(21, constants.size());
        assertEquals(43, result.size());

        var bookstoreTokens = TokenUtils.tokenTypesByFile(result, TEST_SUBJECTS[0]);
        var bookstoreRenamedTokens = TokenUtils.tokenTypesByFile(result, TEST_SUBJECTS[2]);
        var bookstoreExtendedTokens = TokenUtils.tokenTypesByFile(result, TEST_SUBJECTS[1]);
        assertTrue(bookstoreTokens.size() < bookstoreExtendedTokens.size());
        assertIterableEquals(bookstoreTokens, bookstoreRenamedTokens);
    }

    @AfterEach
    public void tearDown() {
        FileUtil.clearFiles(new File(BASE_PATH.toString()), Language.VIEW_FILE_SUFFIX);
    }

}
