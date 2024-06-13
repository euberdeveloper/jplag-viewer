package de.jplag.java;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.kohsuke.MetaInfServices;

import de.jplag.ParsingException;
import de.jplag.Token;

/**
 * Language for Java 9 and newer.
 */
@MetaInfServices(de.jplag.Language.class)
public class JavaLanguage implements de.jplag.Language {
    private static final String IDENTIFIER = "java";
    public static final int JAVA_VERSION = 21;

    private final Parser parser;

    public JavaLanguage() {
        parser = new Parser();
    }

    @Override
    public String[] suffixes() {
        return new String[] {".java", ".JAVA"};
    }

    @Override
    public String getName() {
        return "Java";
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int minimumTokenMatch() {
        return 9;
    }

    @Override
    public List<Token> parse(Set<File> files, boolean normalize) throws ParsingException {
        return this.parser.parse(files);
    }

    @Override
    public boolean tokensHaveSemantics() {
        return true;
    }

    @Override
    public boolean supportsNormalization() {
        return true;
    }

    @Override
    public String toString() {
        return this.getIdentifier();
    }
}
