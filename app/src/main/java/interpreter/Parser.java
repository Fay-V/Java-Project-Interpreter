package interpreter;

import java.util.ArrayList;
import java.util.List;

import static interpreter.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        skipNewlines();
        consume(SCRIPT_AREA, "Expect 'SCRIPT AREA'.");
        skipNewlines();
        consume(START_SCRIPT, "Expect 'START SCRIPT'.");
        skipNewlines();

        // Declarations (only at top level)
        while (match(DECLARE)) {
            statements.add(declaration());
            consume(NEWLINE, "Expect newline after declaration.");
            skipNewlines();
        }

        // Executable statements
        while (!check(END_SCRIPT) && !isAtEnd()) {
            statements.add(statement());
            match(NEWLINE);   // replace consume with match
            skipNewlines();
        }

        consume(END_SCRIPT, "Expect 'END SCRIPT'.");
        // skipNewlines();
        // consume(EOF, "Expect end of file.");
        return statements;
    }

    private Stmt declaration() {
        Token type = consumeAnyOf("Expect data type.", INT, CHAR, BOOL, FLOAT);
        List<Token> names = new ArrayList<>();
        List<Expr> initializers = new ArrayList<>();
        do {
            Token name = consume(IDENTIFIER, "Expect variable name.");
            Expr initializer = null;
            if (match(EQUAL)) {
                initializer = expression();
            }
            names.add(name);
            initializers.add(initializer);
        } while (match(COMMA));
        return new Stmt.Declaration(type, names, initializers);
    }

    private Stmt statement() {
        if (match(PRINT_COLON)) return printStatement();
        if (match(SCAN_COLON)) return scanStatement();
        if (match(IF)) return ifStatement();
        if (match(FOR)) return forStatement();
        if (match(REPEAT)) return repeatStatement();
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr expr = expression();
        return new Stmt.Print(expr);
    }

    private Stmt scanStatement() {
        List<Token> variables = new ArrayList<>();
        do {
            variables.add(consume(IDENTIFIER, "Expect variable name."));
        } while (match(COMMA));
        return new Stmt.Scan(variables);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after IF.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        skipNewlines();
        consume(START_IF, "Expect 'START IF'.");
        consumeNewlineOrSkip("Expect newline after START IF.");

        Stmt thenBranch = block(END_IF);
        while (match(NEWLINE)) {}   // consume all newlines

        List<Stmt.If.ElseIf> elseIfs = new ArrayList<>();
        while (match(ELSE)) {
            while (match(NEWLINE)) {}   // consume any newlines after ELSE
            if (check(IF)) {
                consume(IF, "Expect 'IF' after ELSE.");
                consume(LEFT_PAREN, "Expect '(' after ELSE IF.");
                Expr elifCondition = expression();
                consume(RIGHT_PAREN, "Expect ')' after condition.");
                skipNewlines();
                consume(START_IF, "Expect 'START IF'.");
                consumeNewlineOrSkip("Expect newline after START IF.");
                Stmt elifBranch = block(END_IF);
                while (match(NEWLINE)) {}   // consume newlines after block
                elseIfs.add(new Stmt.If.ElseIf(elifCondition, elifBranch));
            } else {
                consume(START_IF, "Expect 'START IF' for ELSE.");
                consumeNewlineOrSkip("Expect newline after START IF.");
                Stmt elseBranch = block(END_IF);
                while (match(NEWLINE)) {}   // consume newlines after block
                return new Stmt.If(condition, thenBranch, elseIfs, elseBranch);
            }
        }
        return new Stmt.If(condition, thenBranch, elseIfs, null);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after FOR.");
        Expr initializer = null;
        if (!check(COMMA)) {
            initializer = expression();
        }
        consume(COMMA, "Expect ',' after initializer.");
        Expr condition = null;
        if (!check(COMMA)) {
            condition = expression();
        }
        consume(COMMA, "Expect ',' after condition.");
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after FOR clauses.");
        skipNewlines();
        consume(START_FOR, "Expect 'START FOR'.");
        consumeNewlineOrSkip("Expect newline after START FOR.");
        Stmt body = block(END_FOR);
        return new Stmt.For(initializer, condition, increment, body);
    }

    private Stmt repeatStatement() {
        // REPEAT already matched
        consume(WHEN, "Expect 'WHEN' after REPEAT.");
        consume(LEFT_PAREN, "Expect '(' after WHEN.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        skipNewlines();
        consume(START_REPEAT, "Expect 'START REPEAT'.");
        consumeNewlineOrSkip("Expect newline after START REPEAT.");
        Stmt body = block(END_REPEAT);
        return new Stmt.Repeat(condition, body);
    }

    private Stmt block(TokenType endType) {
        List<Stmt> statements = new ArrayList<>();
        while (!check(endType) && !isAtEnd()) {
            statements.add(statement());
            consume(NEWLINE, "Expect newline after statement.");
            skipNewlines();
        }
        consume(endType, "Expect '" + endType.name() + "'.");
        return new Stmt.Block(statements);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    // ---------- Expressions ----------
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // right-associative
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            throw error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(EQUAL_EQUAL, NOT_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = concatenation();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = concatenation();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr concatenation() {
        Expr expr = addition();
        while (match(AMPERSAND)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();
        while (match(STAR, SLASH, PERCENT)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(NOT, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(NIL)) return new Expr.Literal(previous().literal);
        if (match(NUMBER, STRING, CHAR_LITERAL)) return new Expr.Literal(previous().literal);
        if (match(DOLLAR, ESCAPE)) return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    // ---------- Helpers ----------
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private Token consumeAnyOf(String message, TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) return advance();
        }
        throw error(peek(), message);
    }

    private void consumeNewlineOrSkip(String message) {
        if (check(NEWLINE)) {
            advance();
        } else {
            throw error(peek(), message);
        }
    }

    private void skipNewlines() {
        while (match(NEWLINE)) { }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        App.error(token, message);
        return new ParseError();
    }

    private static class ParseError extends RuntimeException {}
}