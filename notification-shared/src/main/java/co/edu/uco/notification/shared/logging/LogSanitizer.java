package co.edu.uco.notification.shared.logging;

public class LogSanitizer {
    private LogSanitizer() {
    }

    public static String sanitize(final String value) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value)
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
