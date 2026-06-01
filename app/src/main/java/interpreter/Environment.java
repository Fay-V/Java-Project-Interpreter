package interpreter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, DataType> types = new HashMap<>();
    private final Set<String> initialized = new HashSet<>();

    void define(String name, DataType type, Object value) {
        if (values.containsKey(name)) {
            throw new RuntimeError(null, "Variable '" + name + "' is already declared.");
        } else {
            values.put(name, value);
            types.put(name, type);
        }
        
        if (value != null) {
            initialized.add(name);
        }
    }

    Object get(Token name) {
        String variableName = name.lexeme;
        if (!values.containsKey(variableName)) {
            throw new RuntimeError(name, "Undefined variable '" + variableName + "'.");
        }
        if (!initialized.contains(variableName)) {
            throw new RuntimeError(name, "Variable '" + variableName + "' has not been initialized.");
        }
        return values.get(variableName);
    }

    DataType getType(String name) {
        return types.get(name);
    }

    void assign(Token name, Object value) {
        String variableName = name.lexeme;
        DataType expected = types.get(variableName);
        if (expected == null) {
            throw new RuntimeError(name, "Undefined variable '" + variableName + "'.");
        }
        if (value == null) {
            throw new RuntimeError(name, "Cannot assign nil to a typed variable.");
        }
        Object converted = checkAndConvert(expected, value, name);
        values.put(variableName, converted);
        initialized.add(variableName);
    }

    private Object checkAndConvert(DataType expected, Object value, Token token) {
        switch (expected) {
            case INT:
                if (value instanceof Integer) return value;
                if (value instanceof Double) {
                    double d = (Double) value;
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        return (int) d;
                    }
                    throw new RuntimeError(token, "Cannot assign a fractional number to INT.");
                }
                throw new RuntimeError(token, "Type mismatch: expected INT.");
            case FLOAT:
                if (value instanceof Double) return value;
                if (value instanceof Integer) return ((Integer) value).doubleValue();
                throw new RuntimeError(token, "Type mismatch: expected FLOAT.");
            case CHAR:
                if (value instanceof Character) return value;
                throw new RuntimeError(token, "Type mismatch: expected CHAR.");
            case BOOL:
                if (value instanceof Boolean) return value;
                throw new RuntimeError(token, "Type mismatch: expected BOOL.");
            default:
                throw new RuntimeError(token, "Unknown data type.");
        }
    }
}