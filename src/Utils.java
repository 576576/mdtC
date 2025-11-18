import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Utils {
    final static String[] dotCtrlCodes = {".ctrl(", ".enable(", ".config(", ".color(", ".shoot(",
            ".ulocate(", ".unpack(", ".pflush(", ".dflush(", ".write("};
    final static String[] ctrlCodes = {"print(", "printchar(", "format(", "wait(", "stop(",
            "end(", "ubind(", "uctrl(", "jump(", "jump2(", "printf("};
    final static Map<String, Integer> operatorOffsetMap = new HashMap<>() {{
        put("op", 2);
        put("sensor", 1);
        put("getlink", 1);
        put("radar", 7);
        put("uradar", 7);
        put("lookup", 2);
        put("packcolor", 1);
        put("read", 1);
    }};
    final static Map<String, String> operatorReverseMap = new HashMap<>() {{
        put("equal", "notEqual");
        put("notEqual", "equal");
        put("lessThan", "greaterThanEq");
        put("lessThanEq", "greaterThan");
        put("greaterThan", "lessThanEq");
        put("greaterThanEq", "lessThan");
        put("strictEqual", "notEqual");
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

    static void main() {

    }

    static String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            printError("Unable to read file." + e.getMessage());
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

    static void openWithSystemExplorer(String filePath) {
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

    static void formatFile(String filePath, String outPath) {
        String inputContent = readFile(filePath);

        String convertedContent = MdtcFormater.convertToFormat(inputContent);
        if (convertedContent.isEmpty()) {
            IO.println("Nothing to format.");
            return;
        }

        writeFile(outPath, convertedContent);
        IO.println("Formatted output at:\n> " + outPath);
    }

    static void convertFile(String filePath, String outPath) {
        if (Main.isToFormat) formatFile(filePath, filePath);

        String inputContent = readFile(filePath);

        stdIOStream convertedContent = MdtcConverter.convertCodeBlock(inputContent);
        String outContent = convertedContent.toString();

        writeFile(outPath, outContent);
        IO.println("Compiled output at:\n> " + outPath);

        if (Main.isOpenOutput) openWithSystemExplorer(outPath);
    }

    static void convertFileReverse(String filePath, String outPath) {
        String inputContent = readFile(filePath);

        stdIOStream convertedContent = MdtcConverterReverse.convertCodeBlock(inputContent);
        String outContent = convertedContent.toString();

        writeFile(outPath, outContent);
        IO.println("Reversed output at:\n> " + outPath);

        if (Main.isToFormat) formatFile(outPath, outPath);
        if (Main.isOpenOutput) openWithSystemExplorer(outPath);
    }

    static String padParams(String[] params, int paramNum) {
        if (params.length > paramNum) params = Arrays.copyOf(params, paramNum);
        while (params.length < paramNum) {
            params = Arrays.copyOf(params, params.length + 1);
            params[params.length - 1] = "0";
        }
        return String.join(" ", params);
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
    public static String[] stringSplit(String str) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
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
        return tokens.toArray(String[]::new);
    }

    static String[] stringSplitPro(String str) {
        if (str.isEmpty()) return new String[0];
        final String[] keysFormer = {".sensor", ".read"}, keysMiddle = {",", ";"};
        var stringSplit = stringSplit(str);
        ArrayList<String> tokens = new ArrayList<>(List.of(stringSplit));
        String tokenFirst = tokens.getFirst();
        if (tokenFirst.startsWith("::") && !tokenFirst.equals("::")) {
            tokens.set(0, "::");
            tokens.add(1, tokenFirst.substring(2));
        }
        for (int i = 1; i < tokens.size(); i++) {
            String token_now = tokens.get(i), token_former, token_middle;
            if (token_now.startsWith("::") && !token_now.equals("::")) {
                tokens.set(i, "::");
                tokens.add(i + 1, token_now.substring(2));
            }
            if (token_now.equals("(")) {
                token_former = tokens.get(i - 1);
                for (var key : keysFormer) {
                    if (token_former.endsWith(key) && !token_former.equals(key)) {
                        tokens.remove(i - 1);
                        tokens.add(i - 1, key);
                        tokens.add(i - 1, token_former.substring(0, token_former.indexOf(key)));
                        i++;
                        break;
                    }
                }
                token_middle = tokens.get(i + 1);
                for (var key : keysMiddle) {
                    String[] tokenSplit = token_middle.split(key);
                    if (tokenSplit.length < 2) continue;
                    tokens.remove(i + 1);
                    for (int k = 0; k < tokenSplit.length; k++) {
                        tokens.add(i + 1 + k * 2, tokenSplit[k]);
                        tokens.add(i + 2 + k * 2, key);
                    }
                    tokens.remove(i + tokenSplit.length * 2);
                    i = i + tokenSplit.length * 2 - 1;
                }
            }
        }
        return tokens.toArray(String[]::new);
    }

    static String stringOf(String[] arr) {
        return Arrays.stream(arr).reduce("", (a, b) -> a + b);
    }

    static String listToCodeBlock(ArrayList<String> bashList) {
        return bashList.stream().reduce("", (a, b) -> a + "\n" + b);
    }

    /**
     * 转换所有已声明变量和函数内标签到保留变量名和标签
     */
    static String replaceReserve(String s, ArrayList<String> reserveVars, ArrayList<String> reserveTags) {
        var splitList = stringSplitPro(s);
        for (int i = 0; i < splitList.length; i++) {
            for (int j = 0; j < reserveVars.size(); j++) {
                var key = reserveVars.get(j);
                if (splitList[i].equals(key))
                    splitList[i] = "ARG." + j;
            }
            for (int j = 0; j < reserveTags.size(); j++) {
                var key = reserveTags.get(j);
                if (splitList[i].equals(key))
                    splitList[i] = "PRESERVE_TAG." + j;
            }
        }
        return stringOf(splitList);
    }

    static boolean isSpecialControl(String codeLine) {
        String[] keys = new String[]{"::", "}", "do{", "for(", "if("};
        for (String key : keys) {
            if (codeLine.startsWith(key)) return true;
        }
        return false;
    }

    static int getEndBracket(String expr, int start) {
        Stack<Integer> stack = new Stack<>();
        for (int i = start; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') {
                stack.push(i);
            } else if (expr.charAt(i) == ')') {
                if (stack.isEmpty()) return -1; // Mismatched closing bracket
                stack.pop();
                if (stack.isEmpty()) return i; // Found the matching closing bracket
            }
        }
        return -1; // No matching closing bracket found
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

    /**
     * 将中置表达式转为逆波兰表达式
     *
     * @return 逆波兰表达式数组
     */
    public static String[] generateRpn(String str) {
        return rpn(new Stack<>(), stringSplit(str));
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
