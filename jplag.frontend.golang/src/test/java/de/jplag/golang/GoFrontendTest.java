package de.jplag.golang;

import static de.jplag.golang.GoTokenTestUtils.getDummyToken;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jplag.Token;
import de.jplag.TokenConstants;
import de.jplag.TokenList;
import de.jplag.TokenPrinter;

class GoFrontendTest {
    /**
     * Test source file that is supposed to produce a complete set of tokens, i.e. all types of tokens.
     */
    private static final String COMPLETE_TEST_FILE = "Complete.go";

    /**
     * Regular expression that describes lines consisting only of whitespace and optionally a line comment.
     */
    private static final String EMPTY_OR_SINGLE_LINE_COMMENT = "\\s*(//.*|/\\*.*\\*/)?";

    /**
     * Regular expression that describes lines containing the start of a multiline comment and no code before it.
     */
    private static final String DELIMITED_COMMENT_START = "\\s*/\\*(?:(?!\\*/).)*$";

    /**
     * Regular expression that describes lines containing the end of a multiline comment and no more code after that.
     */
    private static final String DELIMITED_COMMENT_END = ".*\\*/\\s*$";

    private final Logger logger = LoggerFactory.getLogger("GoLang frontend test");
    private final String[] testFiles = new String[] {COMPLETE_TEST_FILE};
    private final File testFileLocation = Path.of("src", "test", "resources", "de", "jplag", "golang").toFile();
    private Language language;

    @BeforeEach
    void setup() {
        language = new Language();
    }

    @Test
    void parseTestFiles() {
        for (String fileName : testFiles) {
            TokenList tokens = language.parse(testFileLocation, new String[] {fileName});
            String output = TokenPrinter.printTokens(tokens, testFileLocation);
            logger.info(output);

            testSourceCoverage(fileName, tokens);
            if (fileName.equals(COMPLETE_TEST_FILE)) {
                testTokenCoverage(tokens, fileName);
            }
        }
    }

    /**
     * Confirms that every type of GoToken has a Sting representation associated to it.
     */
    @Test
    void testTokenToString() {
        var missingTokens = IntStream.range(0, GoTokenConstants.NUM_DIFF_TOKENS).mapToObj(GoTokenTestUtils::getDummyToken)
                .filter(token -> token.type2string().contains("UNKNOWN")).toList();

        if (!missingTokens.isEmpty()) {
            var typeList = missingTokens.stream().map(Token::getType).map(Object::toString).collect(Collectors.joining(", "));
            fail("Found token types with no string representation: %s".formatted(typeList));
        }

    }

    /**
     * Confirms that the code is covered to a basic extent, i.e. each line of code contains at least one token.
     * @param fileName a code sample file name
     * @param tokens the TokenList generated from the sample
     */
    private void testSourceCoverage(String fileName, TokenList tokens) {
        File testFile = new File(testFileLocation, fileName);

        List<String> lines = null;
        try {
            lines = Files.readAllLines(testFile.toPath());
        } catch (IOException exception) {
            logger.info("Error while reading test file %s".formatted(fileName), exception);
            fail();
        }

        // All lines that contain code
        var codeLines = getCodeLines(lines);
        // All lines that contain a token
        var tokenLines = IntStream.range(0, tokens.size()).mapToObj(tokens::getToken).mapToInt(Token::getLine).distinct().boxed().toList();

        if (codeLines.size() > tokenLines.size()) {
            List<Integer> missedLinesIndices = new ArrayList<>(codeLines);
            missedLinesIndices.removeAll(tokenLines);
            var missedLines = missedLinesIndices.stream().map(Object::toString).collect(Collectors.joining(", "));
            if (!missedLines.isBlank()) {
                fail("Found lines in file '%s' that are not represented in the token list. \n\tMissed lines: %s".formatted(fileName, missedLines));
            }
        }
        OptionalInt differingLine = IntStream.range(0, codeLines.size())
                .dropWhile(index -> Objects.equals(codeLines.get(index), tokenLines.get(index))).findAny();
        differingLine.ifPresent(
                i -> fail("Not all lines of code in '%s' are represented in tokens, starting with line %d.".formatted(fileName, codeLines.get(i))));
    }

    /**
     * Gets the line numbers of lines containing actual code, omitting empty lines and comment lines.
     * @param lines lines of a code file
     * @return an array of the line numbers of code lines
     */
    private List<Integer> getCodeLines(List<String> lines) {
        // This boxed boolean can be accessed from within the lambda method below
        var state = new Object() {
            boolean insideComment = false;
        };

        var codeLines = IntStream.rangeClosed(1, lines.size()).sequential().filter(idx -> {
            String line = lines.get(idx - 1);
            if (line.matches(EMPTY_OR_SINGLE_LINE_COMMENT)) {
                return false;
            } else if (line.matches(DELIMITED_COMMENT_START)) {
                state.insideComment = true;
                return false;
            } else if (state.insideComment) {
                // This fails if code follows after '*/'. If the code is formatted well, this should not happen.
                if (line.matches(DELIMITED_COMMENT_END)) {
                    state.insideComment = false;
                }
                return false;
            }
            return true;
        });

        return codeLines.boxed().toList();

    }

    /**
     * Confirms that all Token types are 'reachable' with a complete code example.
     * @param tokens TokenList which is supposed to contain all types of tokens
     * @param fileName The file name of the complete code example
     */
    private void testTokenCoverage(TokenList tokens, String fileName) {
        var foundTokens = tokens.allTokens().stream().parallel().mapToInt(Token::getType).sorted().distinct().boxed().toList();

        // Exclude SEPARATOR_TOKEN, as it does not occur
        var missingTokenTypes = IntStream.range(0, GoTokenConstants.NUM_DIFF_TOKENS).filter(i -> i != TokenConstants.SEPARATOR_TOKEN).boxed()
                .collect(Collectors.toList());
        missingTokenTypes.removeAll(foundTokens);

        if (!missingTokenTypes.isEmpty()) {
            String missingTypesList = missingTokenTypes.stream().map(type -> getDummyToken(type).type2string()).collect(Collectors.joining(", "));
            fail("Some Token types were not found in the complete code example '%s': %s.".formatted(fileName, missingTypesList));
        }

    }

}
