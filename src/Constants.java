import java.util.*;
import java.util.regex.Pattern;

public class Constants {
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
    final static Map<String, String> operatorAliasMap = new HashMap<>() {{
        put("log10", "lg");
        put("log", "ln");
        put("logn", "log");
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
    final static Map<String, String> operatorValueMap = new HashMap<>() {{
        put("add", "+");
        put("sub", "-");
        put("mul", "*");
        put("div", "/");
        put("idiv", "//");
        put("mod", "%");
        put("emod", "%%");
        put("pow", ".^");
        put("equal", "==");
        put("notEqual", "!=");
        put("land", "&&");
        put("lessThan", "<");
        put("lessThanEq", "<=");
        put("greaterThan", ">");
        put("greaterThanEq", ">=");
        put("strictEqual", "===");
        put("shl", "<<");
        put("shr", ">>");
        put("ushr", ">>>");
        put("or", "|");
        put("and", "&");
        put("xor", "^");
        put("set", "=");
        put("lbracket", "(");
        put("rbracket", ")");
        put("always", "==");
    }};
    final static Set<String> reducedOpSet = new HashSet<>(Constants.operatorKeyMap.keySet()) {{
        List.of("(", ")", "=").forEach(this::remove);
    }};

    final static List<String> supportFormats = List.of(".mdtc", ".mdtcode", ".libmdtc");

    /**
     * 处理运算符的优先级
     * 括号最高, 指数次之, 乘除位移再次之, 加减再次之, 比较垫底
     *
     */
    enum Operator {
        ADD("+", 4), SUBTRACT("-", 4),
        MULTIPLY("*", 5), INTEGER_DIVIDE("//", 5),
        DIVIDE("/", 5), PERCENTAGE_MODULO("%%", 5),
        MODULO("%", 5), EXPONENT(".^", 7),
        STRICT_EQUALS("===", 3), EQUALS("==", 3),
        NOT_EQUALS("!=", 3), LOGICAL_AND("&&", 2),
        GREATER_THAN_OR_EQUAL_TO(">=", 3), LESS_THAN_OR_EQUAL_TO("<=", 3),
        UNSIGNED_RIGHT_SHIFT(">>>", 5), RIGHT_SHIFT(">>", 5),
        LEFT_SHIFT("<<", 5), BITWISE_XOR("^", 2),
        GREATER_THAN(">", 3), LESS_THAN("<", 3),
        AND("&", 2), OR("|", 2),
        LEFT_BRACKET("(", 10), RIGHT_BRACKET(")", 10),
        ASSIGN("=", 1);


        final String value;
        final int priority;

        Operator(String value, int priority) {
            this.value = value;
            this.priority = priority;
        }

        /**
         * 比较两个符号的优先级
         *
         * @return c1的优先级是否比c2的高，高则返回正数，等于返回0，小于返回负数
         */
        public static int cmp(String c1, String c2) {
            int p1 = 0, p2 = 0;
            for (Operator o : Operator.values()) {
                if (o.value.equals(c1)) p1 = o.priority;
                if (o.value.equals(c2)) p2 = o.priority;
            }
            return p1 - p2;
        }

        /**
         * 枚举出来的才视为运算符，用于扩展
         *
         * @return 运算符合法性
         */
        public static boolean isOperator(String c) {
            for (Operator o : Operator.values()) {
                if (o.value.equals(c)) return true;
            }
            return false;
        }

        public static int getPriority(String c) {
            for (Operator o : Operator.values()) {
                if (o.value.equals(c)) return o.priority == 10 ? 0 : o.priority;
            }
            return 11;
        }
    }
}
