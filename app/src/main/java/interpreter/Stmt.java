package interpreter;

import java.util.List;

abstract class Stmt {
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

    static class Block extends Stmt {
        final List<Stmt> statements;
        Block(List<Stmt> statements) { this.statements = statements; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }
    }

    static class Declaration extends Stmt {
        final Token type;
        final List<Token> names;
        final List<Expr> initializers;
        Declaration(Token type, List<Token> names, List<Expr> initializers) {
            this.type = type; this.names = names; this.initializers = initializers;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitDeclarationStmt(this); }
    }

    static class Expression extends Stmt {
        final Expr expression;
        Expression(Expr expression) { this.expression = expression; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }
    }

    static class Print extends Stmt {
        final Expr expression;
        Print(Expr expression) { this.expression = expression; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitPrintStmt(this); }
    }

    static class Scan extends Stmt {
        final List<Token> variables;
        Scan(List<Token> variables) { this.variables = variables; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitScanStmt(this); }
    }

    static class If extends Stmt {
        static class ElseIf {
            final Expr condition;
            final Stmt thenBranch;
            ElseIf(Expr condition, Stmt thenBranch) { this.condition = condition; this.thenBranch = thenBranch; }
        }
        final Expr condition;
        final Stmt thenBranch;
        final List<ElseIf> elseIfs;
        final Stmt elseBranch;
        If(Expr condition, Stmt thenBranch, List<ElseIf> elseIfs, Stmt elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch;
            this.elseIfs = elseIfs; this.elseBranch = elseBranch;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }
    }

    static class For extends Stmt {
        final Expr initializer;
        final Expr condition;
        final Expr increment;
        final Stmt body;
        For(Expr initializer, Expr condition, Expr increment, Stmt body) {
            this.initializer = initializer; this.condition = condition;
            this.increment = increment; this.body = body;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitForStmt(this); }
    }

    static class Repeat extends Stmt {
        final Expr condition;
        final Stmt body;
        Repeat(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitRepeatStmt(this); }
    }
}