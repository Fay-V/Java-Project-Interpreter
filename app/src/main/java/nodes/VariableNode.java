package nodes;

public class VariableNode{
    public String name;
    public ExpressionNode initializer; // optional

    public VariableNode(String name, ExpressionNode initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(indentation).append("VariableNode (name: ").append(name).append(")\n");
        if (initializer != null) {
            sb.append(indentation).append("  initializer:\n");
            sb.append(initializer.toString(indent + 2));
        }
        return sb.toString();
    }
}