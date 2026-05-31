package interpreter;

import java.util.List;

public abstract class Statement {
    public interface Visitor {
        void visitBlockStmt(Block stmt);
        void visitDeclarationStmt(Declaration stmt);
        void visitExpressionStmt(Expression stmt);
        void visitPrintStmt(Print stmt);
        void visitScanStmt(Scan stmt);
        void visitIfStmt(If stmt);
        void visitForStmt(For stmt);
        void visitRepeatStmt(Repeat stmt);
    }

    public abstract void accept(Visitor visitor);

    public static class Block extends Statement {
        final List<Statement> statements;
        Block(List<Statement> statements) { this.statements = statements; }
        @Override public void accept(Visitor visitor) {
            visitor.visitBlockStmt(this);
        }
    }

    public static class Declaration extends Statement {
        final Token type;
        final List<Token> names;
        final List<interpreter.Expression> initializers;
        Declaration(Token type, List<Token> names, List<interpreter.Expression> initializers) {
            this.type = type; this.names = names; this.initializers = initializers;
        }
        @Override public void accept(Visitor visitor) {
            visitor.visitDeclarationStmt(this);
        }
    }

    public static class Expression extends Statement {
        final interpreter.Expression expression;
        Expression(interpreter.Expression expression) { this.expression = expression; }
        @Override public void accept(Visitor visitor) {
            visitor.visitExpressionStmt(this);
        }
    }

    public static class Print extends Statement {
        final interpreter.Expression expression;
        Print(interpreter.Expression expression) { this.expression = expression; }
        @Override public void accept(Visitor visitor) {
            visitor.visitPrintStmt(this);
        }
    }

    public static class Scan extends Statement {
        final List<Token> variables;
        Scan(List<Token> variables) { this.variables = variables; }
        @Override public void accept(Visitor visitor) {
            visitor.visitScanStmt(this);
        }
    }

    public static class If extends Statement {
        record ElseIf(interpreter.Expression condition, Statement thenBranch) {
        }
        final interpreter.Expression condition;
        final Statement thenBranch;
        final List<ElseIf> elseIfs;
        final Statement elseBranch;
        If(interpreter.Expression condition, Statement thenBranch, List<ElseIf> elseIfs, Statement elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch;
            this.elseIfs = elseIfs; this.elseBranch = elseBranch;
        }
        @Override public void accept(Visitor visitor) {
            visitor.visitIfStmt(this);
        }
    }

    public static class For extends Statement {
        final interpreter.Expression initializer;
        final interpreter.Expression condition;
        final interpreter.Expression increment;
        final Statement body;
        For(interpreter.Expression initializer, interpreter.Expression condition, interpreter.Expression increment, Statement body) {
            this.initializer = initializer; this.condition = condition;
            this.increment = increment; this.body = body;
        }
        @Override public void accept(Visitor visitor) {
            visitor.visitForStmt(this);
        }
    }

    public static class Repeat extends Statement {
        final interpreter.Expression condition;
        final Statement body;
        Repeat(interpreter.Expression condition, Statement body) { this.condition = condition; this.body = body; }
        @Override public void accept(Visitor visitor) {
            visitor.visitRepeatStmt(this);
        }
    }
}