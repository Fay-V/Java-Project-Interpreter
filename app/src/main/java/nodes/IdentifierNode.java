package nodes;

public class IdentifierNode extends ExpressionNode {
    public String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "IdentifierNode (name: " + name + ")\n";
    }
}