
package de.jplag.text;

import java.io.File;

import de.jplag.Token;
import de.jplag.TokenList;

public class Language implements de.jplag.Language {

    private Parser parser;

    public Language() {
        this.parser = new Parser();
    }

    @Override
    public int errorCount() {
        return this.parser.errorsCount();
    }

    @Override
    public String[] suffixes() {
        String[] res = {".TXT", ".txt", ".ASC", ".asc", ".TEX", ".tex"};
        return res;
    }

    @Override
    public String getName() {
        return "Text AbstractParser";
    }

    @Override
    public String getShortName() {
        return "text";
    }

    @Override
    public int minimumTokenMatch() {
        return 5;
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
        return false;
    }

    @Override
    public boolean usesIndex() {
        return false;
    }

    @Override
    public int numberOfTokens() {
        return parser.serial;
    }

    @Override
    public String type2string(int type) {
        return Token.type2string(type);
    }
}
