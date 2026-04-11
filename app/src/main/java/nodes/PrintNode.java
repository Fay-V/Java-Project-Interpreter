package nodes;

public class PrintNode extends StatementNode{
    ExpressionNode expression;
    public PrintNode(ExpressionNode expression){
        this.expression = expression;
    }

    @Override
    public String toString(int indent) {
        String indentation = "  ".repeat(indent);
        return indentation + "PrintNode\n" +
                expression.toString(indent + 1);
    }
}
