import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Utils {
    static String readFile(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path);
        } catch (Exception e) {
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

    static String formatParams(int paramNum, String[] params, String defaultParam, String delimiter) {
        params = Arrays.copyOf(params, paramNum);
        for (int i = 0; i < params.length; i++)
            if (params[i] == null) params[i] = defaultParam;
        return String.join(delimiter, params);
    }

    static String padParams(String defaultParam, int paramNum, String paramString) {
        return formatParams(paramNum, paramString.split(","), defaultParam, " ");
    }

    static String padParams(int paramNum, String... params) {
        return formatParams(paramNum, params, "0", " ");
    }

    static String padParams(int paramNum, String paramString) {
        return formatParams(paramNum, paramString.split(","), "0", " ");
    }

    static String reduceParams(String defaultParam, String paramString) {
        int dpLength = defaultParam.length();
        while (paramString.endsWith(defaultParam)) {
            paramString = paramString.substring(0, paramString.length() - dpLength).trim();
        }
        return paramString.replace(" ", ",");
    }

    static String reduceParams(String defaultParam, String... params) {
        String paramString = String.join(" ", params);
        return reduceParams(defaultParam, paramString);
    }

    static String reduceParams(String defaultParam, int paramNum, String... params) {
        params = Arrays.copyOf(params, paramNum);
        for (int i = 0; i < params.length; i++)
            if (params[i] == null) params[i] = defaultParam;
        return reduceParams(defaultParam, params);
    }

    static String reduceCondition(String condition) {
        String[] params = condition.split(" ", 3);
        String operator = params[0];
        if (operator.equals("always")) return "always";
        if (operator.equals("never")) return "never";
        return params[1] + Constants.midOpValueMap.get(operator) + params[2].trim();
    }

    static boolean isDotCtrlCode(String codeLine) {
        for (String command : Constants.dotCtrlCodes) if (codeLine.contains(command)) return true;
        return false;
    }

    static boolean isCtrlCode(String codeLine) {
        for (String command : Constants.ctrlCodes) if (codeLine.startsWith(command)) return true;
        return false;
    }

    static void printError(String message) {
        System.err.println("\u001B[31m" + message + "\u001B[0m");
    }

    static int getEndDotChain(String expr, int start) {
        int end = getEndBracket(expr, start);
        while (end < expr.length() - 1 && expr.charAt(end + 1) == '.') {
            if (expr.startsWith(".^", end + 1)) return end;
            for (String key : Constants.dotCtrlCodes) if (expr.startsWith(key, end + 1)) return end;
            for (String key : Constants.dotCodes) if (expr.startsWith(key, end + 1)) return end;

            int endNext = getEndBracket(expr, end + 1);
            if (endNext != -1) end = endNext;
            else return end;
        }
        return end;
    }

    static String getDotBlock(String expr) {
        int index = expr.length() - 1;
        for (String key : Constants.dotCtrlCodes) {
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
    static List<String> stringSplit(String str) {
        if (str.isEmpty()) return List.of();
        if (str.startsWith("::")) return List.of("::", str.substring(2));
        if (str.contains("::")) str = str.substring(0, str.indexOf("::"));

        final List<Character> keysSplit = List.of(',', ';');
        List<String> tokens = new ArrayList<>();
        StringBuilder tokenBuilder = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (keysSplit.contains(c)) {
                tokens.add(tokenBuilder.toString().trim());
                tokenBuilder = new StringBuilder();
                tokens.add(c + "");
                continue;
            }
            boolean isOperator = false;
            for (Constants.Operator o : Constants.Operator.values()) {
                if (str.startsWith(o.value, i)) {
                    if (!tokenBuilder.toString().trim().isEmpty()) {
                        tokens.add(tokenBuilder.toString().trim());
                        tokenBuilder = new StringBuilder();
                    }
                    tokens.add(o.value);
                    i += o.value.length() - 1;
                    isOperator = true;
                    break;
                }
            }
            if (!isOperator) tokenBuilder.append(c);
        }
        if (!tokenBuilder.toString().trim().isEmpty())
            tokens.add(tokenBuilder.toString().trim());

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.startsWith("-") && !isNumeric(token)) {
                List<String> tokenTo = List.of("(", "0", Constants.Operator.sub.value, token.substring(1), ")");
                tokens.remove(i);
                tokens.addAll(i, tokenTo);
                i += tokenTo.size() - 1;
            }
        }

        for (int i = 1; i < tokens.size() - 1; i++) {
            String token = tokens.get(i);
            tokenBuilder = new StringBuilder();

            if (token.equals("(")) {
                String tokenNow = tokens.get(i - 1);
                if (Constants.dotOpReduced.contains(tokenNow)) {
                    tokenBuilder.append(tokenNow).append(token);
                    int matchIndex = 0;
                    for (int j = i + 1; j < tokens.size(); j++) {
                        tokenNow = tokens.get(j);
                        if (tokenNow.equals("(")) matchIndex++;
                        if (tokenNow.equals(")")) matchIndex--;
                        if (matchIndex != 0) continue;
                        if (Constants.dotOpReduced.contains(tokenNow) || j == tokens.size() - 1) {
                            String tokenTo = stringOf(tokens.subList(i - 1, j));
                            tokens.subList(i - 1, j).clear();
                            tokens.add(i - 1, tokenTo);
                            break;
                        }
                    }
                }
            }
        }
        return tokens;
    }

    static List<String> bracketPartSplit(String str) {
        if (str.isEmpty()) return List.of();

        List<String> tokens = new ArrayList<>();
        List<String> splitList = stringSplit(str);
        int matchIndex = 0;
        StringBuilder token = new StringBuilder();
        for (String part : splitList) {
            if (matchIndex == 0 && ",;".contains(part)) {
                tokens.add(token.toString());
                token = new StringBuilder();
                continue;
            } else if (part.equals("(")) matchIndex++;
            else if (part.equals(")")) matchIndex--;
            token.append(part);
        }
        if (!token.isEmpty()) tokens.add(token.toString());
        tokens.replaceAll(String::trim);
        return tokens;
    }

    static String stringOf(List<String> list) {
        return list.stream().reduce("", (a, b) -> a + b);
    }

    static String stringBlockOf(List<String> bashList) {
        return bashList.stream().reduce("", (a, b) -> a + "\n" + b).trim();
    }

    /**
     * 转换所有已声明变量到保留变量名
     */
    static String replaceVars(String s, List<String> varsList, List<String> replaceToList) {
        if (varsList.size() != replaceToList.size()) {
            Utils.printError("Unable to deal with " + varsList + replaceToList);
        }
        List<String> splitList = stringSplit(s);
        for (int i = 0; i < splitList.size(); i++) {
            for (int j = 0; j < varsList.size(); j++) {
                if (splitList.get(i).equals(varsList.get(j)))
                    splitList.set(i, replaceToList.get(j));
            }
        }
        return stringOf(splitList);
    }

    static String replaceVar(String s, String var, String replaceToVar) {
        List<String> splitList = stringSplit(s);
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
        List<String> splitList = stringSplit(s);
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
        final Map<String, String> reMap = Constants.operatorReverseMap;
        String[] splitList = codeLine.split(" ");
        for (int i = 0; i < splitList.length; i++) {
            String part = splitList[i];
            if (reMap.containsKey(part)) splitList[i] = reMap.get(part);
        }
        return String.join(" ", splitList);
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
            if (!Constants.operatorReverseMap.containsKey((params[1]))) {
                String target = params[Constants.operatorOffsetMap.get(key)];
                return String.join(" ", "notEqual", target, "0");
            }
            return String.join(" ", params[1], params[3], params[4]);
        } else if (Constants.operatorOffsetMap.containsKey(key)) {
            String target = params[Constants.operatorOffsetMap.get(key)];
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
        return str != null && Constants.NUMBER_PATTERN.matcher(str).matches();
    }

    /**
     * 将中置表达式转为逆波兰表达式
     *
     * @return 逆波兰表达式数组
     */
    public static List<String> generateRpn(String str) {
        return rpn(stringSplit(str));
    }

    public static List<String> rpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        for (String token : tokens) {
            if (isOperator(token)) {
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
                    while (!operators.empty() && !operators.peek().equals("(") && cmp(token, operators.peek()) <= 0) {
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
        return output;
    }

    static void removeSpareTags(ArrayList<String> bashList) {
        for (int i = 0; i < bashList.size(); i++) {
            String line = bashList.get(i);
            if (line.startsWith("::")) {
                String tagTo = line.substring(2);
                if (bashList.stream().noneMatch(l -> l.startsWith("jump " + tagTo + " "))) {
                    bashList.remove(i);
                    i--;
                }
            }
        }
    }

    static boolean isLowPriority(String op0, String... ops) {
        int p0 = getPriority(op0);
        for (String op : ops) {
            int p = getPriority(op);
            if (p >= p0) return true;
        }
        return false;
    }

    /**
     * 比较两个符号的优先级
     *
     * @return c1的优先级是否比c2的高，高则返回正数，等于返回0，小于返回负数
     */
    public static int cmp(String c1, String c2) {
        int p1, p2;
        p1 = Constants.midOpPriorityMap.getOrDefault(c1, 0);
        p2 = Constants.midOpPriorityMap.getOrDefault(c2, 0);
        return p1 - p2;
    }

    /**
     * 枚举出来的才视为运算符，用于扩展
     *
     * @return 运算符合法性
     */
    public static boolean isOperator(String c) {
        return Constants.midOpPriorityMap.containsKey(c);
    }

    public static int getPriority(String c) {
        int priority = Constants.midOpPriorityMap.getOrDefault(c, 11);
        if (priority == 10) priority = 0;
        return priority;
    }
}
