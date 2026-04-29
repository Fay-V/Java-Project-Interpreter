package interpreter;

import nodes.*;

public class Interpreter {

    private final Environment env = new Environment();

    // ================= ENTRY =================

    public void execute(ProgramNode program) {
        for (StatementNode stmt : program.statements) {
            executeStatement(stmt);
        }
    }

    // ================= STATEMENTS =================

    private void executeStatement(StatementNode stmt) {

        if (stmt instanceof DeclarationNode decl) {
            handleDeclaration(decl);
            return;
        }

        if (stmt instanceof AssignmentNode assign) {
            String value = evaluate(assign.value);
            env.assign(assign.name, value);
            return;
        }

        if (stmt instanceof PrintNode print) {
            String result = evaluate(print.expression);
            System.out.println(result);
            return;
        }

        throw new RuntimeException("Unsupported statement: " + stmt.getClass());
    }

    // ================= DECLARATION =================

    private void handleDeclaration(DeclarationNode decl) {

        for (VariableNode var : decl.variables) {

            String value = getDefaultValue(decl.type);

            if (var.initializer != null) {
                value = evaluate(var.initializer);
            }

            env.declare(var.name, value);
        }
    }

    private String getDefaultValue(String type) {
        return switch (type) {
            case "INT" -> "0";
            case "FLOAT" -> "0.0";
            case "CHAR" -> "";     // or "\0" if you want strict char
            case "BOOL" -> "FALSE";
            case "STRING" -> "";
            default -> throw new RuntimeException("Unknown type: " + type);
        };
    }
    // ================= EXPRESSIONS (Increment 1 ONLY) =================

    private String evaluate(ExpressionNode expr) {

        if (expr instanceof LiteralNode lit) {
            return translateLiteral(lit.value);
        }

        if (expr instanceof IdentifierNode id) {
            return env.get(id.name);
        }

        if (expr instanceof BinaryExpressionNode bin) {

            // ONLY concatenation supported in Increment 1
            if (bin.operator.equals("&")) {
                return evaluate(bin.left) + evaluate(bin.right);
            }
        }

        if (expr instanceof AssignmentExpressionNode assignExpr) {
            String value = evaluate(assignExpr.value);
            env.assign(assignExpr.variableName, value);
            return value;
        }

        return "";
    }

    // ================= LEXOR SPECIAL SYMBOLS =================

    private String translateLiteral(String value) {

        if (value == null) return "";

        return switch (value) {
            case "$" -> "\n";
            case "#" -> "#";
            default -> value;
        };
    }
}