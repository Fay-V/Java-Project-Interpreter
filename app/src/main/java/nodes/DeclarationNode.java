package nodes;

import nodes.StatementNode;
import nodes.VariableNode;

import java.util.List;

public class DeclarationNode extends StatementNode {
    public String type; // INT, CHAR, BOOL, FLOAT
    public List<VariableNode> variables;

    public DeclarationNode(String type, List<VariableNode> variables) {
        this.type = type;
        this.variables = variables;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(indentation).append("DeclarationNode (type: ").append(type).append(")\n");
        for (VariableNode var : variables) {
            sb.append(var.toString(indent + 1));
        }
        return sb.toString();
    }
}