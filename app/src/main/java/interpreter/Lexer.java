package interpreter;

import java.util.*;

import static interpreter.TokenType.*;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;
    private static final Map<Character, TokenType> simpleTokens;
    private static final Map<Character, CharCategory> charCategories;

    private enum CharCategory {
        OPERATOR, LITERAL, WHITESPACE, SPECIAL
    }
    
    static {
        keywords = new HashMap<>();
        keywords.put("DECLARE", DECLARE);
        keywords.put("INT", INT);
        keywords.put("CHAR", CHAR);
        keywords.put("BOOL", BOOL);
        keywords.put("FLOAT", FLOAT);
        keywords.put("STRING", STRING);
        keywords.put("IF", IF);
        keywords.put("ELSE", ELSE);
        keywords.put("FOR", FOR);
        keywords.put("REPEAT", REPEAT);
        keywords.put("WHEN", WHEN);
        keywords.put("AND", AND);
        keywords.put("OR", OR);
        keywords.put("NOT", NOT);
        keywords.put("SCRIPT", SCRIPT);
        keywords.put("AREA", AREA);
        keywords.put("START", START);
        keywords.put("END", END);
        keywords.put("PRINT", PRINT);
        keywords.put("SCAN", SCAN);
        keywords.put("SWITCH", SWITCH);
        keywords.put("CASE", CASE);
        keywords.put("DEFAULT", DEFAULT);

        simpleTokens = new HashMap<>();
        simpleTokens.put('(', LEFT_PAREN);
        simpleTokens.put(')', RIGHT_PAREN);
        simpleTokens.put(',', COMMA);
        simpleTokens.put(';', SEMICOLON);
        simpleTokens.put('&', AMPERSAND);
        simpleTokens.put('+', PLUS);
        simpleTokens.put('-', MINUS);
        simpleTokens.put('*', STAR);
        simpleTokens.put('/', SLASH);

        charCategories = new HashMap<>();
        charCategories.put('%', CharCategory.OPERATOR);
        charCategories.put('=', CharCategory.OPERATOR);
        charCategories.put('<', CharCategory.OPERATOR);
        charCategories.put('>', CharCategory.OPERATOR);

        charCategories.put('\'', CharCategory.LITERAL);
        charCategories.put('‘', CharCategory.LITERAL);
        charCategories.put('’', CharCategory.LITERAL);
        charCategories.put('"', CharCategory.LITERAL);
        charCategories.put('“', CharCategory.LITERAL);
        charCategories.put('”', CharCategory.LITERAL);
        charCategories.put('[', CharCategory.LITERAL);

        charCategories.put('\n', CharCategory.WHITESPACE);
        charCategories.put(' ', CharCategory.WHITESPACE);
        charCategories.put('\r', CharCategory.WHITESPACE);
        charCategories.put('\t', CharCategory.WHITESPACE);

        charCategories.put('$', CharCategory.SPECIAL);
        charCategories.put(']', CharCategory.SPECIAL);
    }

    Lexer(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();

        if (simpleTokens.containsKey(c)) {
            addToken(simpleTokens.get(c));
            return;
        }

        CharCategory category = charCategories.get(c);
        if (category != null) {
            switch (category) {
                case OPERATOR:   scanOperator(c);   break;
                case LITERAL:    scanLiteral(c);    break;
                case WHITESPACE: scanWhitespace(c); break;
                case SPECIAL:    scanSpecial(c);    break;
            }
            return;
        }

        if (isDigit(c)) {
            number();
        } else if (isAlpha(c)) {
            identifier();
        } else if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)){
            App.error(line, "Unexpected character '" + c + " U+"+ Integer.toHexString(c) +"'.");
        }
    }

    private void scanOperator(char c) {
        switch (c) {
            case '%':
                if (match('%')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(PERCENT);
                }
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                if (match('=')) addToken(LESS_EQUAL);
                else if (match('>')) addToken(NOT_EQUAL);
                else addToken(LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
        }
    }

    private void scanLiteral(char c) {
        if (isSingleQuote(c)) {
            charLiteral();
        } else if (isDoubleQuote(c)) {
            string();
        } else if (c == '[') {
            escapeSequence();
        }
    }

    private void scanWhitespace(char c) {
        if (c == '\n') {
            line++;
            addToken(NEWLINE);
        }
    }

    private void scanSpecial(char c) {
        if (c == '$') {
            addToken(DOLLAR, '\n');
        } else if (c == ']') {
            App.error(line, "Unexpected ']' outside string.");
        }
    }

    private void charLiteral() {
        if (isAtEnd() || isSingleQuote(peek())) {
            App.error(line, "Empty char literal.");
            return;
        }
        char ch = advance();
        if (!isSingleQuote(peek())) {
            App.error(line, "Unterminated char literal.");
        } else {
            advance();
        }
        addToken(CHAR_LITERAL, ch);
    }

    private boolean isSingleQuote(char c) {
        return c == '\'' || c == '‘' || c == '’';
    }

    private void string() {
        StringBuilder sb = new StringBuilder();
        while (!isDoubleQuote(peek()) && !isAtEnd()) {
            if (peek() == '[') {
                advance(); // consume '['
                char escaped = getEscapedChar();
                if (escaped != '\0') sb.append(escaped);
                continue;
            }
            
            char code = peek();
            switch (code){
                case '#': case '[': case ']':
                case '$': case '&': case '"': case '\'':
                    App.error(line, "Escape code '" + code + "' is inside string");
                    advance(); // advance to avoid infinite loop
                    break;
                default:
                    sb.append(advance());
            }
        }
        if (isAtEnd()) { App.error(line, "Unterminated string."); return; }
        advance(); // closing quote
        String content = sb.toString();
        if (content.equals("TRUE")) {
            addToken(BOOL_LITERAL, true);
        } else if (content.equals("FALSE")) {
            addToken(BOOL_LITERAL, false);
        } else {
            addToken(STRING, content);
        }
    }

    private boolean isDoubleQuote(char c) {
        return c == '"' || c == '“' || c == '”';
    }

    private void escapeSequence() {
        char escaped = getEscapedChar();
        if (escaped != '\0') {
            addToken(ESCAPE, escaped);
        }
    }

    private char getEscapedChar() {
        if (isAtEnd()) {
            App.error(line, "Unterminated escape.");
            return '\0';
        }
        char code = advance();
        if (peek() != ']') {
            App.error(line, "Expect ']' after escape.");
            return '\0';
        }
        advance(); // consume closing ']'
        return code;
    }

    private void number() {
        while (isDigit(peek())) advance();

        boolean isFloat = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true;
            do advance();
            while (isDigit(peek()));
        }

        String text = source.substring(start, current);
        if (isFloat) {
            addToken(NUMBER, Double.parseDouble(text));
        } else {
            addToken(NUMBER, Integer.parseInt(text));
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);

        switch (text) {
            case "SCRIPT": {
                int savedCurrent = current;
                int savedLine = line;
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("AREA")) {
                    addToken(SCRIPT_AREA);
                    return;
                }
                current = savedCurrent;
                line = savedLine;
                break;
            }
            case "START": {
                int savedCurrent = current;
                int savedLine = line;
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("SCRIPT")) {
                    addToken(START_SCRIPT);
                } else if (matchNextIdentifier("IF")) {
                    addToken(START_IF);
                } else if (matchNextIdentifier("FOR")) {
                    addToken(START_FOR);
                } else if (matchNextIdentifier("REPEAT")) {
                    addToken(START_REPEAT);
                } else if (matchNextIdentifier("SWITCH")) {
                    addToken(START_SWITCH);
                } else {
                    current = savedCurrent;
                    line = savedLine;
                    break;
                }
                return;
            }
            case "END": {
                int savedCurrent = current;
                int savedLine = line;
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("SCRIPT")) {
                    addToken(END_SCRIPT);
                } else if (matchNextIdentifier("IF")) {
                    addToken(END_IF);
                } else if (matchNextIdentifier("FOR")) {
                    addToken(END_FOR);
                } else if (matchNextIdentifier("REPEAT")) {
                    addToken(END_REPEAT);
                } else if (matchNextIdentifier("SWITCH")) {
                    addToken(END_SWITCH);
                } else {
                    current = savedCurrent;
                    line = savedLine;
                    break;
                }
                return;
            }
            case "PRINT": {
                if (peek() == ':') {
                    advance();
                    addToken(PRINT_COLON);
                    return;
                }
                break;
            }
            case "SCAN": {
                if (peek() == ':') {
                    advance();
                    addToken(SCAN_COLON);
                    return;
                }
                break;
            }
        }

        TokenType type = keywords.get(text);
        addToken(Objects.requireNonNullElse(type, IDENTIFIER));
    }

    private void skipWhitespaceAndNewlines() {
        while (current < source.length()) {
            char c = source.charAt(current);
            if (c == ' ' || c == '\t' || c == '\r') {
                current++;
            } else if (c == '\n') {
                current++;
                line++;
            } else {
                break;
            }
        }
    }

    private boolean matchNextIdentifier(String word) {
        if (current >= source.length()) return false;
        char c = source.charAt(current);
        if (!isAlpha(c)) return false;
        int startPos = current;
        while (current < source.length() && isAlphaNumeric(source.charAt(current))) {
            current++;
        }
        String found = source.substring(startPos, current);
        if (found.equals(word)) {
            return true;
        } else {
            current = startPos;
            return false;
        }
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
