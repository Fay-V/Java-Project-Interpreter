package interpreter;

import nodes.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    List<Token> tokens;
    int current = 0;

    // NEW: track line of last parsed statement
    private int lastStatementLine = -1;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ================= PROGRAM =================

    public ProgramNode parseProgram() {

        expect(TokenType.KEYWORD, "SCRIPT");
        expect(TokenType.KEYWORD, "AREA");
        expect(TokenType.KEYWORD, "START");
        expect(TokenType.KEYWORD, "SCRIPT");

        ProgramNode program = new ProgramNode();

        while (!check(TokenType.KEYWORD, "END")) {
            program.addStatement(parseStatement());
        }

        expect(TokenType.KEYWORD, "END");
        expect(TokenType.KEYWORD, "SCRIPT");
        expect(TokenType.EOF);

        return program;
    }

    // ================= STATEMENT RULE =================

    public StatementNode parseStatement() {

        Token token = peek();

        // 🔥 ENFORCE: only 1 statement per line
        if (token.line == lastStatementLine) {
            throw new RuntimeException(
                    "Only one statement allowed per line at line " + token.line
            );
        }

        lastStatementLine = token.line;

        if (token.value.equals("DECLARE")) {
            return parseDeclaration();
        }

        if (token.value.equals("PRINT")) {
            return parsePrint();
        }

        if (token.type == TokenType.IDENTIFIER) {
            return parseAssignment();
        }

        throw new RuntimeException("Invalid statement at line " + token.line);
    }

    // ================= DECLARATION =================

    public DeclarationNode parseDeclaration() {

        expect(TokenType.KEYWORD, "DECLARE");

        Token typeToken = peek();
        expect(TokenType.TYPE);

        String type = typeToken.value;
        List<VariableNode> variables = new ArrayList<>();

        variables.add(parseVariableNode());

        while (check(TokenType.PUNCTUATION, ",")) {
            expect(TokenType.PUNCTUATION, ",");
            variables.add(parseVariableNode());
        }

        return new DeclarationNode(type, variables);
    }

    public VariableNode parseVariableNode() {

        Token token = peek();
        expect(TokenType.IDENTIFIER);

        ExpressionNode expression = null;

        if (check(TokenType.OPERATOR, "=")) {
            expect(TokenType.OPERATOR, "=");
            expression = parseAssignmentExpression();
        }

        return new VariableNode(token.value, expression);
    }

    // ================= ASSIGNMENT =================

    public AssignmentNode parseAssignment() {

        Token identifier = peek();
        expect(TokenType.IDENTIFIER);

        expect(TokenType.OPERATOR, "=");

        ExpressionNode value = parseAssignmentExpression();

        return new AssignmentNode(identifier.value, value);
    }

    public ExpressionNode parseAssignmentExpression() {

        ExpressionNode left = parseExpression();

        if (check(TokenType.OPERATOR, "=")) {
            expect(TokenType.OPERATOR, "=");

            ExpressionNode right = parseAssignmentExpression();

            return new AssignmentExpressionNode(
                    ((IdentifierNode) left).name,
                    right
            );
        }

        return left;
    }

    // ================= PRINT =================

    public PrintNode parsePrint() {

        expect(TokenType.KEYWORD, "PRINT");
        expect(TokenType.PUNCTUATION, ":");

        ExpressionNode expression = parseExpression();

        return new PrintNode(expression);
    }

    // ================= EXPRESSIONS =================

    public ExpressionNode parseExpression() {

        ExpressionNode left = parsePrimary();

        while (check(TokenType.OPERATOR, "&")) {

            String op = peek().value;
            expect(TokenType.OPERATOR, "&");

            ExpressionNode right = parsePrimary();

            left = new BinaryExpressionNode(left, op, right);
        }

        return left;
    }

    public ExpressionNode parsePrimary() {

        Token token = peek();

        // identifier
        if (token.type == TokenType.IDENTIFIER) {
            expect(TokenType.IDENTIFIER);
            return new IdentifierNode(token.value);
        }

        // literals
        if (token.type == TokenType.INT_LITERAL ||
                token.type == TokenType.FLOAT_LITERAL ||
                token.type == TokenType.STRING_LITERAL ||
                token.type == TokenType.CHAR_LITERAL ||
                token.type == TokenType.BOOL_LITERAL) {

            expect(token.type);
            return new LiteralNode(token.value);
        }

        // $ newline
        if (token.type == TokenType.OPERATOR && token.value.equals("$")) {
            expect(TokenType.OPERATOR, "$");
            return new LiteralNode("\n");
        }

        // ================= ESCAPES =================

        if (check(TokenType.PUNCTUATION, "[")) {

            expect(TokenType.PUNCTUATION, "[");

            Token middle = peek();

            // [ # ]
            if (middle.value.equals("#")) {
                expect(TokenType.PUNCTUATION, "#");
                expect(TokenType.PUNCTUATION, "]");
                return new LiteralNode("#");
            }

            // [ [ ]
            if (middle.value.equals("[")) {
                expect(TokenType.PUNCTUATION, "[");
                expect(TokenType.PUNCTUATION, "]");
                return new LiteralNode("[");
            }

            // [ ] ]
            if (middle.value.equals("]")) {
                expect(TokenType.PUNCTUATION, "]");
                expect(TokenType.PUNCTUATION, "]");
                return new LiteralNode("]");
            }

            throw new RuntimeException("Invalid escape sequence at line " + token.line);
        }

        throw new RuntimeException(
                "Unexpected token " + token.value + " at line " + token.line
        );
    }

    // ================= HELPERS =================

    private void expect(TokenType type) {
        Token token = peek();

        if (token.type != type) {
            throw new RuntimeException("Expected " + type + " at line " + token.line);
        }

        current++;
        skipComments();
    }

    private void expect(TokenType type, String value) {
        Token token = peek();

        if (token.type != type || !token.value.equals(value)) {
            throw new RuntimeException(
                    "Expected " + value + " at line " + token.line +
                            " but found " + token.value
            );
        }

        current++;
        skipComments();
    }

    private Token peek() {
        skipComments();
        return tokens.get(current);
    }

    private boolean check(TokenType type, String value) {
        Token t = peek();
        return t.type == type && t.value.equals(value);
    }

    private void skipComments() {
        while (current < tokens.size() &&
                tokens.get(current).type == TokenType.COMMENT) {
            current++;
        }
    }
}