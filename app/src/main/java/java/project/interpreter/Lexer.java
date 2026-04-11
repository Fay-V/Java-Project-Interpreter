// Lexer.java
package java.project.interpreter;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Lexer {
    private final String source;
    private int pos = 0;
    private int line = 1;
    private int lineStart = 0;

    private static final Map<String, TokenType> RESERVED_WORDS = Map.ofEntries(
        // Keywords
        Map.entry("START",   TokenType.KEYWORD),
        Map.entry("END",     TokenType.KEYWORD),
        Map.entry("SCRIPT",  TokenType.KEYWORD),
        Map.entry("AREA",    TokenType.KEYWORD),
        Map.entry("DECLARE", TokenType.KEYWORD),
        Map.entry("PRINT",   TokenType.KEYWORD),

        // Types
        Map.entry("STRING",  TokenType.TYPE),
        Map.entry("INT",     TokenType.TYPE),
        Map.entry("CHAR",    TokenType.TYPE),
        Map.entry("BOOL",    TokenType.TYPE),

        // Bool literals
        Map.entry("TRUE",    TokenType.BOOL_LITERAL),
        Map.entry("FALSE",   TokenType.BOOL_LITERAL)
        
    );

    private static final Map<String, TokenType> SYMBOLS = Map.ofEntries(
        // Operators
        Map.entry(":=", TokenType.OPERATOR),
        Map.entry("=",  TokenType.OPERATOR),
        Map.entry("&",  TokenType.OPERATOR),

        // Punctuation
        Map.entry(":",  TokenType.PUNCTUATION),
        Map.entry(";",  TokenType.PUNCTUATION),
        Map.entry("(",  TokenType.PUNCTUATION),
        Map.entry(")",  TokenType.PUNCTUATION),

        // Comment marker
        Map.entry("%%", TokenType.COMMENT)
    );

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < source.length()) {
            skipWhitespace();
            if (pos >= source.length()) break;

            char c = source.charAt(pos);

            if (c == '"') {
                tokens.add(readStringLiteral());
            } else if (c == '\'') {
                tokens.add(readCharLiteral());
            } else if (Character.isLetter(c)) {
                tokens.add(readWord());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else {
                tokens.add(readSymbol());
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF", line, column()));
        return tokens;
    }

    // --- Readers ---

    private Token readWord() {
        int col = column();
        StringBuilder sb = new StringBuilder();

        while (pos < source.length() && Character.isLetterOrDigit(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            pos++;
        }

        String word = sb.toString();

        // look up the word in the reserved map, default to IDENTIFIER if not found
        TokenType type = RESERVED_WORDS.getOrDefault(word, TokenType.IDENTIFIER);
        return new Token(type, word, line, col);
    }

    private Token readNumber() {
        int col = column();
        StringBuilder sb = new StringBuilder();

        while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            pos++;
        }

        return new Token(TokenType.INT_LITERAL, sb.toString(), line, col);
    }

    private Token readStringLiteral() {
        int col = column();
        StringBuilder sb = new StringBuilder();
        pos++; // skip opening "

        while (pos < source.length() && source.charAt(pos) != '"') {
            sb.append(source.charAt(pos));
            pos++;
        }

        pos++; // skip closing "
        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
    }

    
    private Token readCharLiteral() {
        int col = column();
        pos++; // skip opening '
        char value = source.charAt(pos);
        pos++; // skip the char
        pos++; // skip closing '
        return new Token(TokenType.CHAR_LITERAL, String.valueOf(value), line, col);
    }

    private Token readSymbol() {
        int col = column();

        // check two-character symbols first
        String two = source.substring(pos, Math.min(pos + 2, source.length()));
        if (SYMBOLS.containsKey(two)) {
            pos += 2;

            // %% means the rest of the line is a comment, hand off to readComment
            if (two.equals("%%")) {
                return readComment(col);
            }

            return new Token(SYMBOLS.get(two), two, line, col);
        }

        // fallback to single character
        String one = String.valueOf(source.charAt(pos));
        pos++;

        TokenType type = SYMBOLS.getOrDefault(one, TokenType.PUNCTUATION);
        return new Token(type, one, line, col);
    }

// col is passed in from readSymbol so the token reports %% position, not the text after it
    private Token readComment(int col) {
        StringBuilder sb = new StringBuilder();

        while (pos < source.length() && source.charAt(pos) != '\n') {
            sb.append(source.charAt(pos));
            pos++;
        }

        return new Token(TokenType.COMMENT, sb.toString().trim(), line, col);
    }

    // --- Helpers ---

    private void skipWhitespace() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '\n') {
                line++;
                lineStart = pos + 1;
                pos++;
            } else if (Character.isWhitespace(c)) {
                pos++;
            } else {
                break;
            }
        }
    }

    private int column() {
        return pos - lineStart + 1;
    }
}