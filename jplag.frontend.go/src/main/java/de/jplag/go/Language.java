package de.jplag.go;

import java.io.File;

import de.jplag.ErrorConsumer;
import de.jplag.TokenList;

public class Language implements de.jplag.Language {

    private static final String SHORT_NAME = "Go";
    private static final String NAME = "Go Parser";
    private static final int DEFAULT_MIN_TOKEN_MATCH = 8;
    private final GoParserAdapter parserAdapter;

    public Language(ErrorConsumer consumer) {
        this.parserAdapter = new GoParserAdapter(consumer);
    }

    @Override
    public String[] suffixes() {
        return new String[] {".go"};
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    @Override
    public int minimumTokenMatch() {
        return DEFAULT_MIN_TOKEN_MATCH;
    }

    @Override
    public TokenList parse(File directory, String[] files) {
        return parserAdapter.parse(directory, files);
    }

    @Override
    public boolean hasErrors() {
        return parserAdapter.hasErrors();
    }

    @Override
    public boolean supportsColumns() {
        return true;
    }

    @Override
    public boolean isPreformatted() {
        return true;
    }

    @Override
    public boolean usesIndex() {
        return true;
    }

    @Override
    public int numberOfTokens() {
        return GoTokenConstants.NUM_DIFF_TOKENS;
    }
}
