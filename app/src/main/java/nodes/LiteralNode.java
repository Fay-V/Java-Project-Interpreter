package nodes;

public class LiteralNode extends ExpressionNode {
    public String value;

    public LiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "LiteralNode (value: " + value + ")\n";
    }
}
