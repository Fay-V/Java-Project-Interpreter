package nodes;

public class AssignmentNode extends StatementNode {
    public String name;
    public ExpressionNode value;

    public AssignmentNode(String name, ExpressionNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "AssignmentNode (variable: " + name + ")\n" +
                indentation + "  value:\n" +
                value.toString(indent + 2);
    }
}