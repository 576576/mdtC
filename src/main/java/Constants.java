import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Constants {
    final static String trueCondition = "always 0 0", falseCondition = "notEqual 0 0";
    final static Pattern NUMBER_PATTERN = Pattern.compile("^([-+])?\\d+(\\.\\d+)?$");
    final static List<String> dotCtrlCodes = List.of(".ctrl(", ".enable(", ".config(", ".color(", ".shoot(",
            ".ulocate(", ".unpack(", ".pflush(", ".dflush(", ".write(");
    final static List<String> dotCodes = List.of(".sensor(", ".read(", ".orElse(");
    final static List<String> dotCodesAll = Stream.concat(dotCtrlCodes.stream(), dotCodes.stream())
            .collect(Collectors.toList());
    final static List<String> dotOpReduced = dotCodesAll.stream().map(s -> s.substring(0, s.length() - 1)).toList();
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
        put("notEqual", "equal");
        put("equal", "notEqual");
        put("strictEqual", "notEqual");
        put("lessThan", "greaterThanEq");
        put("lessThanEq", "greaterThan");
        put("greaterThan", "lessThanEq");
        put("greaterThanEq", "lessThan");
        put("always", "never");
        put("never", "always");
    }};
    final static BiMap<String, String> midOpKeysMap = HashBiMap.create(new HashMap<>() {{
        for (int i = 0; i < Operator.values().length; i++) {
            Operator o = Operator.values()[i];
            put(o.value, o.name());
        }
    }});
    final static BiMap<String, String> midOpValueMap = midOpKeysMap.inverse();
    final static Map<String, Integer> midOpPriorityMap = new HashMap<>() {{
        for (int i = 0; i < Operator.values().length; i++) {
            Operator o = Operator.values()[i];
            put(o.value, o.priority);
        }
    }};
    final static List<String> supportFormats = List.of(".mdtc", ".mdtcode", ".libmdtc");

    enum Operator {
        add("+", 4), sub(".-", 4),
        mul("*", 5), idiv("//", 5),
        div("/", 5), emod("%%", 5),
        mod(".%", 5), pow(".^", 7),
        strictEqual("===", 3), equal("==", 3),
        notEqual("!=", 3), land("&&", 2),
        greaterThanEq(">=", 3), lessThanEq("<=", 3),
        ushr(">>>", 5), shr(">>", 5),
        shl("<<", 5), xor("^", 2),
        greaterThan(">", 3), lessThan("<", 3),
        and("&", 2), or("|", 2),
        lbracket("(", 10), rbracket(")", 10),
        set("=", 1), always("always", 1), never("never", 1);
        final String value;
        final int priority;

        Operator(String value, int priority) {
            this.value = value;
            this.priority = priority;
        }
    }
}
