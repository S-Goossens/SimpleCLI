package cli.internal;

import java.util.ArrayList;
import java.util.List;

public class Constants {
    protected static final String printKeyword = "print";
    protected static final String helpKeyword = "help";
    protected static final ArrayList<String> exitKeywords = new ArrayList<>(List.of(new String[]{"q", "Q", "exit"}));
    protected static final char[] commentSymbols = {'#', '!'};
    protected static final String helpOutputFormat = "%-30s%-20s%-15s%s%n";
    protected static final String doubleQuoteRegex = "([^\"]\\S*|\".+?\")\\s*";
    protected static final String strRegex = "'(.*?)'";

    //TODO: might move REGEX to another file
    //REGEX
    static final String Digits = "(\\p{Digit}+)";
    static final String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    static final String Exp = "[eE][+-]?" + Digits;
    protected static final String fpRegex =
            ("[\\x00-\\x20]*" +
                    "[+-]?(" +
                    "NaN|" +
                    "Infinity|" +
                    "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +
                    "(\\.(" + Digits + ")(" + Exp + ")?)|" +
                    "((" +
                    "(0[xX]" + HexDigits + "(\\.)?)|" +

                    "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                    ")[pP][+-]?" + Digits + "))" +
                    "[fFdD]?))" +
                    "[\\x00-\\x20]*");
}
