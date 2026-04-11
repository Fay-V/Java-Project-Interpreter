package nodes;

import java.util.ArrayList;
import java.util.List;

public class ProgramNode extends ASTNode{
    List<StatementNode> statements;
    public ProgramNode(){
        this.statements = new ArrayList<>();
    }

    public void addStatement(StatementNode statement){
        statements.add(statement);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(indentation).append("ProgramNode\n");
        for (StatementNode stmt : statements) {
            sb.append(stmt.toString(indent + 1));
        }
        return sb.toString();
    }
}
