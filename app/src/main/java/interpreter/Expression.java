package interpreter;

public abstract class Expression {
    public interface Visitor<R> {
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitBinaryExpr(Binary expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitGroupingExpr(Grouping expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Literal extends Expression {
        final Object value;
        Literal(Object value) { this.value = value; }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitLiteralExpr(this); }
    }

    public static class Unary extends Expression {
        final Token operator;
        final Expression right;
        Unary(Token operator, Expression right) { this.operator = operator; this.right = right; }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitUnaryExpr(this); }
    }

    public static class Binary extends Expression {
        final Expression left;
        final Token operator;
        final Expression right;
        Binary(Expression left, Token operator, Expression right) {
            this.left = left; this.operator = operator; this.right = right;
        }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitBinaryExpr(this); }
    }

    public static class Variable extends Expression {
        final Token name;
        Variable(Token name) { this.name = name; }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitVariableExpr(this); }
    }

    public static class Assign extends Expression {
        final Token name;
        final Expression value;
        Assign(Token name, Expression value) { this.name = name; this.value = value; }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitAssignExpr(this); }
    }

    public static class Grouping extends Expression {
        final Expression expression;
        Grouping(Expression expression) { this.expression = expression; }
        @Override public <R> R accept(Visitor<R> visitor) { return visitor.visitGroupingExpr(this); }
    }
}