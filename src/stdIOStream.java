import java.util.ArrayList;

public record stdIOStream(ArrayList<String> bash, String expr, int stat) {
    public stdIOStream(ArrayList<String> bash, String expr) {
        this(bash, expr, 1);
    }

    public stdIOStream(ArrayList<String> bash) {
        this(bash, "");
    }

    public stdIOStream(String expr) {
        this(new ArrayList<>(), expr);
    }

    public stdIOStream() {
        this("");
    }

    public static stdIOStream empty() {
        return new stdIOStream();
    }

    public static stdIOStream from(String expr) {
        return new stdIOStream(expr);
    }

    public static stdIOStream from(ArrayList<String> bash) {
        return new stdIOStream(bash);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String line : bash) {
            sb.append(line).append("\n");
        }
        sb.append(expr);
        return sb.toString();
    }

    public ArrayList<String> toStringArray() {
        ArrayList<String> result = new ArrayList<>(bash);
        result.add(expr);
        result.removeIf(String::isEmpty);
        return result;
    }
}
