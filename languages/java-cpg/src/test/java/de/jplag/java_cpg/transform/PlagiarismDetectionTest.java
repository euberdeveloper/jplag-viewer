package de.jplag.java_cpg.transform;

import static de.jplag.java_cpg.transformation.TransformationRepository.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.jplag.ParsingException;
import de.jplag.Token;
import de.jplag.java_cpg.JavaCpgLanguage;
import de.jplag.java_cpg.transformation.GraphTransformation;

/**
 * An integration test that checks whether pairs of submissions are accepted as equal after being subjected to different
 * transformations.
 */
public class PlagiarismDetectionTest {

    protected static final Path BASE_PATH = Path.of("src", "test", "resources", "java");
    protected static File baseDirectory;
    private static JavaCpgLanguage language;

    @BeforeAll
    public static void setUpOnce() {
        language = new JavaCpgLanguage();
        baseDirectory = BASE_PATH.toFile();
    }

    private static Stream<Arguments> getArguments() {
        return Stream.of(Arguments.of("singleUseVariable", new GraphTransformation[] {inlineSingleUseVariable}),
                Arguments.of("constantClass", new GraphTransformation[] {moveConstantToOnlyUsingClass, removeLibraryRecord, removeLibraryField}),
                Arguments.of("for2While", new GraphTransformation[] {forStatementToWhileStatement}),
                Arguments.of("negatedIf", new GraphTransformation[] {ifWithNegatedConditionResolution}),
                Arguments.of("unusedVariables", new GraphTransformation[] {removeUnusedVariableDeclaration, removeEmptyDeclarationStatement}),
                Arguments.of("dfgLinearization", new GraphTransformation[] {}));
    }

    @ParameterizedTest
    @MethodSource("getArguments")
    void testPlagiarismPair(String submissionsPath, GraphTransformation[] transformation) {
        language.resetTransformations();
        language.addTransformations(transformation);

        File root = new File(baseDirectory, submissionsPath);
        File[] content = root.listFiles();
        Assertions.assertNotNull(content);
        List<Set<File>> submissions = new ArrayList<>();
        if (Arrays.stream(content).anyMatch(File::isDirectory)) {
            Arrays.stream(content).map(submissionDir -> {
                try (Stream<Path> stream = Files.walk(submissionDir.toPath())) {
                    return stream.map(Path::toFile).filter(File::isFile).collect(Collectors.toSet());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).forEach(submissions::add);
        } else {
            // single-file submission
            Arrays.stream(content).map(Set::of).forEach(submissions::add);
        }

        List<List<Token>> results = new ArrayList<>();
        for (Set<File> submissionFiles : submissions) {
            try {
                List<Token> tokens = language.parse(submissionFiles, true);
                results.add(tokens);
            } catch (ParsingException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                Assertions.assertIterableEquals(results.get(i), results.get(j));
            }
        }
    }

    @AfterEach
    public void resetTransformations() {
        language.resetTransformations();
    }
}
