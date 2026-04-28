package interpreter;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Lexer {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int lineStart = 0;

    // ================= RESERVED WORDS =================
    private static final Map<String, TokenType> RESERVED_WORDS = Map.ofEntries(
            Map.entry("START", TokenType.KEYWORD),
            Map.entry("END", TokenType.KEYWORD),
            Map.entry("SCRIPT", TokenType.KEYWORD),
            Map.entry("AREA", TokenType.KEYWORD),
            Map.entry("DECLARE", TokenType.KEYWORD),
            Map.entry("PRINT", TokenType.KEYWORD),

            Map.entry("STRING", TokenType.TYPE),
            Map.entry("INT", TokenType.TYPE),
            Map.entry("CHAR", TokenType.TYPE),
            Map.entry("BOOL", TokenType.TYPE),
            Map.entry("FLOAT", TokenType.TYPE),

            Map.entry("TRUE", TokenType.BOOL_LITERAL),
            Map.entry("FALSE", TokenType.BOOL_LITERAL)
    );

    // ================= SYMBOLS =================
    private static final Map<String, TokenType> SYMBOLS = Map.ofEntries(

            Map.entry(":=", TokenType.OPERATOR),
            Map.entry("=", TokenType.OPERATOR),
            Map.entry("&", TokenType.OPERATOR),
            Map.entry("$", TokenType.OPERATOR),

            Map.entry("+", TokenType.OPERATOR),
            Map.entry("-", TokenType.OPERATOR),
            Map.entry("*", TokenType.OPERATOR),
            Map.entry("/", TokenType.OPERATOR),
            Map.entry("%", TokenType.OPERATOR),

            Map.entry(">", TokenType.OPERATOR),
            Map.entry("<", TokenType.OPERATOR),
            Map.entry(">=", TokenType.OPERATOR),
            Map.entry("<=", TokenType.OPERATOR),
            Map.entry("==", TokenType.OPERATOR),
            Map.entry("<>", TokenType.OPERATOR),

            Map.entry(":", TokenType.PUNCTUATION),
            Map.entry(";", TokenType.PUNCTUATION),
            Map.entry("(", TokenType.PUNCTUATION),
            Map.entry(")", TokenType.PUNCTUATION),
            Map.entry("[", TokenType.PUNCTUATION),
            Map.entry("]", TokenType.PUNCTUATION),
            Map.entry("#", TokenType.PUNCTUATION),
            Map.entry(",", TokenType.PUNCTUATION),

            Map.entry("%%", TokenType.COMMENT)
    );

    // ================= CONSTRUCTOR =================
    public Lexer(String source) {
        this.source = normalizeQuotes(source);
    }

    // ================= NORMALIZE SMART QUOTES =================
    private String normalizeQuotes(String input) {
        return input
                .replace('“', '"')
                .replace('”', '"')
                .replace('‘', '\'')
                .replace('’', '\'');
    }

    // ================= TOKENIZE =================
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < source.length()) {
            skipWhitespace();
            if (pos >= source.length()) break;

            char c = source.charAt(pos);

            // STRING
            if (c == '"') {
                tokens.add(readStringLiteral());
            }

            // CHAR
            else if (c == '\'') {
                tokens.add(readCharLiteral());
            }

            // IDENTIFIER / KEYWORD
            else if (Character.isLetter(c) || c == '_') {
                tokens.add(readWord());
            }

            // NUMBER
            else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            }

            // SYMBOL
            else {
                tokens.add(readSymbol());
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF", line, column()));
        return tokens;
    }

    // ================= IDENTIFIER =================
    private Token readWord() {
        int col = column();
        StringBuilder sb = new StringBuilder();

        char first = source.charAt(pos);

        if (!(Character.isLetter(first) || first == '_')) {
            throw new RuntimeException("Invalid identifier start: " + first);
        }

        sb.append(first);
        pos++;

        while (pos < source.length()) {
            char c = source.charAt(pos);

            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                pos++;
            } else {
                break;
            }
        }

        String word = sb.toString();
        TokenType type = RESERVED_WORDS.getOrDefault(word, TokenType.IDENTIFIER);

        return new Token(type, word, line, col);
    }

    // ================= NUMBER =================
    private Token readNumber() {
        int col = column();
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false;

        while (pos < source.length()) {
            char c = source.charAt(pos);

            if (Character.isDigit(c)) {
                sb.append(c);
                pos++;
            } else if (c == '.' && !hasDecimal) {
                hasDecimal = true;
                sb.append(c);
                pos++;
            } else {
                break;
            }
        }

        TokenType type = hasDecimal ? TokenType.FLOAT_LITERAL : TokenType.INT_LITERAL;
        return new Token(type, sb.toString(), line, col);
    }

    // ================= STRING =================
    private Token readStringLiteral() {
        int col = column();
        StringBuilder sb = new StringBuilder();

        pos++; // skip opening "

        while (pos < source.length() && source.charAt(pos) != '"') {
            sb.append(source.charAt(pos));
            pos++;
        }

        if (pos >= source.length()) {
            throw new RuntimeException("Unterminated string at line " + line);
        }

        pos++; // skip closing "
        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
    }

    // ================= CHAR =================
    private Token readCharLiteral() {
        int col = column();

        pos++; // skip opening '

        if (pos >= source.length()) {
            throw new RuntimeException("Unterminated char at line " + line);
        }

        char value = source.charAt(pos);
        pos++;

        if (pos >= source.length() || source.charAt(pos) != '\'') {
            throw new RuntimeException("Unterminated char at line " + line);
        }

        pos++; // skip closing '

        return new Token(TokenType.CHAR_LITERAL, String.valueOf(value), line, col);
    }

    // ================= SYMBOLS =================
    private Token readSymbol() {
        int col = column();

        if (pos + 1 < source.length()) {
            String two = source.substring(pos, pos + 2);

            if (SYMBOLS.containsKey(two)) {
                pos += 2;

                if (two.equals("%%")) {
                    return readComment(col);
                }

                return new Token(SYMBOLS.get(two), two, line, col);
            }
        }

        String one = String.valueOf(source.charAt(pos));
        pos++;

        TokenType type = SYMBOLS.getOrDefault(one, TokenType.PUNCTUATION);
        return new Token(type, one, line, col);
    }

    // ================= COMMENT =================
    private Token readComment(int col) {
        StringBuilder sb = new StringBuilder();

        while (pos < source.length() && source.charAt(pos) != '\n') {
            sb.append(source.charAt(pos));
            pos++;
        }

        return new Token(TokenType.COMMENT, sb.toString().trim(), line, col);
    }

    // ================= WHITESPACE =================
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

    // ================= COLUMN =================
    private int column() {
        return pos - lineStart + 1;
    }
}