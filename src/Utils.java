import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Utils {
    static String readFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.readString(path);
        } catch (FileNotFoundException e) {
            try {
                Files.createFile(path);
            } catch (IOException ex) {
                printError("Unable to create file: " + ex.getMessage());
            }
        } catch (IOException e) {
            printError("Unable to read file: " + e.getMessage());
        }
        return "";
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
        return params[1] + Constants.operatorValueMap.get(params[0]) + params[2];
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
    public static List<String> stringSplit(String str) {
        final List<Character> keysSplit = List.of(',', ';');
        final Set<String> reducedOpSet = Constants.reducedOpSet;
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
            for (Constants.Operator o : Constants.Operator.values()) {
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

        for (int i = 1; i < tokens.size(); i++) {
            if ((i == 1 || reducedOpSet.contains(tokens.get(i - 2))) && tokens.get(i - 1).equals("-")) {
                if (isNumeric(tokens.get(i))) {
                    tokens.set(i, "-" + tokens.get(i));
                    tokens.remove(i - 1);
                    if (i > 2 && i < tokens.size() &&
                            tokens.get(i - 2).equals("(") && tokens.get(i).equals(")")) {
                        tokens.remove(i - 2);
                        tokens.remove(i - 1);
                    }
                } else {
                    tokens.add(i - 1, "0");
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
                for (var key : Constants.dotCodes.stream().map(s -> s.substring(0, s.length() - 1)).toList()) {
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
        for (var op : Constants.operatorReverseMap.entrySet())
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
            if (Constants.Operator.isOperator(token)) {
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
                    while (!operators.empty() && !operators.peek().equals("(") && Constants.Operator.cmp(token, operators.peek()) <= 0) {
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
        int p0 = Constants.Operator.getPriority(op0);
        for (String op : ops) {
            int p = Constants.Operator.getPriority(op);
            if (p >= p0) return true;
        }
        return false;
    }
}
