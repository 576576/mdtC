import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    final static String[] dotCtrlCodes = {".ctrl(", ".enable(", ".config(", ".color(", ".shoot(",
            ".ulocate(", ".unpack(", ".pflush(", ".dflush(", ".write("};
    final static String[] ctrlCodes = {"print(", "printchar(", "format(", "wait(", "stop(",
            "end(", "ubind(", "uctrl(", "ushoot(", "jump(", "jump2(", "printf(", "tag("};
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
    }};
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    static void main() {
        IO.println(bracketPartSplit("7,min(8,9)"));
    }

    static String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            printError("Unable to read file: " + e.getMessage());
            return "";
        }
    }

    static void writeFile(String filePath, String content) {
        try {
            Files.writeString(Paths.get(filePath), content);
        } catch (IOException e) {
            printError("Unable to write file." + e.getMessage());
        }
    }

    static void openWithExplorer(String filePath) {
        File directory = new File(filePath);
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(directory);
            } catch (IOException e) {
                printError("Unable to open directory." + e.getMessage());
            }
        }
    }

    static String padParams(String[] params, int paramNum, String defaultParam) {
        params = Arrays.copyOf(params, paramNum);
        for (int i = 0; i < params.length; i++)
            if (params[i] == null) params[i] = defaultParam;
        return String.join(" ", params);
    }

    static String padParams(String[] params, int paramNum) {
        return padParams(params, paramNum, "0");
    }

    static String padParams(String paramString, int paramNum) {
        String[] params = paramString.split(" ");
        return padParams(params, paramNum);
    }

    static boolean isDotCtrlCode(String codeLine) {
        for (String command : dotCtrlCodes) if (codeLine.contains(command)) return true;
        return false;
    }

    static boolean isCtrlCode(String codeLine) {
        for (String command : ctrlCodes) if (codeLine.startsWith(command)) return true;
        return false;
    }

    static void printError(String message) {
        System.err.println("\u001B[31m" + message + "\u001B[0m");
    }

    static int getEndDotCtrl(String expr, int start) {
        int end = getEndBracket(expr, start);
        while (end < expr.length() - 1 && expr.charAt(end + 1) == '.') {
            if (expr.startsWith(".^", end + 1)) return end;
            for (String key : dotCtrlCodes) {
                if (expr.startsWith(key, end + 1)) return end;
            }
            int endNext = getEndBracket(expr, end + 1);
            if (endNext != -1) end = endNext;
            else return end;
        }
        return end;
    }

    static String getDotCtrlBlock(String expr) {
        int index = expr.length() - 1;
        for (String key : dotCtrlCodes) {
            int keyIndex = expr.indexOf(key);
            if (keyIndex != -1 && keyIndex < index) index = keyIndex;
        }
        return expr.substring(0, index);
    }

    /**
     * 预处理字符串,分离变量和运算符
     *
     * @return 变量分离的字符串数组
     */
    public static List<String> stringSplit(String str) {
        final List<Character> keysSplit = List.of(',', ';');
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (keysSplit.contains(c)) {
                tokens.add(token.toString().trim());
                token = new StringBuilder();
                tokens.add(c + "");
                continue;
            }
            boolean isOperator = false;
            for (Operator o : Operator.values()) {
                if (str.startsWith(o.value, i)) {
                    if (!token.toString().trim().isEmpty()) {
                        tokens.add(token.toString().trim());
                        token = new StringBuilder();
                    }
                    tokens.add(o.value);
                    i += o.value.length() - 1; // Skip the length of the operator
                    isOperator = true;
                    break;
                }
            }
            if (!isOperator) token.append(c);
        }
        if (!token.toString().trim().isEmpty())
            tokens.add(token.toString().trim());

        for (int i = 2; i < tokens.size() - 1; i++) {
            if (tokens.get(i - 2).equals("(") && tokens.get(i - 1).equals("-") && isNumeric(tokens.get(i)) && tokens.get(i + 1).equals(")")) {
                tokens.set(i - 2, "-" + tokens.get(i));
                tokens.remove(i - 1);
                tokens.remove(i - 1);
                tokens.remove(i - 1);
                i += 2;
            }
        }
        return tokens;
    }

    static List<String> bracketPartSplit(String str) {
        final List<String> keysSplit = List.of(",", ";");
        if (str.isEmpty()) return List.of();
        List<String> tokens = new ArrayList<>();
        List<String> splitList = stringSplit(str);
        int matchIndex = 0;
        StringBuilder token = new StringBuilder();
        for (String part : splitList) {
            if (matchIndex == 0 && keysSplit.contains(part)) {
                tokens.add(token.toString().trim());
                token = new StringBuilder();
                continue;
            } else if (part.equals("(")) matchIndex++;
            else if (part.equals(")")) matchIndex--;
            token.append(part);
        }
        if (!token.isEmpty()) tokens.add(token.toString().trim());
        return tokens;
    }

    static List<String> stringSplitPro(String str) {
        if (str.isEmpty()) return List.of();
        final String[] keysDot = {".sensor", ".read"};
        List<String> tokens = stringSplit(str);
        String tokenFirst = tokens.getFirst();
        if (tokenFirst.startsWith("::") && !tokenFirst.equals("::")) {
            tokens.set(0, "::");
            tokens.add(1, tokenFirst.substring(2));
        }
        for (int i = 1; i < tokens.size(); i++) {
            String token_now = tokens.get(i), token_dot;
            if (token_now.startsWith("::") && !token_now.equals("::")) {
                tokens.set(i, "::");
                tokens.add(i + 1, token_now.substring(2));
            }
            if (token_now.equals("(")) {
                token_dot = tokens.get(i - 1);
                for (var key : keysDot) {
                    if (token_dot.endsWith(key) && !token_dot.equals(key)) {
                        tokens.remove(i - 1);
                        tokens.add(i - 1, key);
                        tokens.add(i - 1, token_dot.substring(0, token_dot.indexOf(key)));
                        i++;
                        break;
                    }
                }
            }
        }
        return tokens;
    }

    static String stringOf(List<String> list) {
        return list.stream().reduce("", (a, b) -> a + b);
    }

    static String listToCodeBlock(ArrayList<String> bashList) {
        return bashList.stream().reduce("", (a, b) -> a + "\n" + b).trim();
    }

    /**
     * 转换所有已声明变量到保留变量名
     */
    static String replaceVars(String s, List<String> varsList, List<String> replaceToList) {
        if (varsList.size() != replaceToList.size()) {
            Utils.printError("Unable to deal with " + varsList + replaceToList);
        }
        List<String> splitList = stringSplitPro(s);
        for (int i = 0; i < splitList.size(); i++) {
            for (int j = 0; j < varsList.size(); j++) {
                if (splitList.get(i).equals(varsList.get(j)))
                    splitList.set(i, replaceToList.get(j));
            }
        }
        return stringOf(splitList);
    }

    static String replaceVar(String s, String var, String replaceToVar) {
        List<String> splitList = stringSplitPro(s);
        for (int i = 0; i < splitList.size(); i++) {
            if (splitList.get(i).equals(var))
                splitList.set(i, replaceToVar);
        }
        return stringOf(splitList);
    }

    /**
     * 转换所有已声明标签到保留变量名和标签
     */
    static String replaceTags(String s, List<String> tagsList, String prefix) {
        final List<String> keyList = List.of("::", "jump");
        List<String> splitList = stringSplitPro(s);
        if (splitList.size() < 2 || !keyList.contains(splitList.getFirst())) return s;
        for (int i = 0; i < keyList.size(); i++) {
            if (splitList.getFirst().equals(keyList.get(i))) {
                String tag = splitList.get(i + 1);
                if (tagsList.contains(tag)) splitList.set(i + 1, prefix + tag);
            }
        }
        return stringOf(splitList);
    }

    static String reverseCondition(String codeLine) {
        for (var op : operatorReverseMap.entrySet())
            if (codeLine.contains(op.getKey()))
                return codeLine.replace(op.getKey(), op.getValue());
        return codeLine;
    }

    static boolean isSpecialControl(String codeLine) {
        String[] keys = new String[]{"::", "}", "do{", "for(", "if(", "else{"};
        for (String key : keys) {
            if (codeLine.startsWith(key)) return true;
        }
        return false;
    }

    static int getEndBracket(String expr, int start) {
        if (start < 0) return -1;
        Stack<Integer> stack = new Stack<>();
        for (int i = start; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') {
                stack.push(i);
            } else if (expr.charAt(i) == ')') {
                if (stack.isEmpty()) return -1;
                stack.pop();
                if (stack.isEmpty()) return i;
            }
        }
        return -1;
    }

    static String getCondition(String codeLine) {
        final String defaultCondition = "always 0 0";
        String[] params = codeLine.split(" ");
        if (params.length == 0) return defaultCondition;
        String key = params[0];
        if (key.equals("op")) {
            if (!operatorReverseMap.containsKey((params[1]))) {
                String target = params[operatorOffsetMap.get(key)];
                return String.join(" ", "notEqual", target, "0");
            }
            return String.join(" ", params[1], params[3], params[4]);
        } else if (operatorOffsetMap.containsKey(key)) {
            String target = params[operatorOffsetMap.get(key)];
            return String.join(" ", "notEqual", target, "0");
        }
        return defaultCondition;
    }

    static Map<String, String> getChainParams(String s) {
        String expr = ".main(" + s + ")";
        Map<String, String> paramsMap = new HashMap<>();
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '.') {
                int start = expr.indexOf("(", i), end = getEndBracket(expr, start);
                String key = expr.substring(i + 1, start);
                String value = expr.substring(start + 1, end).trim();
                if (!value.isEmpty()) paramsMap.put(key, value);
                i = end;
            }
        }
        return paramsMap;
    }

    public static boolean isNumeric(String str) {
        return str != null && NUMBER_PATTERN.matcher(str).matches();
    }

    /**
     * 将中置表达式转为逆波兰表达式
     *
     * @return 逆波兰表达式数组
     */
    public static String[] generateRpn(String str) {
        return rpn(new Stack<>(), stringSplit(str).toArray(String[]::new));
    }

    public static String[] rpn(Stack<String> operators, String[] tokens) {
        ArrayList<String> output = new ArrayList<>();
        for (String token : tokens) {
            if (Operator.isOperator(token)) {
                if (token.equals("(")) {
                    operators.push(token);
                } else if (token.equals(")")) {
                    while (!operators.empty() && !operators.peek().equals("(")) {
                        output.add(operators.pop());
                    }
                    if (!operators.empty() && operators.peek().equals("(")) {
                        operators.pop();
                    }
                } else {
                    while (!operators.empty() && !operators.peek().equals("(") && Operator.cmp(token, operators.peek()) <= 0) {
                        output.add(operators.pop());
                    }
                    operators.push(token);
                }
            } else {
                output.add(token);
            }
        }

        // 遍历结束，将运算符栈全部压入output
        while (!operators.empty()) {
            if (!operators.peek().equals("(")) {
                output.add(operators.pop());
            } else {
                operators.pop(); // Remove any remaining left parentheses
            }
        }
        return output.toArray(new String[0]);
    }

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
    }
}
