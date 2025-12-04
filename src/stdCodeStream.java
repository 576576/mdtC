import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record stdCodeStream(ArrayList<String> bash, String expr, int stat) {
    public stdCodeStream(ArrayList<String> bash, String expr) {
        this(bash, expr, 1);
    }

    public stdCodeStream(ArrayList<String> bash) {
        this(bash, "");
    }

    public stdCodeStream(String expr) {
        this(new ArrayList<>(), expr);
    }

    public stdCodeStream() {
        this("");
    }

    public stdCodeStream(String expr, int stat) {
        this(new ArrayList<>(), expr, stat);
    }

    public stdCodeStream(ArrayList<String> bash, int stat) {
        this(bash, "", stat);
    }

    public static stdCodeStream of() {
        return new stdCodeStream();
    }

    public static stdCodeStream of(String expr) {
        return new stdCodeStream(expr);
    }

    public static stdCodeStream of(ArrayList<String> bash) {
        return new stdCodeStream(bash);
    }

    public static stdCodeStream of(String expr, int stat) {
        return new stdCodeStream(expr, stat);
    }


    public static stdCodeStream of(ArrayList<String> bash, int stat) {
        return new stdCodeStream(bash, stat);
    }

    public static stdCodeStream of(ArrayList<String> bash, String expr) {
        return new stdCodeStream(bash, expr);
    }

    public static stdCodeStream of(ArrayList<String> bash, String expr, int stat) {
        return new stdCodeStream(bash, expr, stat);
    }

    public static stdCodeStream of(String[] bash) {
        return new stdCodeStream(new ArrayList<>(List.of(bash)));
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
