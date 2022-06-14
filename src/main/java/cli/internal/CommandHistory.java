package cli.internal;

import java.io.*;
import java.util.Stack;

public class CommandHistory {
    private static CommandHistory instance;
    private final Stack<String> history;

    private CommandHistory() {
        history = new Stack<>();
    }

    public static CommandHistory getInstance() {
        if (instance == null) {
            instance = new CommandHistory();
        }
        return instance;
    }

    public void addToHistory(String command) {
        history.add(command);
    }

    public void writeHistoryToFile(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedWriter outputStream = new BufferedWriter(new FileWriter(filename, false));
        for (String cmd : history) {
            sb.append(cmd + System.lineSeparator());
        }
        String str = sb.toString();
        outputStream.write(str, 0, str.length());
        outputStream.close();
    }
}
