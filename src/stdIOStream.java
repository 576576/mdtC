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

    public stdIOStream(stdIOStream stream, int stat) {
        this(stream.bash, stream.expr, stream.stat + stat);
    }

    public stdIOStream() {
        this("");
    }

    public stdIOStream(String expr, int stat) {
        this(new ArrayList<>(), expr, stat);
    }

    public stdIOStream(ArrayList<String> bash, int stat) {
        this(bash, "", stat);
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

    public static stdIOStream from(String expr, int stat) {
        return new stdIOStream(expr, stat);
    }

    public static stdIOStream from(ArrayList<String> bash, int stat) {
        return new stdIOStream(bash, stat);
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
