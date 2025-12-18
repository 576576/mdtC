import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record stdCodeStream(ArrayList<String> bash, String expr, int stat) {
    private stdCodeStream(ArrayList<String> bash, String expr) {
        this(bash, expr, 1);
    }

    private stdCodeStream(ArrayList<String> bash, int stat) {
        this(bash, "", stat);
    }

    private stdCodeStream(ArrayList<String> bash) {
        this(bash, "");
    }

    private stdCodeStream(String expr, int stat) {
        this(new ArrayList<>(), expr, stat);
    }

    private stdCodeStream(String expr) {
        this(new ArrayList<>(), expr);
    }

    private stdCodeStream() {
        this("");
    }

    public static stdCodeStream of(ArrayList<String> bash, String expr, int stat) {
        return new stdCodeStream(bash, expr, stat);
    }

    public static stdCodeStream of(ArrayList<String> bash, String expr) {
        return new stdCodeStream(bash, expr);
    }

    public static stdCodeStream of(ArrayList<String> bash, int stat) {
        return new stdCodeStream(bash, stat);
    }

    public static stdCodeStream of(ArrayList<String> bash) {
        return new stdCodeStream(bash);
    }

    public static stdCodeStream of(String expr, int stat) {
        return new stdCodeStream(expr, stat);
    }

    public static stdCodeStream of(String expr) {
        return new stdCodeStream(expr);
    }

    public static stdCodeStream of() {
        return new stdCodeStream();
    }

    @Override
    public String toString() {
        return bash.stream().map(line -> line + "\n")
                .collect(Collectors.joining("", "", expr));
    }

    public List<String> toList() {
        List<String> result = new ArrayList<>(bash);
        result.add(expr);
        result.removeIf(String::isEmpty);
        return result;
    }
}
