package cli.internal;

import cli.api.Command;
import cli.api.CommandParameter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ParameterResolver {
    /**
     * Generates the help instructions based on the metadata and parameters.
     *
     * @param m        method that should be explained
     * @param metadata metadata of the method @see{@link Command}
     * @return string with the help output
     */
    public static String generateHelp(Method m, Command metadata) {
        StringBuilder output = new StringBuilder();
        //TODO: usage should be automated (automatically add required in the right order)
        output.append("Usage: ").append(metadata.key()).append(" [OPTION]...").append(System.lineSeparator());
        output.append("\t").append(metadata.description()).append(System.lineSeparator());
        Parameter[] params = m.getParameters();

        for (Parameter p : params) {
            CommandParameter cp = p.getAnnotation(CommandParameter.class);

            String dataType = p.getType().getSimpleName();
            String req = cp.required() ? "required" : "";
            output.append(String.format(Constants.helpOutputFormat, Arrays.toString(cp.keys()), dataType, req, cp.help()));
        }

        return output.toString();
    }

    /**
     * Get method parameters annotated with @see{@link CommandParameter}.
     *
     * @param params parameters of the method
     * @return list of CommandParameter annotations.
     */
    public static ArrayList<CommandParameter> getCommandParameters(Parameter[] params) {
        // add parameter if there is no key.
        ArrayList<CommandParameter> commandParameters = new ArrayList<>();

        for (Parameter p : params) {
            commandParameters.add(p.getAnnotation(CommandParameter.class));
        }

        return commandParameters;
    }

    /**
     * Processes the arguments that are given and checks whether all the required arguments are present.
     *
     * @param args list of string that are the given arguments
     * @param m    method to process
     * @return list of arguments to be given to method
     */
    public static List<Object> processArguments(ArrayList<String> args, Method m) {
        Parameter[] params = m.getParameters();

        List<Object> parameters = new ArrayList<>();

        List<String[]> missingKeys = new ArrayList<>();

        for (Parameter p : params) {
            CommandParameter cp = p.getAnnotation(CommandParameter.class);

            String[] keys = cp.keys();

            Class<?> clazz = p.getType();

            int keyIndex = -1;

            for (String key : keys) {
                int index = args.indexOf(key);
                //TODO: allow execution with positional arguments as well
                if (index >= 0) {
                    keyIndex = index;
                    break;
                }
            }

            if (keyIndex >= 0) {

                //Check data type: if it's a boolean or number we need to convert
                if (args.get(keyIndex + 1) == null) {
                    throw new IllegalArgumentException("Missing argument for the flag " + keys);
                }

                String value = args.get(keyIndex + 1);
                Object var;

                if ((value.startsWith("'") && value.endsWith("'"))) {
                    var = castParameter(clazz, value.substring(1, value.length()-1));
                } else if (Pattern.matches(Constants.fpRegex, value)) {
                    var = castParameter(clazz, value);
                } else if (value.equals("true") || value.equals("false")) {
                    var = Boolean.parseBoolean(value);
                } else {
                    Object variable = CliState.getInstance().getVariable(value);
                    if (variable != null) {
                        var = castParameter(clazz, variable);
                    } else {
                        throw new IllegalArgumentException("Variable " + value + " not found.");
                    }
                }

                parameters.add(var);

                args.remove(keyIndex+1);
                args.remove(keyIndex);
            } else if (cp.required()) {
                missingKeys.add(keys);
            } else {
                parameters.add(null);
            }
        }

        for (int i = 0; i < args.size(); i++) {
            System.out.println("Unrecognized parameter: " + args.get(i) + " " + args.get(i+1));
            i++;
        }

        if (missingKeys.size() > 0) {
            StringBuilder missing = new StringBuilder();
            for (String[] keys: missingKeys) {
                missing.append(System.lineSeparator()).append("\t- ").append(Arrays.toString(keys));
            }
            throw new IllegalArgumentException("Please add parameter(s) with key(s): " + missing);
        }

        return parameters;
    }

    private static Object castParameter(Class<?> clazz, Object var) {
        if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return Double.parseDouble(var.toString());
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return Integer.parseInt(var.toString());
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return Long.parseLong(var.toString());
        } else {
            return clazz.cast(var);
        }
    }
}
