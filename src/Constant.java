import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Constant {
    final static Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");
    final static List<String> dotCtrlCodes = List.of(".ctrl(", ".enable(", ".config(", ".color(", ".shoot(",
            ".ulocate(", ".unpack(", ".pflush(", ".dflush(", ".write(");
    final static List<String> dotCodes = List.of(".sensor(", ".read(", ".orElse(");
    final static List<String> ctrlCodes = List.of("print(", "printchar(", "format(", "wait(", "stop(",
            "end(", "ubind(", "uctrl(", "ushoot(", "jump(", "jump2(", "printf(", "tag(", "raw(");
    final static Map<String, Integer> operatorOffsetMap = new HashMap<>() {{
        put("op", 2);
        put("sensor", 1);
        put("getlink", 1);
        put("radar", 7);
        put("uradar", 7);
        put("lookup", 2);
        put("packcolor", 1);
        put("read", 1);
        put("set", 1);
        put("select", 1);
    }};
    final static Map<String, String> operatorReverseMap = new HashMap<>() {{
        put("equal", "notEqual");
        put("strictEqual", "notEqual");
        put("always", "notEqual");
        put("notEqual", "equal");
        put("lessThan", "greaterThanEq");
        put("lessThanEq", "greaterThan");
        put("greaterThan", "lessThanEq");
        put("greaterThanEq", "lessThan");
    }};
    final static Map<String, String> operatorKeyMap = new HashMap<>() {{
        put("+", "add");
        put("-", "sub");
        put("*", "mul");
        put("/", "div");
        put("//", "idiv");
        put("%", "mod");
        put("%%", "emod");
        put(".^", "pow");
        put("==", "equal");
        put("!=", "notEqual");
        put("&&", "land");
        put("<", "lessThan");
        put("<=", "lessThanEq");
        put(">", "greaterThan");
        put(">=", "greaterThanEq");
        put("===", "strictEqual");
        put("<<", "shl");
        put(">>", "shr");
        put(">>>", "ushr");
        put("|", "or");
        put("&", "and");
        put("^", "xor");
        put("=", "set");
        put("(", "lbracket");
        put(")", "rbracket");
    }};
    final static List<String> supportFormats = List.of(".mdtc", ".mdtcode", ".libmdtc");
}
