import java.util.List;

public class Tests {
    /**
     * 集合的部分测试类
     */
    static void main() {
        println(Utils.stringSplit("x=1+(-1)-(-x0)"));
    }

    static String unfold(List<String> list) {
        return Utils.stringBlockOf(list) + "\n--------\n";
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
}
