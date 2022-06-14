package cli.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class ExceptionHandler {
    public static String handleException(Exception e) {
        if (e instanceof InstantiationException) {
            return "Could not create instance of requested class.";
        } else if (e instanceof IllegalAccessException) {
            return "Could not access method.";
        } else if (e instanceof InvocationTargetException) {
            return "Something went wrong in called method: " + e.getStackTrace()[e.getStackTrace().length-1] +
                    System.lineSeparator() + "Caused by " + e.getCause();
        } else if (e instanceof NoSuchMethodException) {
            return "Method could not be found.";
        } else if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        } else if (e instanceof ClassCastException) {
            return "Unable to cast class, \n" + e.getMessage();
        } else {
            return e.getMessage();
        }
    }
}
