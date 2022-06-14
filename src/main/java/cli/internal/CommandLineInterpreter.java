package cli.internal;

import cli.api.Command;
import cli.api.CommandInterpreter;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineInterpreter implements CommandInterpreter {
    private final Class[] cls;
    private final HashMap<String, Method> commands = new HashMap<>();
    private final HashMap<Class<?>, Object> instances = new HashMap<>();
    PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private String shellPrefix = "";
    private String description = "";
    private boolean interactive = false;
    private boolean fileLoop = false;

    public CommandLineInterpreter(Class[] cls, String shellPrefix, String description) {
        this.cls = cls;
        this.shellPrefix = shellPrefix;
        this.description = description;
        initialiseAnnotationCommands();
    }

    public CommandLineInterpreter(Class[] cls, String shellPrefix) {
        this.cls = cls;
        this.shellPrefix = shellPrefix;
        initialiseAnnotationCommands();
    }

    public CommandLineInterpreter(Class[] cls) {
        this.cls = cls;
        initialiseAnnotationCommands();
    }

    @Override
    public void start() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            interactive = true;

            run(br);

            br.close();
        } catch (Exception e) {
            out.println(ExceptionHandler.handleException(e));
        }
    }

    @Override
    public void startFromFile(String filename, boolean debugMode) {
        try {
            runFile(filename, debugMode);
        } catch (Exception e) {
            out.println(ExceptionHandler.handleException(e));
            System.exit(-1);
        }
    }

    public void runFile(String filename, boolean debugMode) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));

        String input;
        int line = 1;

        fileLoop = true;

        while ((input = br.readLine()) != null && !Constants.exitKeywords.contains(input) && fileLoop) {
            try {
                input = input.trim();
                input = removeInlineComments(input);

                if (isValidLine(input)) {
                    if (debugMode) out.println(input);
                    handleInput(input);
                }

                line++;
            } catch (Exception e) {
                out.println("Error at command: '" + input + "', at line " + line + ", in file \"" + filename + "\".");
                String output = ExceptionHandler.handleException(e);
                if (output != null) out.println(output);

                if (!interactive) {
                    out.println("Exit...");
                    System.exit(-1);
                }

                fileLoop = false;
            }
        }

        br.close();
    }

    /**
     * Runs the interpreter based on a BufferedReader supplying the CLI with input.
     *
     * @param br BufferedReader, can be a InputStreamReader or FileReader
     */
    private void run(BufferedReader br) throws IOException {
        String input;

        out.print(shellPrefix);

        while ((input = br.readLine()) != null && !Constants.exitKeywords.contains(input)) {
            try {
                input = input.trim();
                input = removeInlineComments(input);

                if (isValidLine(input)) {
                    handleInput(input);
                }

                // This adds the prefix BEFORE the next line is read.
                out.print(shellPrefix);
            } catch (Exception e) {
                out.println("Error at command: '" + input + "'.");
                String output = ExceptionHandler.handleException(e);
                if (output != null) out.println(output);

                out.print(shellPrefix);
            }
        }
    }

    private String removeInlineComments(String input) {
        ArrayList<Integer> quoteIndexes = new ArrayList<>();
        ArrayList<Integer> commentIndexes = new ArrayList<>();

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == "\"".charAt(0)) {
                quoteIndexes.add(i);
            } else {
                for (char c : Constants.commentSymbols) {
                    if (c == current) {
                        if (i == 0) {
                            // complete line is a comment, return empty string
                            return "";
                        }
                        commentIndexes.add(i);
                    }
                }
            }
        }

        if (quoteIndexes.size() % 2 != 0) {
            throw new IllegalArgumentException("Unclosed string.");
        }

        for (int i : commentIndexes) {
            boolean inString = false;
            for (int j = 0; j < quoteIndexes.size(); j += 2) {
                if (i > quoteIndexes.get(j) && i < quoteIndexes.get(j + 1)) {
                    inString = true;
                    break;
                }
            }
            if (!inString) {
                input = input.substring(0, i);
                break;
            }
        }

        return input;
    }

    /**
     * Checks if the line is a comment or is empty.
     *
     * @param input line to check
     * @return boolean that tells if the line is valid and should be handled and otherwise ignored
     */
    private boolean isValidLine(String input) {
        return !input.isEmpty() && !input.startsWith("#");
    }

    /**
     * Handles the input.
     *
     * @param input line to handle
     */
    private void handleInput(String input) throws Exception {
        boolean store = false;
        String varName = "";
        String commandString;
        int assignmentIndex = input.indexOf("=");

        if (assignmentIndex != -1) {
            varName = input.substring(0, assignmentIndex).trim().replace(" ", "");
            commandString = input.substring(assignmentIndex + 1).trim();
            store = true;
        } else {
            commandString = input;
        }

        // Code that allows users to use double quotes for indicating string with spaces.
        ArrayList<String> args = new ArrayList<>();
        Matcher m = Pattern.compile(Constants.doubleQuoteRegex).matcher(commandString);
        while (m.find())
            args.add(m.group(1).replace("\"", "'"));

        String command = args.remove(0);

        handleCommands(command, args, store, varName);
        CommandHistory.getInstance().addToHistory(input);
    }

    private void initialiseAnnotationCommands() {
        for (Class c : cls) {
            Method[] methods = c.getMethods();
            for (Method m : methods) {
                Command metadata = m.getAnnotation(Command.class);
                if (metadata != null) {
                    String commandName = (metadata.key().isBlank() || metadata.key().isEmpty()) ? m.getName() : metadata.key();
                    commands.put(commandName, m);
                }
            }
        }
    }

    private void handleCommands(String command, ArrayList<String> args, boolean store, String varName) throws Exception {
        if (store && (commands.containsKey(varName) ||
                Constants.exitKeywords.contains(varName) ||
                Constants.printKeyword.equals(varName)) ||
                Constants.helpKeyword.equals(varName)
        ) {
            throw new IllegalArgumentException(varName + " is a reserved keyword");
        }

        Method m = commands.get(command);

        if (m != null) {
            handleAnnotationCommand(m, args, store, varName);
            return;
        }

        switch (command) {
            case Constants.printKeyword -> {
                printVariable(args);
                return;
            }
            case Constants.helpKeyword -> {
                if (args.get(0) != null) {
                    Set<String> commandKeys = commands.keySet();
                    ArrayList<String> foundKeys = new ArrayList<>();

                    for (String c: commandKeys) {
                        if (c.contains(args.get(0))) foundKeys.add(c);
                    }

                    if (foundKeys.size() > 0) {
                        out.println("Commands found with search:" + System.lineSeparator());
                        for (String c : foundKeys) {
                            Method commandMethod = commands.get(c);
                            out.println(ParameterResolver.generateHelp(commandMethod, commandMethod.getAnnotation(Command.class)));
                        }
                    }

                } else {
                    printHelp();
                }
                return;
            }
            case "call" -> {
                if ((args.contains("-f") || args.contains("--file"))) {
                    String filename = args.get(1).replace("'", "");
                    boolean debug = args.contains("-d") || args.contains("--debug");
                    runFile(filename, debug);
                } else {
                    throw new IllegalArgumentException("No file given for call");
                }
                return;
            }
            case "write-script" -> {
                if ((args.contains("-f") || args.contains("--file"))) {
                    String filename = args.get(1).replace("'", "");
                    CommandHistory.getInstance().writeHistoryToFile(filename);
                } else {
                    throw new IllegalArgumentException("No output file given");
                }
                return;
            }
        }

        if (store) {
            if (command.matches(Constants.strRegex) || Pattern.matches(Constants.fpRegex, command) || command.equals("true") || command.equals("false")) {
                storeVariable(varName, command);
                return;
            }
        }

        out.println("Command '" + command + "' not found.");
    }

    /**
     * Method that prints the help info for all commands
     */
    private void printHelp() {
        //TODO: add explanation about default commands and maybe allow a title and version to be added.
        if (!description.isEmpty()) out.println(description);
        out.println("Appending a command with the -h flag will print the description and parameters for the command.");
        commands.forEach((key, value) -> out.println(ParameterResolver.generateHelp(value, value.getAnnotation(Command.class))));
    }

    /**
     * Prints all variables given as arguments
     *
     * @param args given arguments
     */
    private void printVariable(ArrayList<String> args) {
        for (String arg : args) {
            Object var = CliState.getInstance().getVariable(arg);

            if (var != null) {
                out.println(var);
            } else {
                out.println("Object does not exist.");
            }
        }
    }

    /**
     * Stores a variable to the @see{@link CliState} instance.
     *
     * @param varName name for the variable
     * @param value   value to save
     */
    private void storeVariable(String varName, String value) {
        Object var;

        if ((value.startsWith("'") && value.endsWith("'"))) {
            var = value.substring(1, value.length() - 1);
        } else if (value.equals("true") || value.equals("false")) {
            var = Boolean.parseBoolean(value);
        } else {
            var = value;
        }

        CliState.getInstance().addVariable(varName, var);
    }

    /**
     * Handles the case where a command is used that is in the list of annotated methods.
     *
     * @param m    the annotated method
     * @param args the arguments to pass through
     */
    private void handleAnnotationCommand(Method m, ArrayList<String> args, boolean store, String varName) throws Exception {

        if (args.size() > 0) {
            Command metadata = m.getAnnotation(Command.class);
            switch (args.get(0)) {
                case "-d", "--description" -> {
                    out.println(metadata.description());
                    return;
                }
                case "-h", "--help" -> {
                    out.println(ParameterResolver.generateHelp(m, metadata));
                    return;
                }
                default -> {
                }
            }
        }

        Class<?> clazz = m.getDeclaringClass();
        Object obj;
        if (instances.containsKey(clazz)) {
            obj = instances.get(clazz);
        } else {
            obj = clazz.getDeclaredConstructor().newInstance();
            instances.put(clazz, obj);
        }

        List<Object> arguments = ParameterResolver.processArguments(args, m);

        // Call method with arguments
        Object returnValue = m.invoke(obj, arguments.toArray());

        if (store) {
            CliState.getInstance().addVariable(varName, returnValue);
        }
    }
}
