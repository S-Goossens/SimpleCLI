package cli.internal;

import java.util.HashMap;

/**
 * Class that holds the state for the CLI.
 * Uses a singleton pattern.
 */
public class CliState {

    private static CliState instance;
    private final HashMap<String, Object> variables;

    private CliState() {
        variables = new HashMap<>();
    }

    public static CliState getInstance() {
        if (instance == null) {
            instance = new CliState();
        }
        return instance;
    }

    public void addVariable(String name, Object object) {
        variables.put(name, object);
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }
}
