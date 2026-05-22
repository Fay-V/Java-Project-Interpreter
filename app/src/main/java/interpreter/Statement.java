package interpreter;

import java.util.List;

abstract class Statement {
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitDeclarationStmt(Declaration stmt);
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitScanStmt(Scan stmt);
        R visitIfStmt(If stmt);
        R visitForStmt(For stmt);
        R visitRepeatStmt(Repeat stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Block extends Statement {
        final List<Statement> statements;
        Block(List<Statement> statements) { this.statements = statements; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }
    }

    static class Declaration extends Statement {
        final Token type;
        final List<Token> names;
        final List<interpreter.Expression> initializers;
        Declaration(Token type, List<Token> names, List<interpreter.Expression> initializers) {
            this.type = type; this.names = names; this.initializers = initializers;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitDeclarationStmt(this); }
    }

    static class Expression extends Statement {
        final interpreter.Expression expression;
        Expression(interpreter.Expression expression) { this.expression = expression; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }
    }

    static class Print extends Statement {
        final interpreter.Expression expression;
        Print(interpreter.Expression expression) { this.expression = expression; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitPrintStmt(this); }
    }

    static class Scan extends Statement {
        final List<Token> variables;
        Scan(List<Token> variables) { this.variables = variables; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitScanStmt(this); }
    }

    static class If extends Statement {
        static class ElseIf {
            final interpreter.Expression condition;
            final Statement thenBranch;
            ElseIf(interpreter.Expression condition, Statement thenBranch) { this.condition = condition; this.thenBranch = thenBranch; }
        }
        final interpreter.Expression condition;
        final Statement thenBranch;
        final List<ElseIf> elseIfs;
        final Statement elseBranch;
        If(interpreter.Expression condition, Statement thenBranch, List<ElseIf> elseIfs, Statement elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch;
            this.elseIfs = elseIfs; this.elseBranch = elseBranch;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }
    }

    static class For extends Statement {
        final interpreter.Expression initializer;
        final interpreter.Expression condition;
        final interpreter.Expression increment;
        final Statement body;
        For(interpreter.Expression initializer, interpreter.Expression condition, interpreter.Expression increment, Statement body) {
            this.initializer = initializer; this.condition = condition;
            this.increment = increment; this.body = body;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitForStmt(this); }
    }

    static class Repeat extends Statement {
        final interpreter.Expression condition;
        final Statement body;
        Repeat(interpreter.Expression condition, Statement body) { this.condition = condition; this.body = body; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitRepeatStmt(this); }
    }
}