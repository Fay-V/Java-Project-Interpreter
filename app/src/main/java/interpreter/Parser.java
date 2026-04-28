package interpreter;

import nodes.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    List<Token> tokens;
    int current = 0;
    public Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    public ProgramNode parseProgram() {

        // 1. Expect SCRIPT AREA START SCRIPT
        expect(TokenType.KEYWORD, "SCRIPT");
        expect(TokenType.KEYWORD, "AREA");
        expect(TokenType.KEYWORD, "START");
        expect(TokenType.KEYWORD, "SCRIPT");

        // 2. Create program node
        ProgramNode program = new ProgramNode();

        // 3. Parse statements until END SCRIPT
        while (!check(TokenType.KEYWORD, "END")) {
            program.addStatement(parseStatement());
        }

        // 4. Consume END SCRIPT
        expect(TokenType.KEYWORD, "END");
        expect(TokenType.KEYWORD, "SCRIPT");
        expect(TokenType.EOF);

        return program;
    }

    public StatementNode parseStatement(){
        Token token = peek();

        if(token.value.equals("DECLARE")){
            return parseDeclaration();
        } else if(token.value.equals("PRINT")){
            return parsePrint();
        } else if(token.type == TokenType.IDENTIFIER){
            return parseAssignment();
        }

        throw new RuntimeException("Invalid statement at line " + token.line);
    }

    public DeclarationNode parseDeclaration(){
        expect(TokenType.KEYWORD, "DECLARE");

        Token typeToken = peek();
        expect(TokenType.TYPE);

        String type = typeToken.value;
        List<VariableNode> variables = new ArrayList<>();

        // at least one variable
        variables.add(parseVariableNode());

        // handle commas
        while (check(TokenType.PUNCTUATION, ",")) {
            expect(TokenType.PUNCTUATION, ",");
            variables.add(parseVariableNode());
        }

        return new DeclarationNode(type, variables);
    }

    public VariableNode parseVariableNode(){
        Token token = peek();
        expect(TokenType.IDENTIFIER);

        ExpressionNode expression = null;

        if (check(TokenType.OPERATOR, "=")) {
            expect(TokenType.OPERATOR, "=");
            expression = parseAssignmentExpression();
        }

        return new VariableNode(token.value, expression);
    }

    public AssignmentNode parseAssignment() {

        Token identifier = peek();
        expect(TokenType.IDENTIFIER);

        expect(TokenType.OPERATOR, "=");

        ExpressionNode value = parseAssignmentExpression();

        return new AssignmentNode(identifier.value, value);
    }

    public ExpressionNode parseAssignmentExpression() {

        ExpressionNode left = parseExpression();

        // handle chaining: y = 4
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

    public PrintNode parsePrint() {
        expect(TokenType.KEYWORD, "PRINT");
        expect(TokenType.PUNCTUATION, ":");

        ExpressionNode expression = parseExpression();

        return new PrintNode(expression);
    }

    public ExpressionNode parseExpression(){
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

        // literal
        if (token.type == TokenType.INT_LITERAL ||
                token.type == TokenType.STRING_LITERAL ||
                token.type == TokenType.CHAR_LITERAL ||
                token.type == TokenType.BOOL_LITERAL) {

            expect(token.type);
            return new LiteralNode(token.value);
        }

        if (token.type == TokenType.OPERATOR && token.value.equals("$")) {
            expect(TokenType.OPERATOR, "$");
            return new LiteralNode("\n"); // $ = newline (spec: carriage return)
        }

        if (check(TokenType.PUNCTUATION, "[")) {
            expect(TokenType.PUNCTUATION, "[");
            expect(TokenType.PUNCTUATION, "#");
            expect(TokenType.PUNCTUATION, "]");
            return new LiteralNode("#");
        }

        throw new RuntimeException(
                "Unexpected token " + token.value + " in expression at line " + token.line
        );
    }

    // ================= HELPERS =================

    private void skipComments() {
        while (current < tokens.size() &&
                tokens.get(current).type == TokenType.COMMENT) {
            current++;
        }
    }

    private void expect(TokenType type){
        Token token = peek();

        if(token.type != type){
            throw new RuntimeException(
                    "Syntax error at line " + token.line +
                            ", column " + token.column +
                            " expected type: " + type +
                            " but found: " + token.type
            );
        }

        current++;
        skipComments();
    }

    private void expect(TokenType type, String value){
        Token token = peek();

        if(token.type != type || !token.value.equals(value)){
            throw new RuntimeException(
                    "Syntax error at line " + token.line +
                            ", column " + token.column +
                            " expected: " + value +
                            " but found: " + token.value
            );
        }
        current++;
        skipComments();
    }

    private Token peek() {
        skipComments();
        return tokens.get(current);
    }

    private boolean check(TokenType type, String value){
        Token token = peek();
        return token != null && token.type == type && token.value.equals(value);
    }
}
