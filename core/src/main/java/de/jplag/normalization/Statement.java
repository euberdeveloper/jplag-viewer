package de.jplag.normalization;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.jplag.Token;
import de.jplag.semantics.CodeSemantics;

class Statement implements Comparable<Statement> {

    private final List<Token> tokens;
    private final int lineNumber;
    private final CodeSemantics semantics;

    Statement(List<Token> tokens, int lineNumber) {
        this.tokens = Collections.unmodifiableList(tokens);
        this.lineNumber = lineNumber;
        this.semantics = CodeSemantics.join(tokens.stream().map(Token::getSemantics).toList());
    }

    List<Token> tokens() {
        return tokens;
    }

    CodeSemantics semantics() {
        return semantics;
    }

    void markKeep() {
        semantics.markKeep();
    }

    private int tokenOrdinal(Token token) {
        return ((Enum<?>) token.getType()).ordinal(); // reflects the order the enums were declared in
    }

    @Override
    public int compareTo(Statement other) {
        int sizeComp = Integer.compare(this.tokens.size(), other.tokens.size());
        if (sizeComp != 0)
            return -sizeComp; // bigger size should come first
        Iterator<Token> myTokens = this.tokens.iterator();
        Iterator<Token> otherTokens = other.tokens.iterator();
        for (int i = 0; i < this.tokens.size(); i++) {
            int tokenComp = Integer.compare(tokenOrdinal(myTokens.next()), tokenOrdinal(otherTokens.next()));
            if (tokenComp != 0)
                return tokenComp;
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        return tokens.equals(((Statement) object).tokens);
    }

    @Override
    public int hashCode() {
        return tokens.hashCode();
    }

    @Override
    public String toString() {
        return lineNumber + ": " + String.join(" ", tokens.stream().map(Token::toString).toList());
    }
}
