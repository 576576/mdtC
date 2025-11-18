import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class MdtcConverter {
    static Map<String, stdIOStream> funcMap = new HashMap<>();

    static void main() {
        String str = "u=1\njump().when(u.sensor(@flag)!=u.fx)";
        IO.println(convertCodeBlock(str));
    }

    /**
     * 主转换函数入口
     *
     * @return {@code stdIOStream}
     */
    public static stdIOStream convertCodeBlock(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>();

        codeBlock = insertImport(codeBlock);
        codeBlock = unfoldRepeat(codeBlock);

        var funcStartIndex = codeBlock.indexOf("\nfunction");
        if (funcStartIndex != -1) {
            String funcBlock = codeBlock.substring(funcStartIndex);
            codeBlock = codeBlock.substring(0, funcStartIndex);

            funcMap = generateFuncMap(funcBlock);
            codeBlock = insertFunc(codeBlock);
        }

        if (Main.isGeneratePrimeCode) {
            String filePath = Main.filePath;
            if (!filePath.endsWith("_prime.mdtc") && filePath.endsWith(".mdtc")) {
                filePath = filePath.replace(".mdtc", "_prime.mdtc");
                String writeContent = MdtcFormater.convertToFormat(codeBlock);
                Utils.writeFile(filePath, writeContent);

                IO.println("PrimeCode output at:\n" + filePath);
            } else IO.println("Skip writing prime code.");
        }

        int refMax = 1;
        for (String line : codeBlock.split("\n")) {
            if (!line.trim().isEmpty()) {
                stdIOStream convertedLine = convertCodeLine(stdIOStream.from(line.trim()));
                refMax = Math.max(refMax, convertedLine.stat());
                bashList.addAll(convertedLine.toStringArray());
            }
        }
        bashList.add(0, "::HEAD");
        bashList.add("::END");
        if (!bashList.contains("::DEFAULT"))
            bashList.add(1, "::DEFAULT");

        stdIOStream result_clear = convertSet(stdIOStream.from(bashList, refMax));
        stdIOStream result_pre_jump = convertJump(result_clear);
        return convertLink(result_pre_jump);
    }

    private static String insertImport(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        List<String> importLines = bashList.stream()
                .filter(line -> line.startsWith("import ")).toList();
        bashList.removeIf(line -> line.startsWith("import "));
        StringBuilder codeBlockBuilder = new StringBuilder(codeBlock);
        for (var line : importLines) {
            String importPath = line.substring(6).trim();
            codeBlockBuilder.append("\n").append(Utils.readFile(importPath));
        }
        codeBlock = codeBlockBuilder.toString();

        //todo:支持只导入部分函数(关键字 from/as)
//        Map<String, ArrayList<String>> insertFuncMap = new HashMap<>();

        return codeBlock;
    }

    /**
     * <p>展开{@code repeat}块</p>
     * <p>等价的1D数组实现, 嵌套即可实现n维数组</p>
     */
    private static String unfoldRepeat(String codeBlock) {
        var ref = new Object() {
            int midNum = 1;
        };
        final String keyStart = "repeat(", keyEnd = "}";
        final String[] keysJump = {"do{", "for(", "if("};
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();
        bashList.replaceAll(String::trim);
        ArrayList<String> preserveTags;
        String preserveVar = "";
        int matchIndex = 0, repeatRoutes = 0;
        int repeatStart = 0;
        boolean entryFound = false;
        for (int i = 0; i < bashList.size(); i++) {
            String bash = bashList.get(i);
            if (bash.startsWith(keyEnd)) {
                matchIndex--;
                if (!entryFound) continue;
                if (matchIndex == 0) {
                    preserveTags = new ArrayList<>(bashCache.stream().
                            filter(line -> line.startsWith("::")).toList());
                    preserveTags.replaceAll(s -> s.substring(2));

                    ArrayList<String> tagsList = preserveTags;
                    var varList = new ArrayList<>(List.of(new String[]{preserveVar}));
                    bashCache.replaceAll(s -> Utils.replaceReserve(s, varList, tagsList));

                    ArrayList<String> bashTo, bashToAdd = new ArrayList<>();
                    String finalPreserveVar = preserveVar;
                    for (int j = 0; j < repeatRoutes; j++) {
                        bashTo = new ArrayList<>(bashCache);
                        int finalJ = j;

                        bashTo.replaceAll(s -> s.replace("ARG.0", finalPreserveVar + finalJ));
                        bashTo.replaceAll(s -> s.replace("(PRESERVE_TAG.", "(REPEAT." + ref.midNum + "_PRESERVE_TAG."));
                        bashTo.replaceAll(s -> s.replace("::PRESERVE_TAG.", "::REPEAT." + ref.midNum + "_PRESERVE_TAG."));
                        bashTo.replaceAll(s -> s.replace("(FUNC.", "(REPEAT." + ref.midNum + "_FUNC."));
                        bashTo.replaceAll(s -> s.replace("::FUNC.", "::REPEAT." + ref.midNum + "_FUNC."));
                        bashToAdd.addAll(bashTo);
                    }
                    for (int j = -2; j < bashCache.size(); j++) bashList.remove(repeatStart);
                    bashList.addAll(repeatStart, bashToAdd);

                    bashCache = new ArrayList<>();
                    entryFound = false;
                    ref.midNum++;
                }
            }

            if (matchIndex > 0 && entryFound) bashCache.add(bash);

            if (bash.startsWith(keyStart)) {
                String bracketContent = bash.substring(7, bash.lastIndexOf(")"));
                String[] repeatInfos = bracketContent.split(",");
                if (repeatInfos.length == 0) {
                    Utils.printError("Error: repeat() not enough infos");
                    return codeBlock;
                }
                if (repeatInfos.length < 2) {
                    repeatRoutes = Integer.parseInt(repeatInfos[0]);
                } else {
                    preserveVar = repeatInfos[0];
                    repeatRoutes = Integer.parseInt(repeatInfos[1]);
                }
                entryFound = true;
                repeatStart = i;
                matchIndex++;
            }
            for (var key : keysJump) {
                if (bash.startsWith(key)) {
                    matchIndex++;
                    break;
                }
            }
        }
        return stdIOStream.from(bashList).toString();
    }

    /**
     * 内嵌函数到代码块
     *
     * @return {@code stdIOStream}
     */
    private static String insertFunc(String codeBlock) {
        var ref = new Object() {
            int midNum = 1;
        };
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();
        for (String bash : bashList) {
            for (var func : funcMap.entrySet()) {
                var funcKey = func.getKey();
                while (bash.contains(funcKey)) {
                    ArrayList<String> funcBody = new ArrayList<>(func.getValue().bash());
                    boolean funcStat = !func.getValue().expr().startsWith("void");
                    int start = bash.indexOf(funcKey);
                    int end = Utils.getEndBracket(bash, start);
                    ArrayList<String> varsName = new ArrayList<>(List.of(bash.substring(start + funcKey.length(), end).split(",")));
                    String returnName = funcStat ? "FUNC." + ref.midNum : "FUNC.void";
                    varsName.addFirst(returnName);
                    for (int j = 0; j < varsName.size(); j++) {
                        int finalJ = j;
                        funcBody.replaceAll(s -> s.replace("ARG." + finalJ, varsName.get(finalJ)));
                    }
                    funcBody.replaceAll(s -> s.replace("(PRESERVE_TAG.", "(FUNC." + ref.midNum + "_PRESERVE_TAG."));
                    funcBody.replaceAll(s -> s.replace("::PRESERVE_TAG.", "::FUNC." + ref.midNum + "_PRESERVE_TAG."));
                    funcBody.replaceAll(s -> s.replace("ARG.0", "FUNC." + ref.midNum));
                    bashCache.addAll(funcBody);
                    bash = bash.substring(0, start) + returnName + bash.substring(end + 1);
                    ref.midNum++;
                }
            }
            bash = bash.replaceAll("FUNC.void", "");

            if (!bash.trim().isEmpty()) bashCache.add(bash);
        }
        return Utils.listToCodeBlock(bashCache);
    }

    /**
     * <p>转换一行代码</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertCodeLine(stdIOStream stream) {
        String codeLine = stream.expr();
        if (Utils.isSpecialControl(codeLine)) return stream;
        if (Utils.isCtrlCode(codeLine)) return convertCtrl(stream);
        if (Utils.isDotCtrlCode(codeLine)) return convertDotCtrl(stream);

        while (Utils.stringSplit(stream.expr()).length > 1) {
            stream = convertDot(stream);
            stream = convertFront(stream);
            stream = convertMiddle(stream);
        }

        return convertSet(stream);
    }

    /**
     * <p>转换{@code CtrlCode}类型函数</p>
     * <p>{@code CtrlCode} 为无副作用的以{@code ()}形式内接调用函数.</p>
     * <p>有效函数名为:{@code
     * print printchar format wait ubind stop end
     * jump jump2 printf}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertCtrl(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();
        var ref = new Object() {
            int midNum = stream.stat();
        };

        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put("print(", s -> "print " + s);
            put("printchar(", s -> "printchar " + s);
            put("format(", s -> "format " + s);
            put("wait(", s -> "wait " + s);
            put("stop(", _ -> "stop");
            put("end(", _ -> "end");

            put("ubind(", s -> "ubind " + s);
            put("uctrl(", s -> "ucontrol " + Utils.padParams(s.replace(',', ' '), 6));

            put("draw(", s -> "draw " + Utils.padParams(s.replace(',', ' '), 7));

            put("jump(", s -> {
                final String defaultTarget = "DEFAULT", defaultCondition = "always 0 0";
                String target, condition = defaultCondition;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                target = paramsMap.getOrDefault("main", defaultTarget);

                s = paramsMap.getOrDefault("when", "");
                String[] splitList = Utils.stringSplitPro(s);
                if (splitList.length > 1) {
                    stdIOStream bashCache = convertCodeLine(stdIOStream.from(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        String bashLast = bashCache.bash().getLast();
                        condition = Utils.getCondition(bashLast);
                        if (!condition.equals(defaultCondition)) bashCache.bash().removeLast();
                        else if (!bashCache.expr().isEmpty())
                            condition = String.join(" ", "notEqual", bashCache.expr(), "0");
                        bashList.addAll(bashCache.bash());
                    }
                } else condition = String.join(" ", "notEqual", s, "0");
                return String.join(" ", "jump", target, condition);
            });

            put("jump2(", s -> {
                String[] strSplit = Utils.stringSplit(s);
                if (strSplit.length > 1) s = "@counter=@counter" + s;
                else s = "@counter=" + s;

                stdIOStream jump2stream = convertCodeLine(stdIOStream.from(s));
                bashList.addAll(jump2stream.bash());
                return jump2stream.expr();
            });
            put("printf(", s -> {
                String[] parts = s.split(",");
                if (parts.length < 2) return "print " + s;
                bashList.add("print " + parts[0]);
                IntStream.range(1, parts.length).mapToObj(i ->
                        "format " + parts[i]).forEach(bashList::add);
                return "";
            });
            put("tag(", s -> "::" + s);
        }};

        final List<String> jumpCtrlKeys = List.of(new String[]{"jump(", "jump2("});
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            if (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotCtrl(expr, start);
                String s = expr.substring(start + entry.getKey().length(), end).trim();
                String[] splitList = Utils.stringSplitPro(s);
                if (splitList.length > 1 && !jumpCtrlKeys.contains(entry.getKey())) {
                    stdIOStream bashCache = convertCodeLine(stdIOStream.from(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        bashList.addAll(bashCache.bash());
                        String midVariable = "mid." + ref.midNum;
                        expr = expr.replace(s, midVariable);
                        s = midVariable;
                    }
                }
                bashList.add(entry.getValue().apply(s));
            }
        }

        return stdIOStream.from(bashList);
    }

    /**
     * <p>转换{@code DotCtrlCode}类型函数</p>
     * <p>{@code DotCtrlCode} 为无副作用的以{@code .}形式后接调用函数.</p>
     * <p>有效函数名为:{@code
     * enable shoot config color unpack dflush pflush ulocate}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertDotCtrl(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();
        String finalExpr = expr;
        var ref = new Object() {
            final String block = Utils.getDotCtrlBlock(finalExpr);
            int midNum = stream.stat();
        };

        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put("ctrl(", s -> "control " + Utils.padParams(s.replace(',', ' '), 5));
            put("enable(", s -> "control enabled " + ref.block + " " + Utils.padParams(s, 4));
            put("config(", s -> "control config " + ref.block + " " + Utils.padParams(s, 4));
            put("color(", s -> "control color " + ref.block + " " + Utils.padParams(s, 4));
            put("shoot(", s -> {
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

            put("ulocate(", s -> {
                if (s.isEmpty())
                    return "ulocate building core 0 null " + ref.block + ".x " + ref.block + ".y " + ref.block + ".f " + ref.block;
                String[] parts = s.split("\\.");
                String locateType, ore = "null", building = "core", enemy = "0";
                if (!s.contains(")")) locateType = s;
                else locateType = s.substring(0, s.indexOf(")"));
                final String[] buildings = {"core", "storage", "generator", "turret", "factory", "repair", "battery", "reactor", "drill", "shield"};
                for (var build : buildings)
                    if (locateType.equals(build)) {
                        locateType = "building";
                        building = build;
                    }

                for (String part : parts) {
                    if (!part.endsWith(")")) part = part + ")";
                    String bracketContent = part.substring(part.indexOf('(') + 1, part.indexOf(')'));

                    if (bracketContent.isEmpty()) continue;
                    if (part.startsWith("ore(")) ore = bracketContent;
                    else if (part.startsWith("building(")) building = bracketContent;
                    else if (part.startsWith("enemy")) enemy = bracketContent;
                }
                return "ulocate " + locateType + " " + building + " " + enemy + " " + ore + " " + ref.block + ".x " + ref.block + ".y " + ref.block + ".f " + ref.block;
            });

            put("unpack(", s -> "unpackcolor " + Utils.padParams(s.split(","), 4) + " " + ref.block);
            put("pflush(", _ -> "printflush " + ref.block);
            put("dflush(", _ -> "drawflush " + ref.block);
            put("write(", s -> {
                String[] parts = s.split(",");
                String content = "null", bit = "0";
                if (parts.length > 0) content = parts[0];
                if (parts.length > 1) bit = parts[1];
                return "write " + content + " " + ref.block + " " + bit;
            });
        }};

        final List<String> chainDCtrlKeys = List.of(new String[]{"shoot(", "ulocate("});
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotCtrl(expr, start);
                String s = expr.substring(start + entry.getKey().length(), end).trim();
                String[] splitList = Utils.stringSplit(s);
                if (splitList.length > 1 && !chainDCtrlKeys.contains(entry.getKey())) {
                    stdIOStream bashCache = convertCodeLine(stdIOStream.from(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        bashList.addAll(bashCache.bash());
                        String midVariable = "mid." + ref.midNum;
                        expr = expr.replace(s, midVariable);
                        s = midVariable;
                        end = Utils.getEndDotCtrl(expr, start);
                    }
                }
                String result = entry.getValue().apply(s.trim());
                bashList.add(result);
                expr = expr.substring(0, start - 1) + expr.substring(end + 1);
            }
        }
        if (expr.equals(ref.block)) expr = "";
        return new stdIOStream(bashList, expr);
    }

    /**
     * <p>转换{@code DotCode}类型函数</p>
     * <p>{@code DotCode} 为有副作用的以{@code .}形式后接调用函数.</p>
     * <p>有效函数名为: {@code sensor read}</p>
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
        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put("sensor(", s -> "sensor mid." + ref.midNum + " " + ref.block + " " + s);
            put("read(", s -> "read mid." + ref.midNum + " " + ref.block + " " + s);
        }};

        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey());
                String[] strSplit = Utils.stringSplit(expr.substring(0, start - 1));
                ref.block = strSplit[strSplit.length - 1];

                int end = Utils.getEndBracket(expr, start);

                String s = expr.substring(start + entry.getKey().length(), end).trim();
                String[] splitList = Utils.stringSplit(s);
                String midVariable;
                if (splitList.length > 1) {
                    stdIOStream bashCache = convertCodeLine(stdIOStream.from(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        bashList.addAll(bashCache.bash());
                        midVariable = "mid." + ref.midNum;
                        expr = expr.replace(s, midVariable);
                        s = midVariable;
                    }
                }
                String result = entry.getValue().apply(s.trim());
                bashList.add(result);
                expr = expr.substring(0, start - ref.block.length() - 1) + "mid." + ref.midNum + expr.substring(end + 1);
                ref.midNum++;
            }
        }
        return new stdIOStream(bashList, expr, ref.midNum);
    }

    /**
     * <p>转换{@code FrontCode}类型函数</p>
     * <p>{@code FrontCode} 为有副作用的以{@code ()}形式内接调用函数.</p>
     * <p>有效函数名为:{@code
     * not abs sign floor ceil round sqrt rand sin cos tan asin acos atan
     * ln lg lb max min len angle angleDiff noise log pack
     * link block unit item liquid team sensor uradar radar}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertFront(stdIOStream stream) {
        ArrayList<String> bashList = new ArrayList<>(stream.bash());
        var ref = new Object() {
            int midNum = stream.stat();
        };

        Map<String, Function<String, String>> funcHandlers_high = new HashMap<>() {{
            put("not(", s -> "op not mid." + ref.midNum + " " + s + " 0");
            put("abs(", s -> "op abs mid." + ref.midNum + " " + s + " 0");
            put("sign(", s -> "op sign mid." + ref.midNum + " " + s + " 0");
            put("floor(", s -> "op floor mid." + ref.midNum + " " + s + " 0");
            put("ceil(", s -> "op ceil mid." + ref.midNum + " " + s + " 0");
            put("round(", s -> "op round mid." + ref.midNum + " " + s + " 0");
            put("sqrt(", s -> "op sqrt mid." + ref.midNum + " " + s + " 0");
            put("rand(", s -> "op rand mid." + ref.midNum + " " + s + " 0");
            put("asin(", s -> "op asin mid." + ref.midNum + " " + s + " 0");
            put("acos(", s -> "op acos mid." + ref.midNum + " " + s + " 0");
            put("atan(", s -> "op atan mid." + ref.midNum + " " + s + " 0");
            put("ln(", s -> "op log mid." + ref.midNum + " " + s + " 0");
            put("lg(", s -> "op log10 mid." + ref.midNum + " " + s + " 0");
            put("lb(", s -> "op logn mid." + ref.midNum + " " + s + " 2");
            put("max(", s -> {
                String[] paramParts = s.split(",");
                return "op max mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("min(", s -> {
                String[] paramParts = s.split(",");
                return "op min mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("len(", s -> {
                String[] paramParts = s.split(",");
                return "op len mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("angle(", s -> {
                String[] paramParts = s.split(",");
                return "op angle mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("angleDiff(", s -> {
                String[] paramParts = s.split(",");
                return "op angleDiff mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("noise(", s -> {
                String[] paramParts = s.split(",");
                return "op noise mid." + ref.midNum + " " + paramParts[0].trim() + " " + paramParts[1].trim();
            });
            put("log(", s -> {
                String[] paramParts = s.split(",");
                return "op logn mid." + ref.midNum + " " + paramParts[1].trim() + " " + paramParts[0].trim();
            });
            put("link(", s -> "getlink mid." + ref.midNum + " " + s);
            put("block(", s -> "lookup block mid." + ref.midNum + " " + s);
            put("unit(", s -> "lookup unit mid." + ref.midNum + " " + s);
            put("item(", s -> "lookup item mid." + ref.midNum + " " + s);
            put("liquid(", s -> "lookup liquid mid." + ref.midNum + " " + s);
            put("team(", s -> "lookup team mid." + ref.midNum + " " + s);
            put("pack(", s -> "packcolor mid." + ref.midNum + " " + Utils.padParams(s.split(","), 4));
            put("uradar(", s -> {
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
        }};

        Map<String, Function<String, String>> funcHandlers_low = new HashMap<>() {{
            put("sin(", s -> "op sin mid." + ref.midNum + " " + s);
            put("cos(", s -> "op cos mid." + ref.midNum + " " + s);
            put("tan(", s -> "op tan mid." + ref.midNum + " " + s);
            put("radar(", s -> {
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
        }};

        String expr = stream.expr();

        for (Map<String, Function<String, String>> handlers : List.of(funcHandlers_high, funcHandlers_low)) {
            for (Map.Entry<String, Function<String, String>> entry : handlers.entrySet()) {
                while (expr.contains(entry.getKey())) {
                    int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotCtrl(expr, start);
                    String s = expr.substring(start + entry.getKey().length(), end).trim();
                    String[] splitList = Utils.stringSplit(s);
                    if (splitList.length > 1) {
                        stdIOStream bashCache = convertCodeLine(stdIOStream.from(s, ref.midNum));
                        if (!bashCache.bash().isEmpty()) {
                            ref.midNum = bashCache.stat();
                            bashList.addAll(bashCache.bash());
                            expr = expr.replace(s, bashCache.expr());
                            end = Utils.getEndDotCtrl(expr, start);
                            s = bashCache.expr();
                        }
                    }
                    String result = entry.getValue().apply(s);
                    bashList.add(result);

                    String regex = expr.substring(start, end + 1);
                    expr = expr.replace(regex, "mid." + ref.midNum);
                    ref.midNum++;
                }
            }
        }

        return new stdIOStream(bashList, expr, ref.midNum);
    }


    /**
     * <p>转换{@code MidCode}类型函数</p>
     * <p>{@code MidCode}为有副作用的以可逆波兰化形式中接调用函数,</p>
     * <p>有效函数名详见{@link Utils#operatorKeyMap operators}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertMiddle(stdIOStream stream) {
        String[] rpnArray = Utils.generateRpn(stream.expr());
        ArrayList<String> stack = new ArrayList<>();
        ArrayList<String> bashList = stream.bash();
        var ref = new Object() {
            int midNum = stream.stat();
        };
        final Map<String, String> operatorMap = Utils.operatorKeyMap;
        final Map<String, Integer> offsetMap = Utils.operatorOffsetMap;

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
                        int ctrlOffset = offsetMap.getOrDefault(bashLast.split(" ")[0], -1);
                        if (ctrlOffset != -1 && bashLast.split(" ")[ctrlOffset].equals(stack.getLast())) {
                            result = bashLast.replaceFirst(stack.getLast(), stack.getFirst());
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

    /**
     * <p>转换{@code set}类型函数的非孤立行</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertSet(stdIOStream stream) {
        final Map<String, Integer> offsetMap = Utils.operatorOffsetMap;
        ArrayList<String> bashList = stream.bash();
        if (bashList.isEmpty()) return stdIOStream.empty();
        for (int i = 1; i < bashList.size(); i++) {
            var bashLast = bashList.get(i);
            if (bashLast.startsWith("set ")) {
                String[] setInfos = bashLast.split(" ");
                String var0 = setInfos[1], midVar = setInfos[2];
                String bashFormer = bashList.get(i - 1);
                int ctrlOffset = offsetMap.getOrDefault(bashFormer.split(" ")[0], -1);
                if (ctrlOffset != -1 && bashFormer.split(" ")[ctrlOffset].equals(midVar)) {
                    bashList.set(i - 1, bashFormer.replaceFirst(midVar, var0));
                    bashList.remove(i);
                    i--;
                }
            }
        }
        return stdIOStream.from(bashList, stream.stat());
    }

    /**
     * 转换{@code if/for/while}为原生{@code jump}
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertJump(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        bashList.removeIf(String::isEmpty);
        var ref = new Object() {
            int tag = stream.stat();
        };

        final String keyStart = "{", keyEnd = "}";
        while (true) {
            int matchIndex = 0, lineIndex = -1, line2Index = -1;
            for (int i = 0; i < bashList.size(); i++) {
                String line = bashList.get(i);
                if (line.endsWith(keyStart)) {
                    if (lineIndex == -1) lineIndex = i;
                    matchIndex++;
                }
                if (line.startsWith(keyEnd)) {
                    if (matchIndex == 1) line2Index = i;
                    matchIndex--;
                }
                if (line2Index != -1) break;
            }
            if (line2Index == -1) {
                if (lineIndex != -1) {
                    Utils.printError("Error: {} not match: No such operation.");
                    return stdIOStream.empty();
                } else break;
            }

            String line = bashList.get(lineIndex), line2 = bashList.get(line2Index);
            ArrayList<String> bashCache;
            int line2Offset = 0;
            if (line.startsWith("do{")) {
                bashList.set(lineIndex, "::PRESERVE_TAG." + ref.tag);
                line2 = line2.substring(line2.indexOf("while(") + 6, line2.lastIndexOf(")"));

                bashCache = convertCodeLine(stdIOStream.from(line2)).toStringArray();
            } else {
                String bracketContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
                if (line.startsWith("for(")) {
                    bashList.set(lineIndex, "::PRESERVE_TAG." + ref.tag);

                    String[] forParts = bracketContent.split(";");
                    if (forParts.length != 3) {
                        Utils.printError("Error: for() content not match");
                        return stdIOStream.empty();
                    }
                    ArrayList<String> initStream = convertCodeLine(stdIOStream.from(forParts[0])).toStringArray();
                    bashList.addAll(lineIndex, initStream);
                    line2Offset += initStream.size();

                    stdIOStream operateStream = convertCodeLine(stdIOStream.from(forParts[2]));
                    stdIOStream conditionStream = convertCodeLine(stdIOStream.from(forParts[1]));
                    bashCache = operateStream.toStringArray();
                    bashCache.addAll(conditionStream.toStringArray());

                } else { //must be if()
                    ArrayList<String> initStream = convertCodeLine(stdIOStream.from(bracketContent)).bash();
                    String[] conditionParts = initStream.getLast().split(" ", 4);
                    Map<String, String> reverseMap = Utils.operatorReverseMap;

                    String condition = reverseMap.get(conditionParts[1]) + " " + conditionParts[3];
                    initStream.removeLast();
                    initStream.add("jump PRESERVE_TAG." + ref.tag + " " + condition);
                    bashList.remove(lineIndex);
                    bashList.addAll(lineIndex, initStream);

                    line2Offset += initStream.size();
                    bashList.set(line2Index + line2Offset - 1, "::PRESERVE_TAG." + ref.tag);
                    ref.tag++;
                    continue;
                }
            }
            String[] conditionParts = bashCache.getLast().split(" ", 4);
            String condition = conditionParts[1] + " " + conditionParts[3];
            bashCache.removeLast();
            bashCache.add("jump PRESERVE_TAG." + ref.tag + " " + condition);

            bashList.remove(line2Index + line2Offset);
            bashList.addAll(line2Index + line2Offset, bashCache);
            ref.tag++;
        }
        return stdIOStream.from(bashList);
    }

    /**
     * 将{@code jump}中的动态链接转为静态
     *
     * @return {@code stdIOStream}
     */
    private static stdIOStream convertLink(stdIOStream stream) {
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
                            tagNum++;
                        }
                    }
                    if (index >= 0) {
                        String jumpString = "jump " + index + " " + String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                        if (jumpString.endsWith("null null null")) {
                            ArrayList<String> bashCache = new ArrayList<>(bashList);
                            bashCache.removeIf(s -> s.startsWith("::"));
                            IO.println(stdIOStream.from(bashCache).toString());
                            Utils.printError("Error at jump cast: line " + i);
                            Utils.printError(i + " " + line);
                            return stream;
                        }
                        bashList.set(i, jumpString);
                    } else {
                        ArrayList<String> bashCache = new ArrayList<>(bashList);
                        bashCache.removeIf(s -> s.startsWith("::"));
                        IO.println(stdIOStream.from(bashCache).toString());
                        Utils.printError("MdtC Compile Error: Jump() tag not found of [" + arg + "]");
                        Utils.printError(i + " " + line);

                        bashList.set(i, "jump 0 always 0 0");
                    }
                }
            }
        }

        bashList.removeIf(line -> line.startsWith("::"));
        return new stdIOStream(bashList, expr);
    }

    /**
     * 将函数块分离到函数,暂存于funcMap
     * {@code funcMap}结构: key:函数名, bash:函数体, expr:返回量, stat:标签数
     */
    static Map<String, stdIOStream> generateFuncMap(String funcBlock) {
        final String keyStart = "function", keyEnd = "}";
        final String[] keysJump = {"do{", "for(", "if("};
        Map<String, stdIOStream> funcMap = new HashMap<>();
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
                    bashCache.replaceAll(s -> Utils.replaceReserve(s, varsList, tagsList));

                    stdIOStream funcStream = stdIOStream.from(bashCache, returnValue, preserveTags.size());
                    funcMap.putIfAbsent(funcName, funcStream);
                    bashCache = new ArrayList<>();
                }
            }

            if (matchIndex > 0) bashCache.add(bash);

            if (bash.startsWith(keyStart)) {
                String[] functionHead = bash.split(" ");
                if (functionHead.length < 3) {
                    Utils.printError("Bad definition of function <anonymous>");
                    return funcMap;
                }
                returnValue = functionHead[1];
                int argsStart = functionHead[2].indexOf("(");
                funcName = functionHead[2].substring(0, argsStart + 1);
                funcArgs = functionHead[2]
                        .substring(argsStart + 1, Utils.getEndBracket(functionHead[2], argsStart))
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
        return funcMap;
    }
}
