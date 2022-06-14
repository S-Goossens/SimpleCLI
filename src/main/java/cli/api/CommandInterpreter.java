package cli.api;

import cli.internal.CommandLineInterpreter;

public interface CommandInterpreter {

    /**
     * Creates a new CommandInterpreter based on the package and sets the shell prefix.
     *
     * @param cls         class to scan for methods with the @see{@link Command} annotation
     * @param shellPrefix the prefix shown at the start of a terminal line e.g. GooseCLI=>
     * @return new CommandInterpreter
     */
    static CommandInterpreter create(Class[] cls, String shellPrefix) {
        return new CommandLineInterpreter(cls, shellPrefix);
    }

    /**
     * Creates a new CommandInterpreter based on the package.
     *
     * @param cls class to scan for methods with the @see{@link Command} annotation
     * @return new CommandInterpreter
     */
    static CommandInterpreter create(Class[] cls) {
        return new CommandLineInterpreter(cls);
    }

    /**
     * Starts the command line interpreter
     */
    void start();

    /**
     * Starts the interpreter with instructions from a file.
     *
     * @param filename name of the file with the instructions for the interpreter
     */
    void startFromFile(String filename, boolean debugMode);
}
