import java.awt.*;
import java.io.*;
import java.util.*;

public class Utils {

    static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        File file = new File(filePath);
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

    static void fileIO() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("MdtC Compiler v" + Main.versionTag);
        System.out.print("Enter .mdtc file path below.\n> ");
        String filePath = "";
        if (!Main.isDebug) filePath = scanner.nextLine();
        filePath = !filePath.isEmpty() ? filePath : Main.fileDefault;

        if (Main.isDebug) System.out.println(filePath);
        File inputFilePath = new File(filePath);
        String inputContent = readFile(filePath);

        stdIOStream convertedContent = MindustryFileConverter.convertCodeBlock(inputContent);

        String outputFilePath = inputFilePath.getAbsolutePath().replace(".mdtc", ".mdtcode");

        writeFile(outputFilePath, convertedContent.toString());
        System.out.println("Compile output at:\n" + outputFilePath);
        if (Main.openAfterCompile)
            openWithSystemExplorer(outputFilePath);
    }

    static Map<String, Integer> operatorOffsetMap() {
        Map<String, Integer> keywordMap = new HashMap<>();
        keywordMap.put("op", 2);
        keywordMap.put("getlink", 1);
        keywordMap.put("radar", 7);
        keywordMap.put("uradar", 7);
        keywordMap.put("lookup", 2);
        keywordMap.put("packcolor", 1);
        keywordMap.put("read", 1);
        return keywordMap;
    }
    
    static Map<String,String> operatorReverseMap(){
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
        String[] keys = {"ctrl", "enable", "config", "color", "shoot", "unpack", "pflush", "dflush", "write"};
        int equalsIndex = codeLine.indexOf('=');
        int dotIndex = codeLine.indexOf("."), midNumIndex = codeLine.indexOf("mid.");
        int bracketIndex = codeLine.indexOf(')');
        if (dotIndex == -1 || bracketIndex == -1 || dotIndex > bracketIndex) return false;
        else return equalsIndex == -1 || (equalsIndex > dotIndex && dotIndex != (midNumIndex + 3));
    }

    static boolean isCtrlCode(String codeLine) {
        String[] keys = {"print(", "printchar(", "format(", "wait(", "stop(", "end(", "ubind(", "uctrl(", "jump(", "jump2(", "printf("};
        for (String command : keys) {
            if (codeLine.startsWith(command)) {
                return true;
            }
        }
        return false;
    }

    static void printRedError(String message) {
        System.err.println("\u001B[31m" + message + "\u001B[0m");
    }

    static int getEndDotCtrl(String expr, int start) {
        int end = getEndBracket(expr, start);
        if (end >= expr.length() - 1 || expr.charAt(end + 1) != '.') return end;
        String[] keys = {"enable(", "config(", "color(", "shoot(", "unpack(", "pflush(", "dflush(", "write("};
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
        String[] keys = {"enable(", "config(", "color(", "shoot(", "unpack(", "pflush(", "dflush(", "write("};
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
        //str = str.replaceAll("\\s", "~"); // Replace all spaces with tilde placeholders
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean isOperator = false;
            for (Operator o : Operator.values()) {
                if (str.startsWith(o.value, i)) {
                    if (!token.isEmpty()) {
                        tokens.add(token.toString());
                        token = new StringBuilder();
                    }
                    tokens.add(o.value);
                    i += o.value.length() - 1; // Skip the length of the operator
                    isOperator = true;
                    break;
                }
            }
            if (!isOperator) {
                token.append(c);
            }
        }
        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }
        return tokens.stream().map(s -> s.replace("~", " ")).toArray(String[]::new); // Replace tildes with spaces in the final output
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

    static boolean isTheBracket(String expr, int start, int end) {
        Stack<Integer> stack = new Stack<>();
        boolean countStart = false;
        int o = 0;
        for (int i = 0; i < expr.length(); i++) {
            if (i == start) countStart = true;
            if (expr.charAt(i) == '(') {
                stack.push(i);
                if (countStart) o++;
            } else if (expr.charAt(i) == ')') {
                if (countStart) o--;
                if (stack.isEmpty()) return false; // Mismatched closing bracket
                stack.pop();
                if (countStart && o == 0) return i == end;
                if (stack.isEmpty()) return false; // Found the matching closing bracket
            }
        }
        return false; // No matching closing bracket found
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
