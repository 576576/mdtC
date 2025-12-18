import java.util.List;

public class Tests {
    /**
     * 集合的部分测试类
     */
    static void main() {
        println(Utils.stringSplit("2>=3<3"));
        IO.println(Utils.isNumeric("-1"));
    }

    static String unfold(List<String> list) {
        return stringTableOf(list) + "\n--------\n";
    }

    static void println(String str) {
        IO.println(str + "\n--------\n");
    }

    static void println(List<String> list) {
        IO.println(unfold(list));
    }

    static void println(stdCodeStream stream) {
        IO.println(unfold(stream.toList()));
    }

    static String stringTableOf(List<String> bashList) {
        return bashList.stream().reduce("", (a, b) -> a + "\t" + b).trim();
    }
}
