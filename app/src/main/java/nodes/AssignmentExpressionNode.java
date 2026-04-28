package nodes;

public class AssignmentExpressionNode extends ExpressionNode {
    public String variableName;
    public ExpressionNode value;

    public AssignmentExpressionNode(String variableName, ExpressionNode value) {
        this.variableName = variableName;
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "AssignmentExpressionNode (variable: " + variableName + ")\n" +
                indentation + "  value:\n" +
                value.toString(indent + 2);
    }
}