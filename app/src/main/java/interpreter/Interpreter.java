package interpreter;

import java.util.List;
import java.util.Scanner;

import static interpreter.TokenType.*;

public class Interpreter implements Expression.Visitor<Object>, Statement.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Statement> statements) {
        try {
            for (Statement statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            App.runtimeError(error);
        }
    }

    private void execute(Statement stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expression expr) {
        return expr.accept(this);
    }

    // ---------- Statements ----------
    @Override
    public Void visitBlockStmt(Statement.Block stmt) {
        for (Statement s : stmt.statements) {
            execute(s);
        }
        return null;
    }

    @Override
    public Void visitDeclarationStmt(Statement.Declaration stmt) {
        DataType type = dataTypeForToken(stmt.type);
        for (int i = 0; i < stmt.names.size(); i++) {
            Token name = stmt.names.get(i);
            Expression initializer = stmt.initializers.get(i);
            Object value = null;
            if (initializer != null) {
                value = evaluate(initializer);
                value = checkAndConvert(type, value, name);
            }
            environment.define(name.lexeme, type, value);
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Statement.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Statement.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitScanStmt(Statement.Scan stmt) {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        String[] parts = line.split(",");
        if (parts.length != stmt.variables.size()) {
            throw new RuntimeError(stmt.variables.get(0),
                "Expected " + stmt.variables.size() + " values, got " + parts.length + ".");
        }
        for (int i = 0; i < parts.length; i++) {
            Token varToken = stmt.variables.get(i);
            String trimmed = parts[i].trim();
            DataType type = environment.getType(varToken.lexeme);
            if (type == null) {
                throw new RuntimeError(varToken, "Variable '" + varToken.lexeme + "' not declared.");
            }
            Object value = parseInputLiteral(trimmed, type, varToken);
            environment.assign(varToken, value);
        }
        return null;
    }

    private Object parseInputLiteral(String input, DataType type, Token errorToken) {
        try {
            switch (type) {
                case INT: return Integer.parseInt(input);
                case FLOAT: return Double.parseDouble(input);
                case CHAR:
                    if (input.length() == 1) return input.charAt(0);
                    throw new RuntimeError(errorToken, "CHAR must be a single character.");
                case BOOL:
                    if (input.equals("TRUE")) return true;
                    if (input.equals("FALSE")) return false;
                    throw new RuntimeError(errorToken, "BOOl must be TRUE or FALSE.");
                default: throw new RuntimeError(errorToken, "Unknown type.");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeError(errorToken, "Invalid number format.");
        }
    }

    @Override
    public Void visitIfStmt(Statement.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else {
            boolean executed = false;
            for (Statement.If.ElseIf elseIf : stmt.elseIfs) {
                if (isTruthy(evaluate(elseIf.condition))) {
                    execute(elseIf.thenBranch);
                    executed = true;
                    break;
                }
            }
            if (!executed && stmt.elseBranch != null) {
                execute(stmt.elseBranch);
            }
        }
        return null;
    }

    @Override
    public Void visitForStmt(Statement.For stmt) {
        if (stmt.initializer != null) {
            evaluate(stmt.initializer);
        }
        while (true) {
            if (stmt.condition != null) {
                if (!isTruthy(evaluate(stmt.condition))) break;
            }
            execute(stmt.body);
            if (stmt.increment != null) {
                evaluate(stmt.increment);
            }
        }
        return null;
    }

    @Override
    public Void visitRepeatStmt(Statement.Repeat stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    // ---------- Expressions ----------
    @Override
    public Object visitLiteralExpr(Expression.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expression.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (right instanceof Integer) ? -(int) right : -(double) right;
            case PLUS:
                checkNumberOperand(expr.operator, right);
                return (right instanceof Integer) ? +(int) right : +(double) right;
            case NOT:
                checkBooleanOperand(expr.operator, right);
                return !(boolean) right;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expression.Binary expr) {
        // Short-circuit logical operators
        if (expr.operator.type == AND) {
            Object left = evaluate(expr.left);
            checkBooleanOperand(expr.operator, left);
            if (!(boolean) left) return false;
            Object right = evaluate(expr.right);
            checkBooleanOperand(expr.operator, right);
            return (boolean) right;
        }
        if (expr.operator.type == OR) {
            Object left = evaluate(expr.left);
            checkBooleanOperand(expr.operator, left);
            if ((boolean) left) return true;
            Object right = evaluate(expr.right);
            checkBooleanOperand(expr.operator, right);
            return (boolean) right;
        }

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof String || right instanceof String)
                    return stringify(left) + stringify(right);
                // numeric addition
                if (left instanceof Integer && right instanceof Integer) return (int) left + (int) right;
                if (left instanceof Double && right instanceof Double) return (double) left + (double) right;
                if (left instanceof Integer && right instanceof Double) return (int) left + (double) right;
                if (left instanceof Double && right instanceof Integer) return (double) left + (int) right;
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS: checkNumberOperands(expr.operator, left, right);
                return numericOp(left, right, (a,b)->a-b, (a,b)->a-b);
            case STAR: checkNumberOperands(expr.operator, left, right);
                return numericOp(left, right, (a,b)->a*b, (a,b)->a*b);
            case SLASH: checkNumberOperands(expr.operator, left, right);
                if (toDouble(right) == 0.0) throw new RuntimeError(expr.operator, "Division by zero.");
                return numericOp(left, right, (a,b)->a/b, (a,b)->a/b);
            case PERCENT: checkIntegerOperands(expr.operator, left, right);
                return (int) left % (int) right;
            case AMPERSAND:
                return stringify(left) + stringify(right);
            case GREATER: checkNumberOperands(expr.operator, left, right);
                return toDouble(left) > toDouble(right);
            case GREATER_EQUAL: checkNumberOperands(expr.operator, left, right);
                return toDouble(left) >= toDouble(right);
            case LESS: checkNumberOperands(expr.operator, left, right);
                return toDouble(left) < toDouble(right);
            case LESS_EQUAL: checkNumberOperands(expr.operator, left, right);
                return toDouble(left) <= toDouble(right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case NOT_EQUAL: return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expression.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expression.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitGroupingExpr(Expression.Grouping expr) {
        return evaluate(expr.expression);
    }

    // ---------- Helpers ----------
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        // Handle boolean-string comparison
        if (a instanceof Boolean && b instanceof String) {
            String bStr = (String) b;
            if (bStr.equals("TRUE")) return (Boolean) a == true;
            if (bStr.equals("FALSE")) return (Boolean) a == false;
            return false;
        }
        if (b instanceof Boolean && a instanceof String) {
            String aStr = (String) a;
            if (aStr.equals("TRUE")) return (Boolean) b == true;
            if (aStr.equals("FALSE")) return (Boolean) b == false;
            return false;
        }

        return a.equals(b);
    }

    private String stringify(Object value) {
        if (value == null) return "nil";
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return text;
        }
        if (value instanceof Character) return String.valueOf((char) value);
        if (value instanceof String) return (String) value;
        if (value instanceof Boolean) return ((boolean) value) ? "TRUE" : "FALSE";
        return value.toString();
    }

    private DataType dataTypeForToken(Token token) {
        switch (token.type) {
            case INT: return DataType.INT;
            case FLOAT: return DataType.FLOAT;
            case CHAR: return DataType.CHAR;
            case BOOL: return DataType.BOOL;
            default: throw new RuntimeError(token, "Invalid data type.");
        }
    }

    private Object checkAndConvert(DataType expected, Object value, Token token) {
        if (value == null) return null;
        switch (expected) {
            case INT:
                if (value instanceof Integer) return value;
                if (value instanceof Double) {
                    double d = (Double) value;
                    if (d == Math.floor(d) && !Double.isInfinite(d)) return (int) d;
                    throw new RuntimeError(token, "Cannot assign fractional number to INT.");
                }
                throw new RuntimeError(token, "Type mismatch: expected INT.");
            case FLOAT:
                if (value instanceof Double) return value;
                if (value instanceof Integer) return ((Integer) value).doubleValue();
                throw new RuntimeError(token, "Type mismatch: expected FLOAT.");
            case CHAR:
                if (value instanceof Character) return value;
                throw new RuntimeError(token, "Type mismatch: expected CHAR.");
            case BOOL:
                if (value instanceof Boolean) return value;
    if (value instanceof String) {
        String s = (String) value;
            if (s.equalsIgnoreCase("TRUE")) return true;
            if (s.equalsIgnoreCase("FALSE")) return false;
            throw new RuntimeError(token, "Invalid boolean literal: " + s);
        }
        throw new RuntimeError(token, "Type mismatch: expected BOOL.");
        }
        throw new RuntimeError(token, "Unknown type.");
    }

    private double toDouble(Object value) {
        if (value instanceof Integer) return (int) value;
        return (double) value;
    }

    private interface IntBinaryOp { int apply(int a, int b); }
    private interface DoubleBinaryOp { double apply(double a, double b); }

    private Object numericOp(Object a, Object b, IntBinaryOp intOp, DoubleBinaryOp doubleOp) {
        if (a instanceof Integer && b instanceof Integer) return intOp.apply((int)a, (int)b);
        double da = (a instanceof Integer) ? (int)a : (double)a;
        double db = (b instanceof Integer) ? (int)b : (double)b;
        return doubleOp.apply(da, db);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Integer || operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if ((left instanceof Integer || left instanceof Double) && (right instanceof Integer || right instanceof Double)) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private void checkIntegerOperands(Token operator, Object left, Object right) {
        if (left instanceof Integer && right instanceof Integer) return;
        throw new RuntimeError(operator, "Operands must be integers.");
    }

    private void checkBooleanOperand(Token operator, Object operand) {
        if (operand instanceof Boolean) return;
        throw new RuntimeError(operator, "Operand must be a boolean.");
    }
}