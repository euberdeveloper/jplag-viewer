package de.jplag.python3;

import java.io.File;

import de.jplag.TokenList;

public class Language implements de.jplag.Language {

    private Parser parser;

    public Language() {
        this.parser = new Parser();
    }

    @Override
    public String[] suffixes() {
        return new String[] {".py"};
    }

    @Override
    public int errorCount() {
        return this.parser.errorsCount();
    }

    @Override
    public String getName() {
        return "Python3 Parser";
    }

    @Override
    public String getShortName() {
        return "python3";
    }

    @Override
    public int minimumTokenMatch() {
        return 12;
    }

    @Override
    public TokenList parse(File dir, String[] files) {
        return this.parser.parse(dir, files);
    }

    @Override
    public boolean hasErrors() {
        return this.parser.hasErrors();
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
        return false;
    }

    @Override
    public int numberOfTokens() {
        return Python3TokenConstants.NUM_DIFF_TOKENS;
    }
}
