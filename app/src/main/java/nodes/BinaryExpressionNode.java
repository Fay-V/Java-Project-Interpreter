package nodes;

public class BinaryExpressionNode extends ExpressionNode {
    public ExpressionNode left;
    public String operator; // "&"
    public ExpressionNode right;

    public BinaryExpressionNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "BinaryExpressionNode (op: " + operator + ")\n" +
                indentation + "  left:\n" +
                left.toString(indent + 2) +
                indentation + "  right:\n" +
                right.toString(indent + 2);
    }
}
