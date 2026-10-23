package payroll;

import java.util.Map;

/** Small JSON writer for the dashboard's maps, lists, and scalar values. */
public final class Json {
    private Json() {}
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number number) {
            if (!Double.isFinite(number.doubleValue())) throw new IllegalArgumentException("Non-finite JSON number");
            return number.toString();
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            var result = new java.util.StringJoiner(",", "{", "}");
            map.forEach((key, item) -> result.add(quote(key.toString()) + ":" + stringify(item)));
            return result.toString();
        }
        if (value instanceof Iterable<?> items) {
            var result = new java.util.StringJoiner(",", "[", "]");
            items.forEach(item -> result.add(stringify(item)));
            return result.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value");
    }
    private static String quote(String text) {
        var result = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 32 || Character.isSurrogate(character)) result.append(String.format("\\u%04x", (int) character));
                    else result.append(character);
                }
            }
        }
        return result.append('"').toString();
    }
}
