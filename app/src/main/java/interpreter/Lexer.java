package interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static interpreter.TokenType.*;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("DECLARE", DECLARE);
        keywords.put("INT", INT);
        keywords.put("CHAR", CHAR);
        keywords.put("BOOL", BOOL);
        keywords.put("FLOAT", FLOAT);
        keywords.put("IF", IF);
        keywords.put("ELSE", ELSE);
        keywords.put("FOR", FOR);
        keywords.put("REPEAT", REPEAT);
        keywords.put("WHEN", WHEN);
        keywords.put("AND", AND);
        keywords.put("OR", OR);
        keywords.put("NOT", NOT);
        keywords.put("NIL", NIL);
        // TRUE and FALSE are not keywords; they are string literals "TRUE"/"FALSE"
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
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case ',': addToken(COMMA); break;
            case ';': addToken(SEMICOLON); break;
            case '&': addToken(AMPERSAND); break;
            case '+': addToken(PLUS); break;
            case '-': addToken(MINUS); break;
            case '*': addToken(STAR); break;
            case '/': addToken(SLASH); break;
            case '%':
                if (match('%')) {
                    // comment until end of line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(PERCENT);
                }
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '!':
                addToken(match('=') ? NOT_EQUAL : BANG);
                break;
            case '<':
                if (match('=')) addToken(LESS_EQUAL);
                else if (match('>')) addToken(NOT_EQUAL);
                else addToken(LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // Single quotes (straight and smart/curly)
            case '\'': case '\u2018': case '\u2019':  // ' ' '
                charLiteral();
                break;
            // Double quotes (straight and smart/curly)
            case '"': case '\u201C': case '\u201D':   // " " "
                string();
                break;
            case '[':
                escapeSequence();
                break;
            case ']':
                App.error(line, "Unexpected ']' outside string.");
                break;
            case '$':
                addToken(DOLLAR, '\n');
                break;
            case '\n':
                line++;
                addToken(NEWLINE);
                break;
            case ' ':
            case '\r':
            case '\t':
                // ignore whitespace
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    App.error(line, "Unexpected character '" + c + "'.");
                }
                break;
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
        return c == '\'' || c == '\u2018' || c == '\u2019';
    }

    private void string() {
        StringBuilder sb = new StringBuilder();
        while (!isDoubleQuote(peek()) && !isAtEnd()) {
            if (peek() == '\n') line++;
            if (peek() == '$') {
                advance();
                sb.append('\n');
            } else if (peek() == '[') {
                advance();
                if (peek() == '#') {
                    advance();
                    if (peek() == ']') {
                        advance();
                        sb.append('#');
                    } else {
                        App.error(line, "Expect ']' after '[#' escape.");
                    }
                } else if (peek() == '[') {
                    advance();
                    if (peek() == ']') {
                        advance();
                        sb.append('[');
                    } else {
                        App.error(line, "Expect ']' after '[[' escape.");
                    }
                } else if (peek() == ']') {
                    advance();
                    if (peek() == ']') {
                        advance();
                        sb.append(']');
                    } else {
                        App.error(line, "Expect ']' after '[]' escape.");
                    }
                } else {
                    App.error(line, "Invalid escape sequence inside string.");
                }
            } else {
                sb.append(advance());
            }
        }
        if (isAtEnd()) {
            App.error(line, "Unterminated string.");
            return;
        }
        advance(); // closing quote
        addToken(STRING, sb.toString());
    }

    private boolean isDoubleQuote(char c) {
        return c == '"' || c == '\u201C' || c == '\u201D';
    }

    private void escapeSequence() {
        // current is after '['
        if (isAtEnd()) { App.error(line, "Unterminated escape."); return; }
        char code = advance();
        Character literal = null;
        switch (code) {
            case '#': literal = '#'; break;
            case '[': literal = '['; break;
            case ']': literal = ']'; break;
            default:
                App.error(line, "Invalid escape code '" + code + "'.");
                return;
        }
        if (peek() != ']') {
            App.error(line, "Expect ']' after escape.");
            return;
        }
        advance(); // consume closing ']'
        addToken(ESCAPE, literal);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);

        switch (text) {
            case "SCRIPT": {
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("AREA")) {
                    addToken(SCRIPT_AREA);
                } else {
                    App.error(line, "Expected 'AREA' after 'SCRIPT'.");
                    addToken(SCRIPT_AREA);
                }
                return;
            }
            case "START": {
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("SCRIPT")) {
                    addToken(START_SCRIPT);
                } else if (matchNextIdentifier("IF")) {
                    addToken(START_IF);
                } else if (matchNextIdentifier("FOR")) {
                    addToken(START_FOR);
                } else if (matchNextIdentifier("REPEAT")) {
                    addToken(START_REPEAT);
                } else {
                    App.error(line, "Expected 'SCRIPT', 'IF', 'FOR', or 'REPEAT' after 'START'.");
                    addToken(START_SCRIPT);
                }
                return;
            }
            case "END": {
                skipWhitespaceAndNewlines();
                if (matchNextIdentifier("SCRIPT")) {
                    addToken(END_SCRIPT);
                } else if (matchNextIdentifier("IF")) {
                    addToken(END_IF);
                } else if (matchNextIdentifier("FOR")) {
                    addToken(END_FOR);
                } else if (matchNextIdentifier("REPEAT")) {
                    addToken(END_REPEAT);
                } else {
                    App.error(line, "Expected 'SCRIPT', 'IF', 'FOR', or 'REPEAT' after 'END'.");
                    addToken(END_SCRIPT);
                }
                return;
            }
            case "PRINT": {
                if (peek() == ':') {
                    advance();
                    addToken(PRINT_COLON);
                } else {
                    App.error(line, "Expected ':' after 'PRINT'.");
                    addToken(PRINT_COLON);
                }
                return;
            }
            case "SCAN": {
                if (peek() == ':') {
                    advance();
                    addToken(SCAN_COLON);
                } else {
                    App.error(line, "Expected ':' after 'SCAN'.");
                    addToken(SCAN_COLON);
                }
                return;
            }
        }

        // Single‑word keyword or identifier (case‑sensitive)
        TokenType type = keywords.get(text);
        if (type != null) {
            addToken(type);
        } else {
            addToken(IDENTIFIER);
        }
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
        int start = current;
        while (current < source.length() && isAlphaNumeric(source.charAt(current))) {
            current++;
        }
        String found = source.substring(start, current);
        if (found.equals(word)) {
            return true;
        } else {
            current = start;
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