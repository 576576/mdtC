import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MindustryFileConverter {

    public static void main(String[] args) {

    }

    public static stdIOStream convertCodeBlock(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>();
        bashList.add("::HEAD");
        for (String line : codeBlock.split("\n")) {
            if (!line.trim().isEmpty()) {
                stdIOStream convertedLine = convertCodeLine(line.trim());
                bashList.addAll(convertedLine.toStringArray());
            }
        }
        stdIOStream result_pre_jump = convertPreJump(stdIOStream.from(bashList));
        return convertJump(result_pre_jump);
    }

    private static stdIOStream convertCodeLine(String codeLine) {
        if (Utils.isSpecialControl(codeLine)) return new stdIOStream(codeLine);
        stdIOStream stdInput = stdIOStream.from(codeLine);
        if (Utils.isCtrlCode(codeLine)) return convertCtrl(stdInput);
        if (Utils.isDotCtrlCode(codeLine)) return convertDotCtrl(stdInput);

        stdIOStream result_dot_free = convertDot(stdInput);
        stdIOStream result_front_free = convertFront(result_dot_free);

        return convertMiddle(result_front_free);
    }

    /**
     * {@code CtrlCode} 为无副作用的以()形式内接调用函数.
     * {@code CtrlCode} 有效函数名为:
     * print printchar format wait ubind stop end
     * jump jump2 printf
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertCtrl(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();

        Map<String, Function<String, String>> funcHandlers = new HashMap<>();
        funcHandlers.put("print", s -> "print " + s);
        funcHandlers.put("printchar", s -> "printchar " + s);
        funcHandlers.put("format", s -> "format " + s);
        funcHandlers.put("wait", s -> "wait " + s);
        funcHandlers.put("ubind", s -> "ubind " + s);
        funcHandlers.put("stop", _ -> "stop");
        funcHandlers.put("end", _ -> "end");
        funcHandlers.put("jump", s -> {
            String target;
            if (!s.contains(")")) {
                target = s.trim().isEmpty() ? "HEAD" : s;
                return "jump " + target + " always 0 0";
            }
            String[] parts = s.split("\\.");
            String[] params = new String[3];
            target = s.substring(0, s.indexOf(")"));
            if (target.isEmpty()) target = "HEAD";
            for (String part : parts) {
                if (!part.endsWith(")")) part = part + ")";
                String bracketContent = part.substring(part.indexOf('(') + 1, part.indexOf(')'));

                if (bracketContent.isEmpty()) continue;
                if (part.startsWith("when(")) {
                    String[] rpnArray = ReversePolishNotation.generateRpn(bracketContent);
                    if (rpnArray.length < 3) continue;
                    String operator = rpnArray[2];
                    Map<String, String> keywordMap = Utils.operatorKeyMap();
                    if (keywordMap.containsKey(operator)) params[0] = keywordMap.get(operator);
                    else continue;
                    params[1] = rpnArray[0];
                    params[2] = rpnArray[1];
                }
            }
            return "jump " + target + " " + Utils.padParams(params, 3);
        });

        funcHandlers.put("jump2", s -> {
            String[] strSplit = Utils.stringSplit(s);
            if (strSplit.length > 1) s = "@counter=@counter" + s;
            else s = "@counter=" + s;
            stdIOStream jump2stream = convertCodeLine(s);
            bashList.addAll(jump2stream.toStringArray());
            return "";
        });
        funcHandlers.put("printf", s -> {
            String[] parts = s.split(",");
            if (parts.length < 2) return "print " + s;
            bashList.add("print " + parts[0]);
            for (int i = 1; i < parts.length; i++) {
                bashList.add("format " + parts[i]);
            }
            return "";
        });

        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            if (expr.contains(entry.getKey() + "(")) {
                int start = expr.indexOf(entry.getKey() + "(");
                int end = expr.lastIndexOf(')');
                String s = expr.substring(start + entry.getKey().length() + 1, end).trim();
                bashList.add(entry.getValue().apply(s));
            }
        }

        return new stdIOStream(bashList, "");
    }

    /**
     * {@code DotCtrlCode} 为无副作用的以.形式后接调用函数.
     * {@code DotCtrlCode} 有效函数名为:
     * enable shoot config color unpack dflush pflush
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertDotCtrl(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        var ref = new Object() {
            String block = "";
        };

        Map<String, Function<String, String>> funcHandlers = new HashMap<>();
        funcHandlers.put("enable", s -> "control enabled " + ref.block + " " + Utils.padParams(s, 4));
        funcHandlers.put("config", s -> "control config " + ref.block + " " + Utils.padParams(s, 4));
        funcHandlers.put("color", s -> "control color " + ref.block + " " + Utils.padParams(s, 4));
        funcHandlers.put("shoot", s -> {
            if (s.isEmpty()) return "control shoot " + ref.block + " 0 0 0 0";
            String[] parts = s.split("\\.");
            String target = "@this", ctrlType = "shootp", shooting = "1";
            if (!s.startsWith(")")) shooting = s.substring(0, Math.max(1, s.indexOf(")")));
            for (String part : parts) {
                if (!part.endsWith(")")) part = part + ")";
                String bracketContent = part.substring(part.indexOf('(') + 1, part.indexOf(')'));

                if (bracketContent.isEmpty()) continue;
                if (part.startsWith("target(")) {
                    if (bracketContent.split(",").length > 1) ctrlType = "shoot";
                    target = bracketContent.replaceAll(",\\s*", " ");
                }
            }
            return "control " + ctrlType + " " + ref.block + " " + Utils.padParams(target + " " + shooting, 4);
        });

        funcHandlers.put("unpack", s -> "unpackcolor " + Utils.padParams(s.split(","), 4) + " " + ref.block);
        funcHandlers.put("pflush", _ -> "printflush " + ref.block);
        funcHandlers.put("dflush", _ -> "drawflush " + ref.block);
        funcHandlers.put("write", s -> {
            String[] parts = s.split(",");
            String content = "null", bit = "0";
            if (parts.length > 0) content = parts[0];
            if (parts.length > 1) bit = parts[1];
            return "write " + content + " " + ref.block + " " + bit;
        });

        String expr = stream.expr();
        ref.block = Utils.getDotCtrlBlock(expr);
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey() + "(")) {
                int start = expr.indexOf(entry.getKey());
                int end = Utils.getEndDotCtrl(expr, start);
                String s = expr.substring(start + entry.getKey().length() + 1, end).trim();
                String result = entry.getValue().apply(s);
                bashList.add(result);
                expr = expr.substring(0, start - 1) + expr.substring(end + 1);
            }
        }
        if (expr.equals(ref.block)) expr = "";
        return new stdIOStream(bashList, expr);
    }

    /**
     * {@code DotCode} 为有副作用的以.形式后接调用函数.
     * {@code DotCode} 有效函数名为:
     * sensor read
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertDot(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();
        var ref = new Object() {
            int midNum = stream.stat();
            String block = "";
        };
        Map<String, Function<String, String>> funcHandlers = new HashMap<>();
        funcHandlers.put("sensor", s -> "sensor mid." + ref.midNum + " " + ref.block + " " + s);
        funcHandlers.put("read", s -> "read mid." + ref.midNum + " " + ref.block + " " + s);

        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey() + "(")) {
                int start = expr.indexOf(entry.getKey());
                String[] strSplit = Utils.stringSplit(expr.substring(0, start - 1));
                ref.block = strSplit[strSplit.length - 1];

                int end = expr.indexOf(')', start);
                while (end < expr.length() - 1 && end != -1 && expr.charAt(end + 1) == '.') {
                    end = expr.indexOf(')', end + 1);
                }

                String s = expr.substring(start + entry.getKey().length() + 1, end).trim();
                String result = entry.getValue().apply(s);
                bashList.add(result);
                expr = expr.substring(0, start - ref.block.length() - 1) + "mid." + ref.midNum + expr.substring(end + 1);
                ref.midNum++;
            }
        }
        return new stdIOStream(bashList, expr, ref.midNum);
    }

    /**
     * {@code FrontCode} 为有副作用的以()形式内接调用函数.
     * {@code FrontCode} 有效函数名为:
     * not abs sign floor ceil round sqrt rand sin cos tan asin acos atan
     * ln lg max min len angle angleDiff noise log pack
     * link block unit item liquid team sensor uradar radar
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertFront(stdIOStream stream) {
        ArrayList<String> bashList = new ArrayList<>(stream.bash());
        var ref = new Object() {
            int midNum = stream.stat();
        };
        Map<String, Function<String, String>> funcHandlers = new HashMap<>();
        Map<String, Function<String, String>> funcHandlers_low = new HashMap<>();

        funcHandlers.put("not", s -> "op not mid." + ref.midNum + " " + s);
        funcHandlers.put("abs", s -> "op abs mid." + ref.midNum + " " + s + " 0");
        funcHandlers.put("sign", s -> "op sign mid." + ref.midNum + " " + s);
        funcHandlers.put("floor", s -> "op floor mid." + ref.midNum + " " + s);
        funcHandlers.put("ceil", s -> "op ceil mid." + ref.midNum + " " + s);
        funcHandlers.put("round", s -> "op round mid." + ref.midNum + " " + s);
        funcHandlers.put("sqrt", s -> "op sqrt mid." + ref.midNum + " " + s);
        funcHandlers.put("rand", s -> "op rand mid." + ref.midNum + " " + s);
        funcHandlers.put("sin", s -> "op sin mid." + ref.midNum + " " + s);
        funcHandlers.put("cos", s -> "op cos mid." + ref.midNum + " " + s);
        funcHandlers.put("tan", s -> "op tan mid." + ref.midNum + " " + s);
        funcHandlers.put("asin", s -> "op asin mid." + ref.midNum + " " + s);
        funcHandlers.put("acos", s -> "op acos mid." + ref.midNum + " " + s);
        funcHandlers.put("atan", s -> "op atan mid." + ref.midNum + " " + s);
        funcHandlers.put("ln", s -> "op log mid." + ref.midNum + " " + s + " 0");
        funcHandlers.put("lg", s -> "op log10 mid." + ref.midNum + " " + s + " 0");

        funcHandlers.put("max", s -> {
            String[] paramParts = s.split(",");
            return "op max mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("min", s -> {
            String[] paramParts = s.split(",");
            return "op min mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("len", s -> {
            String[] paramParts = s.split(",");
            return "op len mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("angle", s -> {
            String[] paramParts = s.split(",");
            return "op angle mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("angleDiff", s -> {
            String[] paramParts = s.split(",");
            return "op angleDiff mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("noise", s -> {
            String[] paramParts = s.split(",");
            return "op noise mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
        });
        funcHandlers.put("log", s -> {
            String[] paramParts = s.split(",");
            return "op logn mid." + ref.midNum + " " + paramParts[1].trim() + " " + paramParts[0].trim();
        });

        funcHandlers.put("link", s -> "getlink mid." + ref.midNum + " " + s);
        funcHandlers.put("block", s -> "lookup block mid." + ref.midNum + " " + s);
        funcHandlers.put("unit", s -> "lookup unit mid." + ref.midNum + " " + s);
        funcHandlers.put("item", s -> "lookup item mid." + ref.midNum + " " + s);
        funcHandlers.put("liquid", s -> "lookup liquid mid." + ref.midNum + " " + s);
        funcHandlers.put("team", s -> "lookup team mid." + ref.midNum + " " + s);
        funcHandlers.put("pack", s -> "packcolor mid." + ref.midNum + " " + Utils.padParams(s.split(","), 4));

        funcHandlers.put("uradar", s -> {
            if (s.equals("()")) return "uradar enemy any any distance 0 0 mid." + ref.midNum;
            String[] parts = s.split("\\.");
            String block = "0", target = "enemy any any", order = "1", sort = "distance";
            for (String part : parts) {
                if (part.endsWith(")")) part = part.substring(0, part.lastIndexOf(")"));
                int bracketIndex = part.indexOf("(") + 1;
                if (bracketIndex >= part.length()) continue;
                if (part.startsWith("target(")) {
                    target = part.replaceAll(",\\s*", " ");
                } else if (part.startsWith("order(")) {
                    order = part;
                } else if (part.startsWith("sort(")) {
                    sort = part;
                }
            }
            String[] targetParts = target.split(" ");
            if (targetParts.length < 3) {
                target = String.join(" ", targetParts) + (targetParts.length == 2 ? " any" : " any any");
            }
            return "uradar " + target + " " + sort + " " + block + " " + order + " mid." + ref.midNum;
        });
        funcHandlers_low.put("radar", s -> {
            if (s.isEmpty()) return "radar enemy any any distance 0 0 mid." + ref.midNum;
            String[] parts = s.split("\\.");
            String block = "@this", target = "enemy any any", order = "1", sort = "distance";
            if (!s.startsWith(")")) block = s.substring(0, s.indexOf(")"));
            for (String part : parts) {
                if (!part.endsWith(")")) part = part + ")";
                String bracketContent = part.substring(part.indexOf('(') + 1, part.indexOf(')'));
                if (bracketContent.isEmpty()) continue;
                if (part.startsWith("target(")) {
                    target = bracketContent.replaceAll(",\\s*", " ");
                } else if (part.startsWith("order(")) {
                    order = bracketContent;
                } else if (part.startsWith("sort(")) {
                    sort = bracketContent;
                }
            }
            String[] targetParts = target.split(" ");
            if (targetParts.length < 3) {
                target = String.join(" ", targetParts) + (targetParts.length == 2 ? " any" : " any any");
            }
            return "radar " + target + " " + sort + " " + block + " " + order + " mid." + ref.midNum;
        });

        String expr = stream.expr();
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey() + "(")) {
                int start = expr.indexOf(entry.getKey());
                int end = Utils.getEndDotCtrl(expr, start);
                String s = expr.substring(start + entry.getKey().length() + 1, end).trim();
                String result = entry.getValue().apply(s);
                bashList.add(result);
                String regex = expr.substring(start, end + 1);
                expr = expr.replace(regex, "mid." + ref.midNum);
                ref.midNum++;
            }
        }
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers_low.entrySet()) {
            while (expr.contains(entry.getKey() + "(")) {
                int start = expr.indexOf(entry.getKey());
                int end = Utils.getEndDotCtrl(expr, start);
                String s = expr.substring(start + entry.getKey().length() + 1, end).trim();
                String result = entry.getValue().apply(s);
                bashList.add(result);
                expr = expr.substring(0, start) + "mid." + ref.midNum + expr.substring(end + 1);
                ref.midNum++;
            }
        }

        return new stdIOStream(bashList, expr, ref.midNum);
    }


    /**
     * {@code MidCode} 为有副作用的以可逆波兰化形式中接调用函数.
     * {@code MidCode} 有效函数名为:
     * + - * / // % %% .^ == != && < <= > >= === << >> >>> | & ^
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertMiddle(stdIOStream stream) {
        String[] rpnArray = ReversePolishNotation.generateRpn(stream.expr());
        ArrayList<String> stack = new ArrayList<>();
        ArrayList<String> bashList = stream.bash();
        var ref = new Object() {
            int midNum = stream.stat();
        };
        Map<String, String> operatorMap = Utils.operatorKeyMap();
        Map<String, Integer> offsetMap = Utils.operatorOffsetMap();

        for (String token : rpnArray) {
            if (operatorMap.containsKey(token)) {
                String op = operatorMap.get(token);
                String midVar = "mid." + ref.midNum;
                if (!op.equals("set")) {
                    String result = "op " + op + " " + midVar + " " + stack.get(stack.size() - 2) + " " + stack.getLast();
                    bashList.add(result);
                    stack.removeLast();
                    stack.removeLast();
                    stack.add(midVar);
                    ref.midNum++;
                } else {
                    String result = "set " + stack.getFirst() + " " + stack.getLast();
                    String bashLast;
                    if (!bashList.isEmpty()) {
                        bashLast = bashList.getLast();
                        if (bashLast.split(" ")[offsetMap.get(bashLast.split(" ")[0])].equals(stack.getLast())) {
                            result = bashLast.replace(stack.getLast(), stack.getFirst());
                            bashList.removeLast();
                        }
                    }
                    bashList.add(result);
                    stack.clear();
                }
            } else {
                stack.add(token);
            }
        }

        StringBuilder expr = new StringBuilder();
        for (String item : stack) {
            expr.append(item);
        }
        return new stdIOStream(bashList, expr.toString());
    }

    private static stdIOStream convertPreJump(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        bashList.removeIf(String::isEmpty);
        var ref = new Object() {
            int tag = 0;
        };

        String[] keysStart = {"do{", "for(", "if("};
        String[] keysEnd = {"}while(", "}", "}"};
        while (true) {
            int matchIndex = 0, lineIndex = -1, line2Index = -1;
            for (int i = 0; i < bashList.size(); i++) {
                String line = bashList.get(i);
                for (int j = 0; j < keysStart.length; j++) {
                    String keyStart = keysStart[j], keyEnd = keysEnd[j];
                    if (line.startsWith(keyStart)) {
                        if (lineIndex == -1) lineIndex = i;
                        matchIndex++;
                    } else if (line.startsWith(keyEnd)) {
                        if (matchIndex == 1) line2Index = i;
                        matchIndex--;
                    }
                }
                if (line2Index != -1) break;
            }
            if (line2Index == -1) {
                if (lineIndex != -1) {
                    Utils.printRedError("Error: {} not match: No such operation.");
                    return stdIOStream.empty();
                } else break;
            }

            String line = bashList.get(lineIndex), line2 = bashList.get(line2Index);
            ArrayList<String> bashCache;
            int line2Offset = 0;
            if (line.startsWith("do{")) {
                bashList.set(lineIndex, "::PRESERVE-TAG#" + ref.tag);
                line2 = line2.substring(line2.indexOf("while(") + 6, line2.lastIndexOf(")"));

                bashCache = convertCodeLine(line2).toStringArray();
            } else {
                String bracketContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
                bashList.set(lineIndex, "::PRESERVE-TAG#" + ref.tag);
                if (line.startsWith("for(")) {
                    String[] forParts = bracketContent.split(";");
                    if (forParts.length != 3) {
                        Utils.printRedError("Error: for() content not match");
                        return stdIOStream.empty();
                    }
                    ArrayList<String> initStream = convertCodeLine(forParts[0]).toStringArray();
                    bashList.addAll(lineIndex, initStream);
                    line2Offset += initStream.size();

                    stdIOStream operateStream = convertCodeLine(forParts[2]);
                    stdIOStream conditionStream = convertCodeLine(forParts[1]);
                    bashCache = operateStream.toStringArray();
                    bashCache.addAll(conditionStream.toStringArray());
                } else {
                    bashCache = convertCodeLine(bracketContent).toStringArray();
                }
            }
            bashCache.removeLast();
            String[] conditionParts = bashCache.getLast().split(" ", 4);
            String condition = conditionParts[1] + " " + conditionParts[3];
            bashCache.removeLast();
            bashCache.add("jump PRESERVE-TAG#" + ref.tag + " " + condition);

            bashList.remove(line2Index + line2Offset);
            bashList.addAll(line2Index + line2Offset, bashCache);
            ref.tag++;
        }
        return stdIOStream.from(bashList);
    }

    private static stdIOStream convertJump(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();

        int tagNum;
        for (int i = 0; i < bashList.size(); i++) {
            String line = bashList.get(i);
            tagNum = 0;
            if (line.startsWith("jump ")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    String arg = parts[1].trim();
                    int index = -1;
                    for (int j = 0; j < bashList.size(); j++) {
                        String codeLine = bashList.get(j);
                        if (bashList.get(j).startsWith("::")) {
                            String tag = codeLine.substring(2).trim();
                            if (tag.equals(arg)) {
                                index = j - tagNum;
                                break;
                            }
                            tagNum += 1;
                        }
                    }
                    if (index >= 0) {
                        bashList.set(i, "jump " + index + " " + String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
                    } else {
                        Utils.printRedError("MdtC Compile Error: Jump() tag not found of [" + arg + "]");
                        bashList.set(i, "jump 0 always 0 0");
                    }
                }
            }
        }

        bashList.removeIf(line -> line.startsWith("::"));

        return new stdIOStream(bashList, expr);
    }

}
