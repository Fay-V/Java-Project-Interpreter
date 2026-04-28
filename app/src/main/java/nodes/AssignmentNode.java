package nodes;

public class AssignmentNode extends StatementNode{
    public String variableName;
    public ExpressionNode expression;

    public AssignmentNode(String variableName, ExpressionNode expression){
        this.variableName = variableName;
        this.expression = expression;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "AssignmentNode (variable: " + variableName + ")\n" +
                indentation + "  value:\n" +
                expression.toString(indent + 2);
    }
}
