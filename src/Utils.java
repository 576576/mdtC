import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Utils {
    public static String filePathAbs;

    static void main() {
        String str = "c.ulocate()";
        IO.println(getDotCtrlBlock(str));

    }

    static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        File file = new File(filePath);
        filePathAbs = file.getAbsolutePath();
        if (!file.exists()) {
            try {
                if (file.createNewFile()) file = new File(filePath);
            } catch (IOException _) {
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + e.getMessage());
        }
        return content.toString();
    }

    static void writeFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException _) {
        }
    }

    static void openWithSystemExplorer(String filePath) {
        File directory = new File(filePath);
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(directory);
                } else {
                    System.out.println("打开目录的操作不受支持");
                }
            } catch (IOException e) {
                System.err.println("无法打开目录: " + e.getMessage());
            }
        } else {
            System.out.println("Desktop不受支持");
        }
    }

    static void formatFile() {
        IO.println("MdtC Compiler v" + Main.versionTag);
        String filePath = "";
        if (!Main.isDebug) filePath = IO.readln("Enter .mdtc file path below.\n> ");
        if (filePath.isEmpty()) {
            filePath = Main.fileDefault;
            IO.println("Using file> " + filePath);
        }
        String inputContent = readFile(filePath);

        String convertedContent = MdtcToFormat.convertToFormat(inputContent);
        if (convertedContent.isEmpty()) {
            IO.println("Nothing is formatted.");
            return;
        }
        writeFile(filePath, convertedContent);
        IO.println("Format successfully.");
    }

    static void convertFile() {
        IO.println("MdtC Compiler v" + Main.versionTag);
        String filePath = "";
        if (!Main.isDebug) filePath = IO.readln("Enter .mdtc file path below.\n> ");
        if (filePath.isEmpty()) {
            filePath = Main.fileDefault;
            IO.println("Using file> " + filePath);
        }
        String inputContent = readFile(filePath);

        stdIOStream convertedContent = MindustryFileConverter.convertCodeBlock(inputContent);
        String outputPath = filePath.replace(".mdtc", ".mdtcode");
        String outputContent = convertedContent.toString();
        if (Main.formatOnExecute) outputContent = MdtcToFormat.convertToFormat(outputContent);

        writeFile(outputPath, outputContent);
        System.out.println("Compile output at:\n" + outputPath);
        if (Main.openAfterCompile)
            openWithSystemExplorer(outputPath);
    }

    static Map<String, Integer> operatorOffsetMap() {
        Map<String, Integer> keywordMap = new HashMap<>();
        keywordMap.put("op", 2);
        keywordMap.put("sensor", 1);
        keywordMap.put("getlink", 1);
        keywordMap.put("radar", 7);
        keywordMap.put("uradar", 7);
        keywordMap.put("lookup", 2);
        keywordMap.put("packcolor", 1);
        keywordMap.put("read", 1);
        return keywordMap;
    }

    static Map<String, String> operatorReverseMap() {
        Map<String, String> keywordMap = new HashMap<>();
        keywordMap.put("equal", "notEqual");
        keywordMap.put("notEqual", "equal");
        keywordMap.put("lessThan", "greaterThanEq");
        keywordMap.put("lessThanEq", "greaterThan");
        keywordMap.put("greaterThan", "lessThanEq");
        keywordMap.put("greaterThanEq", "lessThan");
        keywordMap.put("strictEqual", "notEqual");
        return keywordMap;
    }


    static Map<String, String> operatorKeyMap() {
        Map<String, String> keywordMap = new HashMap<>();
        keywordMap.put("+", "add");
        keywordMap.put("-", "sub");
        keywordMap.put("*", "mul");
        keywordMap.put("/", "div");
        keywordMap.put("//", "idiv");
        keywordMap.put("%", "mod");
        keywordMap.put("%%", "emod");
        keywordMap.put(".^", "pow");
        keywordMap.put("==", "equal");
        keywordMap.put("!=", "notEqual");
        keywordMap.put("&&", "land");
        keywordMap.put("<", "lessThan");
        keywordMap.put("<=", "lessThanEq");
        keywordMap.put(">", "greaterThan");
        keywordMap.put(">=", "greaterThanEq");
        keywordMap.put("===", "strictEqual");
        keywordMap.put("<<", "shl");
        keywordMap.put(">>", "shr");
        keywordMap.put(">>>", "ushr");
        keywordMap.put("|", "or");
        keywordMap.put("&", "and");
        keywordMap.put("^", "xor");
        keywordMap.put("=", "set");
        return keywordMap;
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
        final String[] keys = {".ctrl", ".enable", ".config", ".color", ".shoot", ".ulocate", ".unpack", ".pflush", ".dflush", ".write"};
        for (String command : keys) if (codeLine.contains(command)) return true;
        return false;
    }

    static boolean isCtrlCode(String codeLine) {
        String[] keys = {"print(", "printchar(", "format(", "wait(", "stop(", "end(", "ubind(", "uctrl(", "jump(", "jump2(", "printf("};
        for (String command : keys) if (codeLine.startsWith(command)) return true;
        return false;
    }

    static void printRedError(String message) {
        System.err.println("\u001B[31m" + message + "\u001B[0m");
    }

    static int getEndDotCtrl(String expr, int start) {
        int end = getEndBracket(expr, start);
        if (end >= expr.length() - 1 || expr.charAt(end + 1) != '.') return end;
        final String[] keys = {"ctrl(", "enable(", "config(", "color(", "shoot(", "ulocate(", "unpack(", "pflush(", "dflush(", "write("};
        while (end < expr.length() - 1 && expr.charAt(end + 1) == '.') {
            for (String key : keys) {
                if (expr.startsWith(key, end + 2)) return end;
            }
            int endNext = getEndBracket(expr, end + 2);
            if (endNext != -1) end = endNext;
            else return end;
        }
        return end;
    }

    static String getDotCtrlBlock(String expr) {
        String block;
        int index = expr.length() - 1;
        final String[] keys = {"ctrl(", "enable(", "config(", "color(", "shoot(", "ulocate(", "unpack(", "pflush(", "dflush(", "write("};
        for (String key : keys) {
            int keyIndex = expr.indexOf(key);
            if (keyIndex < index && keyIndex > 0) index = keyIndex;
        }
        block = expr.substring(0, index - 1);
        return block;
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

    /**
     * 将函数块分离到函数,暂存于funcMap
     * {@code funcMap}结构: key:函数名, bash:函数体, expr:返回量, stat:标签数
     */
    static void convertFunc(String funcBlock) {
        final String keyStart = "function", keyEnd = "}";
        final String[] keysJump = {"do{", "for(", "if("};
        ArrayList<String> bashList = new ArrayList<>(List.of(funcBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();
        int matchIndex = 0;
        String funcName = "", returnValue = "";
        String funcArgs = "";
        ArrayList<String> preserveVars, preserveTags;
        for (String bash : bashList) {
            if (bash.startsWith(keyEnd)) {
                matchIndex--;
                if (matchIndex == 0) {
                    String ioVariables = returnValue + " " + funcArgs;
                    preserveVars = new ArrayList<>(List.of(ioVariables.split(" ")));
                    preserveTags = new ArrayList<>(bashCache.stream().
                            filter(line -> line.startsWith("::")).toList());
                    preserveTags.replaceAll(s -> s.substring(2));

                    ArrayList<String> tagsList = preserveTags;
                    ArrayList<String> varsList = preserveVars;
                    bashCache.replaceAll(s -> replaceReserve(s, varsList, tagsList));

                    stdIOStream funcStream = stdIOStream.from(bashCache, returnValue, preserveTags.size());
                    MindustryFileConverter.funcMap.putIfAbsent(funcName, funcStream);
                    bashCache = new ArrayList<>();
                }
            }

            if (matchIndex > 0) bashCache.add(bash);

            if (bash.startsWith(keyStart)) {
                String[] functionHead = bash.split(" ");
                if (functionHead.length < 3) {
                    printRedError("Bad definition of function <anonymous>");
                    return;
                }
                returnValue = functionHead[1];
                int argsStart = functionHead[2].indexOf("(");
                funcName = functionHead[2].substring(0, argsStart + 1);
                funcArgs = functionHead[2]
                        .substring(argsStart + 1, getEndBracket(functionHead[2], argsStart))
                        .replace(',', ' ');
                matchIndex++;
            }
            for (var key : keysJump) {
                if (bash.startsWith(key)) {
                    matchIndex++;
                    break;
                }
            }
        }
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
        STRICT_EQUALS("===", 2), EQUALS("==", 2),
        NOT_EQUALS("!=", 2), LOGICAL_AND("&&", 2),
        GREATER_THAN_OR_EQUAL_TO(">=", 2), LESS_THAN_OR_EQUAL_TO("<=", 2),
        UNSIGNED_RIGHT_SHIFT(">>>", 5), RIGHT_SHIFT(">>", 5),
        LEFT_SHIFT("<<", 5), BITWISE_XOR("^", 2),
        GREATER_THAN(">", 2), LESS_THAN("<", 2),
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
            int p1 = 0;
            int p2 = 0;
            for (Operator o : Operator.values()) {
                if (o.value.equals(c1)) {
                    p1 = o.priority;
                }
                if (o.value.equals(c2)) {
                    p2 = o.priority;
                }
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
                if (o.value.equals(c)) {
                    return true;
                }
            }
            return false;
        }
    }
}
