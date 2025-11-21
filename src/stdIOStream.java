import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public stdIOStream(String expr, int stat) {
        this(new ArrayList<>(), expr, stat);
    }

    public stdIOStream(ArrayList<String> bash, int stat) {
        this(bash, "", stat);
    }

    public static stdIOStream of() {
        return new stdIOStream();
    }

    public static stdIOStream of(String expr) {
        return new stdIOStream(expr);
    }

    public static stdIOStream of(ArrayList<String> bash) {
        return new stdIOStream(bash);
    }

    public static stdIOStream of(String expr, int stat) {
        return new stdIOStream(expr, stat);
    }


    public static stdIOStream of(ArrayList<String> bash, int stat) {
        return new stdIOStream(bash, stat);
    }

    public static stdIOStream of(ArrayList<String> bash, String expr) {
        return new stdIOStream(bash, expr);
    }

    public static stdIOStream of(ArrayList<String> bash, String expr, int stat) {
        return new stdIOStream(bash, expr, stat);
    }

    public static stdIOStream of(String[] bash) {
        return new stdIOStream(new ArrayList<>(List.of(bash)));
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

    public String toPlainString() {
        return bash.stream().collect(Collectors.joining("", "", expr));
    }

    public ArrayList<String> toStringArray() {
        ArrayList<String> result = new ArrayList<>(bash);
        result.add(expr);
        result.removeIf(String::isEmpty);
        return result;
    }
}
