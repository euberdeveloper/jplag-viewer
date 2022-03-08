package de.jplag.scheme;

import java.io.File;

import de.jplag.AbstractParser;
import de.jplag.TokenList;

public class Parser extends AbstractParser implements SchemeTokenConstants {
    private String currentFile;

    private TokenList tokens;

    public TokenList parse(File directory, String files[]) {
        tokens = new TokenList();
        errors = 0;
        for (int i = 0; i < files.length; i++) {
            currentFile = files[i];
            getErrorConsumer().print(null, "Parsing file " + files[i]);
            if (!SchemeParser.parseFile(directory, files[i], null, this))
                errors++;
            tokens.addToken(new SchemeToken(FILE_END, currentFile));
        }
        this.parseEnd();
        return tokens;
    }

    public void add(int type, Token token) {
        int length = token.endColumn - token.beginColumn + 1;
        tokens.addToken(new SchemeToken(type, currentFile, token.beginLine, token.endLine, length));
    }

}
