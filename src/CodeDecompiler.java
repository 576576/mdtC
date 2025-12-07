import java.util.*;
import java.util.function.Function;

public class CodeDecompiler {
    /**
     * 主转换函数入口
     */
    public static String decompile(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));

        stdCodeStream result_link = convertLink(stdCodeStream.of(bashList));
        stdCodeStream result_jump = convertJump(result_link);

        if (Main.primeCodeLevel >= 2) {
            String filePath = Main.filePath;
            if (filePath.endsWith(".mdtcode")) {
                String primeCodePath = filePath.replace(".mdtcode", "_prime.mdtc");
                String writeContent = CodeFormatter.format(result_jump.toString());
                Utils.writeFile(primeCodePath, writeContent);

                IO.println("PrimeCode output at:\n> " + primeCodePath);
            } else IO.println("Skip writing prime code.");
        }

        stdCodeStream result_code = convertCode(result_jump);
        stdCodeStream result_fold = simplifyCode(result_code);

        return convertJump2(result_fold).toString().trim();
    }

    /**
     * 将原始jump绝对跳转转换为标签相对跳转
     */
    static stdCodeStream convertLink(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        List<Integer> tags = bashList.stream()
                .filter(line -> line.startsWith("jump "))
                .map(line -> Integer.parseInt(line.split(" ", 3)[1]))
                .sorted().toList();

        Map<Integer, String> tagMap = new HashMap<>();
        for (int i = 0; i < tags.size(); i++) {
            tagMap.put(tags.get(i), "TAG." + (i + 1));
        }

        int offset = 0;
        for (int i = 0; i < bashList.size(); i++) {
            String line = bashList.get(i);
            if (tagMap.containsKey(i - offset)) {
                bashList.add(i, "::" + tagMap.get(i - offset));
                offset++;
                i++;
            }
            if (line.startsWith("jump ")) {
                String jumpLine = line.split(" ", 3)[1];
                int target = Integer.parseInt(jumpLine);
                bashList.set(i, line.replaceFirst(jumpLine, tagMap.get(target)));
            }
        }

        return stdCodeStream.of(bashList);
    }

    /**
     * 逆转换原生的jump为if/while
     */
    static stdCodeStream convertJump(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        HashSet<String> ignoreTags = new HashSet<>(), ignoreLines = new HashSet<>();

        while (true) {
            String tagTo = "", line2 = "";
            int lineIndex = -1, line2Index = -1;
            for (int i = 0; i < bashList.size(); i++) {
                String line = bashList.get(i);
                if (line.startsWith("::")) {
                    tagTo = line.substring(2);
                    if (ignoreTags.contains(tagTo)) continue;
                    lineIndex = i;
                    break;
                }
            }
            for (int i = 0; i < bashList.size(); i++) {
                String line = bashList.get(i);
                if (line.startsWith("jump " + tagTo)) {
                    if (ignoreLines.contains(line)) continue;
                    line2Index = i;
                    line2 = line;
                    break;
                }
            }
            if (line2Index == -1) {
                if (lineIndex == -1) break;
                ignoreTags.add(tagTo);
                continue;
            }

            final List<String> keysStart = List.of("if(", "do{");
            int matchIndex = 0;
            if (lineIndex < line2Index) {
                o:
                for (int i = lineIndex; i < line2Index; i++) {
                    String line = bashList.get(i);
                    for (String key : keysStart) {
                        if (line.startsWith(key)) {
                            matchIndex++;
                            continue o;
                        }
                    }
                    if (line.startsWith("}")) matchIndex--;
                }
                if (matchIndex != 0) {
                    ignoreLines.add(line2);
                    continue;
                }

                String condition = Utils.reduceCondition(line2.split(" ", 3)[2]);
                bashList.add(lineIndex, "do{");
                bashList.set(line2Index + 1, "}while(" + condition + ")");
            } else {
                o:
                for (int i = line2Index; i < lineIndex; i++) {
                    String line = bashList.get(i);
                    for (String key : keysStart) {
                        if (line.startsWith(key)) {
                            matchIndex++;
                            continue o;
                        }
                    }
                    if (line.startsWith("}")) matchIndex--;
                }
                if (matchIndex != 0) {
                    ignoreLines.add(line2);
                    continue;
                }

                String condition = Utils.reduceCondition(Utils.reverseCondition(line2.split(" ", 3)[2]));
                bashList.add(lineIndex, "}");
                bashList.set(line2Index, "if(" + condition + "){");
            }
        }

        Utils.removeSpareTags(bashList);

        return stdCodeStream.of(bashList);
    }

    /**
     * 逆转换输入代码中的单行代码到mdtc形式
     */
    static stdCodeStream convertCode(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();

        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put("set ", s -> s.replaceFirst(" ", "="));

            put("print ", s -> "print(" + s + ")");
            put("printchar ", s -> "printchar(" + s + ")");
            put("format ", s -> "format(" + s + ")");
            put("wait ", s -> "wait(" + s + ")");

            put("ubind ", s -> "ubind(" + s + ")");
            put("ucontrol ", s -> "uctrl(" + s.replace(" 0", "").replace(' ', ',') + ")");

            put("draw ", s -> "draw(" + Utils.reduceParams("0", s) + ")");

            put("jump ", s -> {
                String[] params = s.split(" ", 2);
                String condition = Utils.reduceCondition(params[1]);
                condition = condition.equals("0==0") ? "" : ".when(" + condition + ")";
                return String.format("jump(%s)%s", params[0], condition);
            });

            put("control ", s -> {
                final List<String> ctrlTypes = List.of("enabled", "config", "color", "shoot", "shootp");

                String[] params = s.split(" ", 3);
                String block = params[1], ctrlType = params[0], target = params[2], target2 = "";

                if (!ctrlTypes.contains(ctrlType)) {
                    ctrlType = "uctrl";
                    target = Utils.reduceParams("0", ctrlType, target);
                } else {
                    if (ctrlType.equals("enabled"))
                        ctrlType = "enable";
                    params = target.split(" ");
                    if (ctrlType.equals("shootp")) {
                        ctrlType = "shoot";
                        target = params[1];
                        target2 = ".to(" + params[0] + ")";
                    } else if (ctrlType.equals("shoot")) {
                        target = params[2];
                        target2 = ".to(" + String.join(",", params[0], params[1]) + ")";
                    } else target = params[0];
                }

                return String.format("%s.%s(%s)%s", block, ctrlType, target, target2);
            });

            put("ulocate ", s -> {
                String[] params = s.split(" ");
                String locateType = params[0], building = params[1], enemy = params[2], ore = params[3], block = params[7];

                String result = String.format("%s.ulocate(%s)", block, locateType.equals("building") ? building : locateType);
                if (locateType.equals("ore")) result += ".ore(" + ore + ")";
                if (!enemy.equals("0")) result += ".enemy(" + enemy + ")";
                return result;
            });

            put("unpackcolor ", s -> {
                String[] params = s.split(" ");
                return params[4] + ".unpack(" + Utils.reduceParams("0", 4, params) + ")";
            });
            put("printflush ", s -> s + ".pflush()");
            put("drawflush ", s -> s + ".dflush()");

            put("write ", s -> {
                String[] params = s.split(" ");
                String content = params[0], block = params[1], bit = params[2];
                content = bit.equals("0") ? content : String.join(",", content, bit);
                return block + ".write(" + content + ")";
            });

            put("sensor ", s -> {
                String[] params = s.split(" ");
                return String.format("%s=%s.sensor(%s)", params[0], params[1], params[2]);
            });

            put("read ", s -> {
                String[] params = s.split(" ");
                return String.format("%s=%s.read(%s)", params[0], params[1], params[2]);
            });

            put("select ", s -> {
                String[] params = s.split(" ");
                String condition = String.join(" ", params[1], params[2], params[3]), result = params[0], target = params[4], target2 = params[5];
                return String.format("%s=%s.orElse(%s)%s", result, target, target2, ".when(" + Utils.reduceCondition(Utils.reverseCondition(condition)) + ")");
            });

            put("op ", s -> {
                String[] params = s.split(" ");
                String operator = params[0], result = params[1], paramString;

                final Map<String, String> operatorMap = Constants.operatorValueMap;

                if (operatorMap.containsKey(operator)) {
                    return String.format("%s=%s%s%s", result, params[2], operatorMap.get(operator), params[3]);
                } else if (operator.equals("logn") && params[4].equals("2")) {
                    operator = "lb";
                    paramString = params[2];
                } else {
                    operator = Constants.operatorAliasMap.getOrDefault(operator, operator);
                    if (operator.equals("log"))
                        paramString = Utils.reduceParams("0", params[3], params[2]);
                    else
                        paramString = Utils.reduceParams("0", params[2], params[3]);
                }
                return String.format("%s=%s(%s)", result, operator, paramString);
            });

            put("getlink ", s -> {
                String[] params = s.split(" ");
                return String.format("%s=link(%s)", params[0], params[1]);
            });

            put("lookup ", s -> {
                final List<String> lkTypes = List.of("block", "unit", "item", "liquid", "team");
                String[] params = s.split(" ");
                String lookupType = params[0], block = params[1], index = params[2], ctrlType = "lookup", content;
                if (lkTypes.contains(lookupType)) {
                    ctrlType = lookupType;
                    content = index;
                } else content = String.join(",", lookupType, index);
                return String.format("%s=%s(%s)", block, ctrlType, content);
            });
            put("packcolor ", s -> {
                String[] params = s.split(" ", 2);
                return String.format("%s=pack(%s)", params[0], params[1].replace(" ", ","));
            });

            put("uradar ", s -> {
                String[] params = s.split(" ");
                String target, order = params[5], sort = params[3], result = params[6];
                target = Utils.reduceParams("any", 3, params[0], params[1], params[2]);

                result += "=uradar()";
                if (!target.isEmpty() && !target.equals("enemy")) result += ".target(" + target + ")";
                if (!order.equals("1")) result += ".order(" + order + ")";
                if (!sort.equals("distance")) result += ".sort(" + sort + ")";

                return result;
            });

            put("radar ", s -> {
                String[] params = s.split(" ");
                String target, order = params[5], sort = params[3], result = params[6], block = params[4];
                target = Utils.reduceParams("any", 3, params[0], params[1], params[2]);

                if (block.equals("@this")) block = "";
                result += "=uradar(" + block + ")";
                if (!target.isEmpty() && !target.equals("enemy")) result += ".target(" + target + ")";
                if (!order.equals("1")) result += ".order(" + order + ")";
                if (!sort.equals("distance")) result += ".sort(" + sort + ")";

                return result;
            });
        }};

        List<String> ignoreKeys = List.of("::", "do{", "for(", "if(", "else{", "}");
        o:
        for (int i = 0; i < bashList.size(); i++) {
            String line = bashList.get(i);
            for (String key : ignoreKeys)
                if (line.startsWith(key)) continue o;
            String[] splitList = line.split(" ", 2);
            if (splitList.length < 2) {
                bashList.set(i, line + "()");
                continue;
            }

            String lineKey = splitList[0] + " ";
            bashList.set(i, funcHandlers.containsKey(lineKey) ?
                    funcHandlers.get(lineKey).apply(splitList[1].trim()) :
                    "raw(\"" + line + "\")");
        }
        return stdCodeStream.of(bashList);
    }

    /**
     * 折叠多重语句({@code DotCode} {@code FrontCode} {@code MidCode}),
     */
    static stdCodeStream simplifyCode(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();

        for (int i = bashList.size() - 1; i > 0; i--) {
            String line = bashList.get(i);
            List<String> parts = Utils.stringSplitPro(line);

            String op0, op1, op2;
            for (String midVar : parts) {
                if (midVar.matches("mid\\.\\d+")) {
                    for (int j = i - 1; j >= 0; j--) {
                        String assignLine = bashList.get(j);
                        if (assignLine.startsWith(midVar + "=")) {
                            String value = assignLine.substring(midVar.length() + 1).trim();
                            List<String> parts2 = Utils.stringSplitPro(value);
                            int replaceIndex = parts.indexOf(midVar);
                            if (replaceIndex == -1) continue;

                            boolean bracketTo = false;
                            if (parts2.size() > 2) {
                                op0 = parts2.get(1);
                                op1 = parts.get(replaceIndex - 1);
                                if (replaceIndex > 1 && replaceIndex < parts.size() - 1) {
                                    op2 = parts.get(replaceIndex + 1);
                                    bracketTo = Utils.isLowPriority(op0, op1, op2);
                                } else if (replaceIndex == parts.size() - 1) {
                                    bracketTo = Utils.isLowPriority(op0, op1);
                                }
                            }

                            boolean finalBracketTo = bracketTo;
                            parts.replaceAll(part -> part.equals(midVar) ? (finalBracketTo ? ("(" + value + ")") : value) : part);
                            bashList.set(i, String.join("", parts));
                            bashList.remove(j);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        return stdCodeStream.of(bashList);
    }

    /**
     * 重整以@counter=@counter开头的语句为jump2()
     */
    static stdCodeStream convertJump2(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        bashList.replaceAll(line -> line.startsWith("@counter=") ? "jump2(" + line.substring(line.startsWith("@counter=@counter") ? 17 : 9).trim() + ")" : line);
        return stdCodeStream.of(bashList);
    }
}
