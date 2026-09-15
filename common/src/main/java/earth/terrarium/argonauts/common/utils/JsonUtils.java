package earth.terrarium.argonauts.common.utils;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String stripComments(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                sb.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                sb.append(c);
                continue;
            }
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') i++;
                if (i < text.length()) sb.append('\n');
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
