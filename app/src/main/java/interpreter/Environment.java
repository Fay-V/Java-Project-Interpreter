package interpreter;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final Map<String, String> memory = new HashMap<>();

    public void declare(String name, String value) {
        memory.put(name, value);
    }

    public void assign(String name, String value) {
        if (!memory.containsKey(name)) {
            throw new RuntimeException("Undeclared variable: " + name);
        }
        memory.put(name, value);
    }

    public String get(String name) {
        if (!memory.containsKey(name)) {
            throw new RuntimeException("Undefined variable: " + name);
        }
        return memory.get(name);
    }
}