package interpreter;

import static interpreter.TokenType.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        skipNewlines();
        consume(SCRIPT_AREA, "Expect 'SCRIPT AREA'.");
        skipNewlines();
        consume(START_SCRIPT, "Expect 'START SCRIPT'.");
        skipNewlines();

        // Executable statements (including declarations)
        while (!check(END_SCRIPT) && !isAtEnd()) {
            statements.add(statement());
            if (!check(END_SCRIPT)) {
                consume(NEWLINE, "Expect newline after statement.");
            }
            skipNewlines();
        }

        consume(END_SCRIPT, "Expect 'END SCRIPT'.");
        skipNewlines();
        if (!isAtEnd()) {
            throw error(tokens.get(current), "Expected end of file");
        }

        return statements;
    }

    private Statement declaration() {
        Token type = null;
        TokenType[] tokenTypes = {INT, CHAR, BOOL, FLOAT, STRING};
        for (TokenType tokenType : tokenTypes) {
            if (check(tokenType)) type = advance();
        }
        if (type == null) {
            throw error(peek(), "Expect data type in declaration.");
        }
        List<Token> names = new ArrayList<>();
        List<Expression> initializers = new ArrayList<>();
        do {
            Token name = consume(IDENTIFIER, "Expect variable name.");
            Expression initializer = null;
            if (match(EQUAL)) {
                initializer = expression();
            }
            names.add(name);
            initializers.add(initializer);
        } while (match(COMMA));
        return new Statement.Declaration(type, names, initializers);
    }

    private Statement statement() {
        if (match(DECLARE)) return declaration();
        if (match(PRINT_COLON)) return printStatement();
        if (match(SCAN_COLON)) return scanStatement();
        if (match(IF)) return ifStatement();
        if (match(FOR)) return forStatement();
        if (match(REPEAT)) return repeatStatement();
        if (match(SWITCH)) return switchStatement(); 
        return expressionStatement();
    }

    private Statement printStatement() {
        Expression expr = expression();
        return new Statement.Print(expr);
    }

    private Statement scanStatement() {
        List<Token> variables = new ArrayList<>();
        do {
            variables.add(consume(IDENTIFIER, "Expect variable name."));
        } while (match(COMMA));
        return new Statement.Scan(variables);
    }

    private Statement ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after IF.");
        Expression condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        skipNewlines();
        consume(START_IF, "Expect 'START IF'.");
        consumeNewlineOrSkip("Expect newline after START IF.");

        Statement thenBranch = block(END_IF);

        List<Statement.If.ElseIf> elseIfs = new ArrayList<>();
        while (checkNextIgnoringNewlines(ELSE)) {
            skipNewlines();
            consume(ELSE, "Expect 'ELSE'.");
            skipNewlines();
            if (match(IF)) {
                consume(LEFT_PAREN, "Expect '(' after ELSE IF.");
                Expression elifCondition = expression();
                consume(RIGHT_PAREN, "Expect ')' after condition.");
                skipNewlines();
                consume(START_IF, "Expect 'START IF'.");
                consumeNewlineOrSkip("Expect newline after START IF.");
                Statement elifBranch = block(END_IF);
                elseIfs.add(new Statement.If.ElseIf(elifCondition, elifBranch));
            } else {
                consume(START_IF, "Expect 'START IF' for ELSE.");
                consumeNewlineOrSkip("Expect newline after START IF.");
                Statement elseBranch = block(END_IF);
                return new Statement.If(condition, thenBranch, elseIfs, elseBranch);
            }
        }
        return new Statement.If(condition, thenBranch, elseIfs, null);
    }

    private boolean checkNextIgnoringNewlines(TokenType type) {
        int temp = current;
        while (temp < tokens.size() && tokens.get(temp).type == NEWLINE) {
            temp++;
        }
        if (temp >= tokens.size()) return false;
        return tokens.get(temp).type == type;
    }

    private Statement forStatement() {
        consume(LEFT_PAREN, "Expect '(' after FOR.");
        Expression initializer = null;
        if (!check(COMMA)) {
            initializer = expression();
        }
        consume(COMMA, "Expect ',' after initializer.");
        Expression condition = null;
        if (!check(COMMA)) {
            condition = expression();
        }
        consume(COMMA, "Expect ',' after condition.");
        Expression increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after FOR clauses.");
        skipNewlines();
        consume(START_FOR, "Expect 'START FOR'.");
        consumeNewlineOrSkip("Expect newline after START FOR.");
        Statement body = block(END_FOR);
        return new Statement.For(initializer, condition, increment, body);
    }

    private Statement repeatStatement() {
        consume(WHEN, "Expect 'WHEN' after REPEAT.");
        consume(LEFT_PAREN, "Expect '(' after WHEN.");
        Expression condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        skipNewlines();
        consume(START_REPEAT, "Expect 'START REPEAT'.");
        consumeNewlineOrSkip("Expect newline after START REPEAT.");
        Statement body = block(END_REPEAT);
        return new Statement.Repeat(condition, body);
    }

    private Statement switchStatement() {
        consume(LEFT_PAREN, "Expect '(' after SWITCH.");
        Expression expression = expression();
        consume(RIGHT_PAREN, "Expect ')' after expression.");
        skipNewlines();
        consume(START_SWITCH, "Expect 'START SWITCH'.");
        consumeNewlineOrSkip("Expect newline after START SWITCH.");

        List<Statement.Switch.Case> cases = new ArrayList<>();
        Statement defaultBody = null;

        while (!check(END_SWITCH) && !isAtEnd()) {
            if (match(CASE)) {
                Expression label = expression();
                consume(NEWLINE, "Expect newline after case label.");
                skipNewlines();
                Statement body = caseBlock();
                cases.add(new Statement.Switch.Case(label, body));
            } else if (match(DEFAULT)) {
                consume(NEWLINE, "Expect newline after DEFAULT.");
                defaultBody = caseBlock();
                break;
            } else {
                throw error(peek(), "Expect 'CASE', 'DEFAULT', or 'END SWITCH'.");
            }
            skipNewlines();
        }

        consume(END_SWITCH, "Expect 'END SWITCH'.");
        return new Statement.Switch(expression, cases, defaultBody);
    }

    private Statement block(TokenType endType) {
        List<Statement> statements = new ArrayList<>();
        while (!check(endType) && !isAtEnd()) {
            statements.add(statement());
            if (!check(endType)) {
                consume(NEWLINE, "Expect newline after statement.");
            }
            skipNewlines();
        }
        // Allow optional newlines before the closing token
        skipNewlines();
        consume(endType, "Expect '" + endType.name() + "'.");
        return new Statement.Block(statements);
    }

    private Statement expressionStatement() {
        Expression expr = expression();
        return new Statement.Expression(expr);
    }

    // ---------- Expressions ----------
    private Expression expression() {
        return assignment();
    }

    private Expression assignment() {
        Expression expr = or();
        if (match(EQUAL)) {
            Token equals = previous();
            Expression value = assignment(); // right-associative
            if (expr instanceof Expression.Variable) {
                Token name = ((Expression.Variable) expr).name;
                return new Expression.Assign(name, value);
            }
            throw error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expression or() {
        Expression expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expression right = and();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression and() {
        Expression expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expression right = equality();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression equality() {
        Expression expr = comparison();
        while (match(EQUAL_EQUAL, NOT_EQUAL)) {
            Token operator = previous();
            Expression right = comparison();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression comparison() {
        Expression expr = concatenation();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expression right = concatenation();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression concatenation() {
        Expression expr = addition();
        while (match(AMPERSAND)) {
            Token operator = previous();
            Expression right = addition();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression addition() {
        Expression expr = multiplication();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expression right = multiplication();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression multiplication() {
        Expression expr = unary();
        while (match(STAR, SLASH, PERCENT)) {
            Token operator = previous();
            Expression right = unary();
            expr = new Expression.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expression unary() {
        if (match(NOT, MINUS, PLUS)) {
            Token operator = previous();
            Expression right = unary();
            return new Expression.Unary(operator, right);
        }
        return primary();
    }

    private Expression primary() {
        if (match(NUMBER, STRING, CHAR_LITERAL, BOOL_LITERAL)) return new Expression.Literal(
                previous().literal
        );
        if (match(DOLLAR, ESCAPE)) return new Expression.Literal(previous().literal);
        if (match(IDENTIFIER)) return new Expression.Variable(previous());
        if (match(LEFT_PAREN)) {
            Expression expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expression.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    // ---------- Helpers ----------
    private Token consume(TokenType type, String message) {
        if (type == EOF) {
            if (isAtEnd()) return advance();
            throw error(peek(), message);
        }
        if (check(type)) return advance();
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
        while (match(NEWLINE)) {
        }
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

    private static class ParseError extends RuntimeException {
    }

    private Statement caseBlock() {
        List<Statement> statements = new ArrayList<>();
        while (!check(CASE) && !check(DEFAULT) && !check(END_SWITCH) && !isAtEnd()) {
            statements.add(statement());
            consume(NEWLINE, "Expect newline after statement.");
            skipNewlines();
        }
        return new Statement.Block(statements);
    }
}
