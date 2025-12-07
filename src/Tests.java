import java.util.List;

public class Tests {
    /**
     * 集合的部分测试类
     */
    static void main() {
//        println(Utils.bracketPartSplit("7,min(8,9)"));
//        println(CodeCompiler.convertCodeLine(
//                stdCodeStream.of("s=min(2+3,5)")));
        println(Utils.stringSplit("(4<<6)-(7&2)"));
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
