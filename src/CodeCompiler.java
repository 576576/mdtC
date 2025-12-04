import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class CodeCompiler {
    static void main() {
        IO.println(convertCodeLine(stdCodeStream.of("s=min(2+3,5)")).toStringArray());
    }

    /**
     * 主转换函数入口
     *
     * @return {@code stdIOStream}
     */
    public static String compile(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>();
        Map<Integer, stdFuncStream> funcMap = new HashMap<>();

        codeBlock = insertImport(codeBlock);

        int funcStartIndex = codeBlock.indexOf("function ");
        if (funcStartIndex != -1) {
            String funcBlock = codeBlock.substring(funcStartIndex);
            funcMap = generateFuncMap(funcBlock);
            codeBlock = codeBlock.substring(0, funcStartIndex);
        }
        String codeCache = "";
        while (!codeBlock.equals(codeCache)) {
            while (!codeBlock.equals(codeCache)) {
                codeCache = codeBlock;
                codeBlock = insertFunc(codeBlock, funcMap);
            }
            codeCache = codeBlock;
            codeBlock = unfoldRepeat(codeBlock);
        }

        if (Main.primeCodeLevel == 1) {
            String filePath = Main.filePath;
            if (filePath.endsWith(".mdtc") && !filePath.endsWith("_prime.mdtc")) {
                String primeCodePath = filePath.replace(".mdtc", "_prime.mdtc");
                String writeContent = CodeFormatter.format(codeBlock);
                Utils.writeFile(primeCodePath, writeContent);

                IO.println("PrimeCode output at:\n> " + primeCodePath);
            } else IO.println("Skip writing prime code.");
        }

        int refNumMax = 1;
        for (String line : codeBlock.split("\n")) {
            if (!line.trim().isEmpty()) {
                stdCodeStream convertedLine = convertCodeLine(stdCodeStream.of(line.trim()));
                refNumMax = Math.max(refNumMax, convertedLine.stat());
                bashList.addAll(convertedLine.toStringArray());
            }
        }

        stdCodeStream result_set = convertSet(stdCodeStream.of(bashList, refNumMax));
        stdCodeStream result_jump = convertJump(result_set);
        return convertLink(result_jump).toString();
    }

    private static String insertImport(String codeBlock) {
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        List<String> importLines = bashList.stream()
                .filter(line -> line.startsWith("import ")).toList();
        bashList.removeIf(line -> line.startsWith("import "));
        StringBuilder codeBlockBuilder = new StringBuilder(Utils.listToCodeBlock(bashList));
        for (var line : importLines) {
            String importPath = line.substring(6).trim();
            if (!importPath.endsWith("mdtc")) importPath += ".libmdtc";
            String importBlock = Utils.readFile(importPath);
            int funcStartIndex = importBlock.indexOf("function ");
            if (funcStartIndex != -1) {
                String funcBlock = importBlock.substring(funcStartIndex);
                codeBlockBuilder.append("\n").append(funcBlock);
            }
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
        final String[] keysJump = {"do{", "for(", "if(", "else{"};

        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();
        bashList.replaceAll(String::trim);

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
                    List<String> tagsList = new ArrayList<>(bashCache.stream().
                            filter(line -> line.startsWith("::"))
                            .map(s -> s.substring(2)).toList());

                    ArrayList<String> bashTo, bashToAdd = new ArrayList<>();
                    String finalVar = preserveVar;
                    for (int j = 1; j <= repeatRoutes; j++) {
                        String prefix = "REPEAT." + ref.midNum + "_";
                        String replaceToVar = preserveVar + j;

                        bashTo = new ArrayList<>(bashCache);
                        bashTo.replaceAll(s -> Utils.replaceTags(s, tagsList, prefix));
                        bashTo.replaceAll(s -> Utils.replaceVar(s, finalVar, replaceToVar));
                        bashToAdd.addAll(bashTo);
                        ref.midNum++;
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
                int start = bash.indexOf("("), end = Utils.getEndBracket(bash, start);
                String bracketContent = bash.substring(start + 1, end);
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
        return stdCodeStream.of(bashList).toString().trim();
    }

    /**
     * 内嵌函数到代码块
     *
     * @return {@code stdIOStream}
     */
    private static String insertFunc(String codeBlock, Map<Integer, stdFuncStream> funcMap) {
        var ref = new Object() {
            int midNum = 1;
        };
        ArrayList<String> bashList = new ArrayList<>(List.of(codeBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();

        for (String bash : bashList) {
            for (var func : funcMap.entrySet()) {
                stdFuncStream funcStream = func.getValue();
                String funcName = funcStream.name();
                List<String> varsList = funcStream.varsList();
                int varsNum = funcStream.vars();

                int ignoreIndex = 0;
                while (bash.contains(funcName)) {
                    int start = bash.indexOf(funcName, ignoreIndex), end = Utils.getEndBracket(bash, start);
                    if (end == -1) break;
                    String funcArgs = bash.substring(start + funcName.length(), end);
                    String[] args2Array = funcArgs.split(",");
                    if (args2Array.length != varsNum - 1) {
                        ignoreIndex = end;
                        continue;
                    }

                    ArrayList<String> funcBody = new ArrayList<>(funcStream.funcBody());
                    String prefix = "FUNC." + ref.midNum + "_";
                    List<String> tagsList = funcStream.tagsList();
                    funcBody.replaceAll(s -> Utils.replaceTags(s, tagsList, prefix));

                    String returnValue = varsList.getFirst(), return2Value;
                    if (returnValue.equals("void")) return2Value = "";
                    else return2Value = prefix + returnValue;
                    List<String> vars2List = new ArrayList<>() {{
                        add(return2Value);
                        addAll(Arrays.asList(args2Array));
                    }};
                    funcBody.replaceAll(s -> Utils.replaceVars(s, varsList, vars2List));

                    bashCache.addAll(funcBody);
                    bash = bash.substring(0, start) + return2Value + bash.substring(end + 1);
                    ref.midNum++;
                }
            }
            if (!bash.trim().isEmpty()) bashCache.add(bash);
        }
        return Utils.listToCodeBlock(bashCache);
    }

    /**
     * <p>转换一行代码</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertCodeLine(stdCodeStream stream) {
        String codeLine = stream.expr();
        if (Utils.isSpecialControl(codeLine)) return stream;
        if (Utils.isCtrlCode(codeLine)) return convertCtrl(stream);
        if (Utils.isDotCtrlCode(codeLine)) return convertDotCtrl(stream);

        while (Utils.stringSplit(stream.expr()).size() > 1) {
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
    private static stdCodeStream convertCtrl(stdCodeStream stream) {
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

            put("ushoot(", s -> {
                final String defaultTarget = "@this", defaultShooting = "1";
                String target, ctrlType, shooting;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                shooting = paramsMap.getOrDefault("main", defaultShooting);
                target = paramsMap.getOrDefault("target", defaultTarget);
                ctrlType = target.contains(",") ? "target" : "targetp";

                target = target.replace(',', ' ');
                String shootArgs = Utils.padParams(target + " " + shooting, 5);
                return "ucontrol " + ctrlType + " " + shootArgs;
            });

            put("draw(", s -> "draw " + Utils.padParams(s.replace(',', ' '), 7));

            put("jump(", s -> {
                final String defaultTarget = "DEFAULT", defaultCondition = "always 0 0";
                String target, condition = defaultCondition;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                target = paramsMap.getOrDefault("main", defaultTarget);

                s = paramsMap.getOrDefault("when", "");
                List<String> splitList = Utils.stringSplitPro(s);
                if (splitList.size() > 1) {
                    stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        String bashLast = bashCache.bash().getLast();
                        condition = Utils.getCondition(bashLast);
                        if (!condition.equals(defaultCondition)) bashCache.bash().removeLast();
                        else if (!bashCache.expr().isEmpty())
                            condition = String.join(" ", "notEqual", bashCache.expr(), "0");
                        bashList.addAll(bashCache.bash());
                    }
                } else if (splitList.size() == 1)
                    condition = String.join(" ", "notEqual", s, "0");

                return String.join(" ", "jump", target, condition);
            });

            put("jump2(", s -> {
                List<String> strSplit = Utils.stringSplitPro(s);
                if (strSplit.size() > 1) s = "@counter=@counter" + s;
                else s = "@counter=" + s;

                stdCodeStream jump2stream = convertCodeLine(stdCodeStream.of(s));
                bashList.addAll(jump2stream.bash());
                return "";
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
            put("raw(", s -> s.substring(1, s.length() - 1));
        }};

        final List<String> ignoreKeys = List.of("jump(", "jump2(", "draw(", "ushoot(", "tag(", "raw(");
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotChain(expr, start);
                if (end == -1) {
                    Utils.printError("Bracket unmatched of frontCode:\n> " + expr);
                    return stream;
                }
                String s = expr.substring(start + entry.getKey().length(), end).trim();
                List<String> splitList = Utils.stringSplitPro(s);
                if (splitList.size() > 1 && !ignoreKeys.contains(entry.getKey())) {
                    List<String> splitParts = Utils.bracketPartSplit(s);
                    for (int i = 0; i < splitParts.size(); i++) {
                        String part = splitParts.get(i);

                        stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(part, ref.midNum));
                        bashList.addAll(bashCache.bash());
                        splitParts.set(i, bashCache.expr());
                        ref.midNum = bashCache.stat();
                    }
                    s = String.join(",", splitParts);

                }
                String result = entry.getValue().apply(s);
                bashList.add(result);
                expr = "";
            }
        }
        return stdCodeStream.of(bashList);
    }

    /**
     * <p>转换{@code DotCtrlCode}类型函数</p>
     * <p>{@code DotCtrlCode} 为无副作用的以{@code .}形式后接调用函数.</p>
     * <p>有效函数名为:{@code
     * enable shoot config color unpack dflush pflush ulocate}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertDotCtrl(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();
        String finalExpr = expr;
        var ref = new Object() {
            final String block = Utils.getDotBlock(finalExpr);
            int midNum = stream.stat();
        };

        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put(".ctrl(", s -> "control " + Utils.padParams(s.replace(',', ' '), 5));
            put(".enable(", s -> "control enabled " + ref.block + " " + Utils.padParams(s, 4));
            put(".config(", s -> "control config " + ref.block + " " + Utils.padParams(s, 4));
            put(".color(", s -> "control color " + ref.block + " " + Utils.padParams(s, 4));
            put(".shoot(", s -> {
                final String defaultTarget = "@this", defaultShooting = "1";
                String target, ctrlType, shooting;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                shooting = paramsMap.getOrDefault("main", defaultShooting);
                target = paramsMap.getOrDefault("target", defaultTarget);
                ctrlType = target.contains(",") ? "shoot" : "shootp";

                target = target.replace(',', ' ');
                String shootArgs = Utils.padParams(target + " " + shooting, 4);
                return "control " + ctrlType + " " + ref.block + " " + shootArgs;
            });

            put(".ulocate(", s -> {
                final String defaultType = "ore", defaultOre = "0", defaultBuilding = "core", defaultEnemy = "0";
                String locateType, ore, building, enemy;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                locateType = paramsMap.getOrDefault("main", defaultType);
                ore = paramsMap.getOrDefault("ore", defaultOre);
                building = paramsMap.getOrDefault("building", defaultBuilding);
                enemy = paramsMap.getOrDefault("enemy", defaultEnemy);

                final List<String> buildings = List.of("core", "storage", "generator", "turret", "factory", "repair", "battery", "reactor", "drill", "shield");
                if (buildings.contains(locateType)) {
                    building = locateType;
                    locateType = "building";
                }
                return String.join(" ", "ulocate", locateType, building, enemy, ore, ref.block + ".x", ref.block + ".y", ref.block + ".f", ref.block);
            });

            put(".unpack(", s -> "unpackcolor " + Utils.padParams(s.split(","), 4) + " " + ref.block);
            put(".pflush(", _ -> "printflush " + ref.block);
            put(".dflush(", _ -> "drawflush " + ref.block);
            put(".write(", s -> {
                String[] parts = s.split(",");
                String content = "null", bit = "0";
                if (parts.length > 0) content = parts[0];
                if (parts.length > 1) bit = parts[1];
                return "write " + content + " " + ref.block + " " + bit;
            });
        }};

        final List<String> ignoreKeys = List.of(".shoot(", ".ulocate(");
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotChain(expr, start);
                if (end == -1) {
                    Utils.printError("Bracket unmatched of frontCode:\n> " + expr);
                    return stream;
                }
                String s = expr.substring(start + entry.getKey().length(), end).trim();
                List<String> splitList = Utils.stringSplitPro(s);
                if (splitList.size() > 1 && !ignoreKeys.contains(entry.getKey())) {
                    List<String> splitParts = Utils.bracketPartSplit(s);
                    for (int i = 0; i < splitParts.size(); i++) {
                        String part = splitParts.get(i);

                        stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(part, ref.midNum));
                        bashList.addAll(bashCache.bash());
                        splitParts.set(i, bashCache.expr());
                        ref.midNum = bashCache.stat();
                    }
                    String reduceContent = String.join(",", splitParts);
                    expr = expr.replace(s, reduceContent);
                    end = Utils.getEndDotChain(expr, start);
                    s = reduceContent;
                }
                String result = entry.getValue().apply(s);
                bashList.add(result);

                expr = expr.substring(0, start) + expr.substring(end + 1);
            }
        }
        if (expr.equals(ref.block)) expr = "";
        return new stdCodeStream(bashList, expr);
    }

    /**
     * <p>转换{@code DotCode}类型函数</p>
     * <p>{@code DotCode} 为有副作用的以{@code .}形式后接调用函数.</p>
     * <p>有效函数名为: {@code sensor read}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertDot(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();
        var ref = new Object() {
            int midNum = stream.stat();
            String block = "";
        };
        Map<String, Function<String, String>> funcHandlers = new HashMap<>() {{
            put(".sensor(", s -> "sensor mid." + ref.midNum + " " + ref.block + " " + s);
            put(".read(", s -> "read mid." + ref.midNum + " " + ref.block + " " + s);
            put(".orElse(", s -> {
                final String defaultTarget = "0", defaultCondition = "always 0 0";
                String target, condition = defaultCondition;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                target = paramsMap.getOrDefault("main", defaultTarget);

                s = paramsMap.getOrDefault("when", "");
                List<String> splitList = Utils.stringSplitPro(s);
                if (splitList.size() > 1) {
                    stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(s, ref.midNum));
                    if (!bashCache.bash().isEmpty()) {
                        ref.midNum = bashCache.stat();
                        String bashLast = bashCache.bash().getLast();
                        condition = Utils.getCondition(bashLast);
                        if (!condition.equals(defaultCondition)) bashCache.bash().removeLast();
                        else if (!bashCache.expr().isEmpty())
                            condition = String.join(" ", "notEqual", bashCache.expr(), "0");
                        bashList.addAll(bashCache.bash());
                    }
                } else if (splitList.size() == 1)
                    condition = String.join(" ", "notEqual", s, "0");

                condition = Utils.reverseCondition(condition);
                return String.join(" ", "select", "mid." + ref.midNum, condition, ref.block, target);
            });
        }};

        final List<String> ignoreKeys = List.of(".orElse(");
        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            while (expr.contains(entry.getKey())) {
                int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotChain(expr, start);
                List<String> splitList = Utils.stringSplitPro(expr.substring(0, start));
                ref.block = splitList.getLast();

                String s = expr.substring(start + entry.getKey().length(), end).trim();
                splitList = Utils.stringSplitPro(s);
                String midVariable;
                if (splitList.size() > 1 && !ignoreKeys.contains(entry.getKey())) {
                    stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(s, ref.midNum));
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
                expr = expr.substring(0, start - ref.block.length()) + "mid." + ref.midNum + expr.substring(end + 1);
                ref.midNum++;
            }
        }
        return new stdCodeStream(bashList, expr, ref.midNum);
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
    private static stdCodeStream convertFront(stdCodeStream stream) {
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
                final String block = "0", defaultTarget = "enemy,any,any", defaultOrder = "1", defaultSort = "distance";
                String target, order, sort;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                target = paramsMap.getOrDefault("target", defaultTarget);
                order = paramsMap.getOrDefault("order", defaultOrder);
                sort = paramsMap.getOrDefault("sort", defaultSort);

                target = Utils.padParams(target.split(","), 3, "any");
                return "uradar " + target + " " + sort + " " + block + " " + order + " mid." + ref.midNum;
            });
        }};

        Map<String, Function<String, String>> funcHandlers_low = new HashMap<>() {{
            put("sin(", s -> "op sin mid." + ref.midNum + " " + s);
            put("cos(", s -> "op cos mid." + ref.midNum + " " + s);
            put("tan(", s -> "op tan mid." + ref.midNum + " " + s);
            put("radar(", s -> {
                final String defaultBlock = "@this", defaultTarget = "enemy,any,any", defaultOrder = "1", defaultSort = "distance";
                String block, target, order, sort;

                Map<String, String> paramsMap = Utils.getChainParams(s);
                block = paramsMap.getOrDefault("main", defaultBlock);
                target = paramsMap.getOrDefault("target", defaultTarget);
                order = paramsMap.getOrDefault("order", defaultOrder);
                sort = paramsMap.getOrDefault("sort", defaultSort);

                target = Utils.padParams(target.split(","), 3, "any");
                return "radar " + target + " " + sort + " " + block + " " + order + " mid." + ref.midNum;
            });
        }};

        String expr = stream.expr();

        final List<String> ignoreKeys = List.of("radar(", "uradar(");
        for (Map<String, Function<String, String>> handlers : List.of(funcHandlers_high, funcHandlers_low)) {
            for (Map.Entry<String, Function<String, String>> entry : handlers.entrySet()) {
                while (expr.contains(entry.getKey())) {
                    int start = expr.indexOf(entry.getKey()), end = Utils.getEndDotChain(expr, start);
                    if (end == -1) {
                        Utils.printError("Bracket unmatched of frontCode:\n> " + expr);
                        return stream;
                    }
                    String s = expr.substring(start + entry.getKey().length(), end).trim();
                    List<String> splitList = Utils.stringSplitPro(s);
                    if (splitList.size() > 1 && !ignoreKeys.contains(entry.getKey())) {
                        List<String> splitParts = Utils.bracketPartSplit(s);
                        for (int i = 0; i < splitParts.size(); i++) {
                            String part = splitParts.get(i);

                            stdCodeStream bashCache = convertCodeLine(stdCodeStream.of(part, ref.midNum));
                            bashList.addAll(bashCache.bash());
                            splitParts.set(i, bashCache.expr());
                            ref.midNum = bashCache.stat();
                        }
                        String reduceContent = String.join(",", splitParts);
                        expr = expr.replace(s, reduceContent);
                        end = Utils.getEndDotChain(expr, start);
                        s = reduceContent;
                    }
                    String result = entry.getValue().apply(s);
                    bashList.add(result);

                    String regex = expr.substring(start, end + 1);
                    expr = expr.replace(regex, "mid." + ref.midNum);
                    ref.midNum++;
                }
            }
        }

        return new stdCodeStream(bashList, expr, ref.midNum);
    }


    /**
     * <p>转换{@code MidCode}类型函数</p>
     * <p>{@code MidCode}为有副作用的以可逆波兰化形式中接调用函数,</p>
     * <p>有效函数名详见{@link Constant#operatorKeyMap operators}</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertMiddle(stdCodeStream stream) {
        String[] rpnArray = Utils.generateRpn(stream.expr());
        ArrayList<String> stack = new ArrayList<>();
        ArrayList<String> bashList = stream.bash();
        var ref = new Object() {
            int midNum = stream.stat();
        };
        final Map<String, String> operatorMap = Constant.operatorKeyMap;
        final Map<String, Integer> offsetMap = Constant.operatorOffsetMap;

        for (String token : rpnArray) {
            if (operatorMap.containsKey(token)) {
                String op = operatorMap.get(token);
                String midVar = "mid." + ref.midNum;
                if (!op.equals("set")) {
                    String arg1 = stack.get(stack.size() - 2), arg2 = stack.getLast();
                    String result = String.join(" ", "op", op, midVar, arg1, arg2);
                    if (op.equals("sub")) {
                        if (arg1.equals("0") && Utils.isNumeric(arg2))
                            result = "set " + midVar + " -" + arg2;
                    }
                    bashList.add(result);
                    stack.removeLast();
                    stack.removeLast();
                    stack.add(midVar);
                    ref.midNum++;
                } else {
                    String result = "set " + stack.getFirst() + " " + stack.getLast();
                    if (!bashList.isEmpty()) {
                        String bashLast = bashList.getLast();
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
        return new stdCodeStream(bashList, expr.toString(), ref.midNum);
    }

    /**
     * <p>转换{@code set}类型函数的非孤立行</p>
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertSet(stdCodeStream stream) {
        final Map<String, Integer> offsetMap = Constant.operatorOffsetMap;
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();

        if (bashList.isEmpty()) return stream;
        for (int i = 1; i < bashList.size(); i++) {
            String bashLast = bashList.get(i);
            if (bashLast.startsWith("set ")) {
                String[] setInfos = bashLast.split(" ");
                String var0 = setInfos[1], midVar = setInfos[2];
                String bashFormer = bashList.get(i - 1);
                int ctrlOffset = offsetMap.getOrDefault(bashFormer.split(" ")[0], -1);
                if (ctrlOffset != -1 && bashFormer.split(" ")[ctrlOffset].equals(midVar)) {
                    bashList.set(i - 1, bashFormer.replaceFirst(midVar, var0));
                    bashList.remove(i);
                    i--;
                    if (midVar.equals(expr)) expr = "";
                }
            }
        }
        return stdCodeStream.of(bashList, expr, stream.stat());
    }

    /**
     * 转换{@code if/for/while}为原生{@code jump}
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertJump(stdCodeStream stream) {
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
                    Utils.printError("Error: {} not match at line " + lineIndex);
                    Utils.printError(lineIndex + " " + bashList.get(lineIndex));
                    return stdCodeStream.of();
                } else break;
            }

            String line = bashList.get(lineIndex), line2 = bashList.get(line2Index);
            ArrayList<String> bashCache;
            String tagTo = "TAG." + ref.tag;

            if (line.startsWith("if(")) {
                tagTo += "_endIf";
                int start = line.indexOf("("), end = Utils.getEndBracket(line, start);
                String bracketContent = line.substring(start + 1, end);

                String jumpString = "jump(" + tagTo + ").when(" + bracketContent + ")";
                ArrayList<String> initStream = convertCtrl(stdCodeStream.of(jumpString)).bash();
                jumpString = Utils.reverseCondition(initStream.getLast());
                initStream.set(initStream.size() - 1, jumpString);

                bashList.set(line2Index, "::" + tagTo);
                bashList.remove(lineIndex);
                bashList.addAll(lineIndex, initStream);
                ref.tag++;
            } else if (line.startsWith("else{")) {
                tagTo += "_endElse";

                String endIfLine = bashList.get(lineIndex - 1);
                if (!endIfLine.startsWith("::") || !endIfLine.endsWith("_endIf")) {
                    Utils.printError("Error: else not match at line " + lineIndex);
                    return stdCodeStream.of();
                }
                String jumpString = "jump " + tagTo + " always 0 0";

                bashList.set(line2Index, "::" + tagTo);
                bashList.remove(lineIndex);
                bashList.add(lineIndex - 1, jumpString);
                ref.tag++;
            } else if (line.startsWith("do{")) {
                tagTo += "_do";
                int start = line2.indexOf("("), end = Utils.getEndBracket(line2, start);
                String bracketContent = line2.substring(start + 1, end);

                String jumpString = "jump(" + tagTo + ").when(" + bracketContent + ")";
                bashCache = convertCtrl(stdCodeStream.of(jumpString)).bash();

                bashList.remove(line2Index);
                bashList.addAll(line2Index, bashCache);
                bashList.set(lineIndex, "::" + tagTo);
                ref.tag++;
            } else if (line.startsWith("for(")) {
                String tagEnd = tagTo + "_endFor";
                tagTo += "_for";
                int start = line.indexOf("("), end = Utils.getEndBracket(line, start);
                String bracketContent = line.substring(start + 1, end);
                bashList.set(lineIndex, "::TAG." + ref.tag);

                String[] forParts = bracketContent.split(";");
                if (forParts.length != 3) {
                    Utils.printError("Error: for() content not match");
                    return stdCodeStream.of();
                }
                ArrayList<String> initStream = convertCodeLine(stdCodeStream.of(forParts[0])).bash();

                String jumpString = "jump(" + tagEnd + ").when(" + forParts[1] + ")";

                ArrayList<String> conditionStream = convertCtrl(stdCodeStream.of(jumpString)).bash();
                jumpString = Utils.reverseCondition(conditionStream.getLast());
                conditionStream.set(conditionStream.size() - 1, jumpString);
                conditionStream.addFirst("::" + tagTo);

                ArrayList<String> operateStream = convertCodeLine(stdCodeStream.of(forParts[2])).bash();
                operateStream.add("jump " + tagTo + " always 0 0");
                operateStream.add("::" + tagEnd);

                bashList.remove(line2Index);
                bashList.addAll(line2Index, operateStream);
                bashList.remove(lineIndex);
                bashList.addAll(lineIndex, conditionStream);
                bashList.addAll(lineIndex, initStream);
                ref.tag++;
            } else {
                Utils.printError("Undefined loop type of " + line);
            }
        }
        return stdCodeStream.of(bashList);
    }

    /**
     * 将{@code jump}中的动态链接转为静态
     *
     * @return {@code stdIOStream}
     */
    private static stdCodeStream convertLink(stdCodeStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();

        if (!bashList.isEmpty()) {
            bashList.add(bashList.size() - 1, "::END");
            if (bashList.getLast().startsWith("::"))
                bashList.add("end");
            if (!bashList.contains("::DEFAULT"))
                bashList.addFirst("::DEFAULT");
            bashList.addFirst("::HEAD");
        }

        if (Main.primeCodeLevel == 2) {
            String filePath = Main.filePath;
            if (filePath.endsWith(".mdtc") && !filePath.endsWith("_prime.mdtc")) {
                String primeCodePath = filePath.replace(".mdtc", "_prime.mdtc");
                String writeContent = Utils.listToCodeBlock(bashList);
                Utils.writeFile(primeCodePath, writeContent);

                IO.println("PrimeCode output at:\n> " + primeCodePath);
            } else IO.println("Skip writing prime code.");
        }

        int tagNum;
        for (int i = 0; i < bashList.size(); i++) {
            String line = bashList.get(i);
            tagNum = 0;
            if (line.startsWith("jump ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length > 1) {
                    String target = parts[1].trim();
                    int index = -1;
                    for (int j = 0; j < bashList.size(); j++) {
                        String codeLine = bashList.get(j);
                        if (bashList.get(j).startsWith("::")) {
                            String tag = codeLine.substring(2).trim();
                            if (tag.equals(target)) {
                                index = j - tagNum;
                                break;
                            }
                            tagNum++;
                        }
                    }
                    if (index >= 0) {
                        String jumpString = String.join(" ", "jump", index + "", parts[2]);
                        bashList.set(i, jumpString);
                    } else {
                        Utils.printError("Compile Error: jump() tag not found of [" + target + "]");
                        Utils.printError(i + 1 + " " + line);
                        return stream;
                    }
                }
            }
        }

        bashList.removeIf(line -> line.startsWith("::"));
        return new stdCodeStream(bashList, expr);
    }

    /**
     * 将函数块分离到函数,暂存于funcMap
     * {@code funcMap}结构: key:Hash(函数名,参数量), funcStream:函数体
     *
     * @return {@code stdFuncStream}
     */
    static HashMap<Integer, stdFuncStream> generateFuncMap(String funcBlock) {
        final String keyStart = "function", keyEnd = "}";
        final String[] keysJump = {"do{", "for(", "if(", "else{"};
        HashMap<Integer, stdFuncStream> funcMap = new HashMap<>();

        ArrayList<String> bashList = new ArrayList<>(List.of(funcBlock.split("\n")));
        ArrayList<String> bashCache = new ArrayList<>();

        int matchIndex = 0;
        String funcName = "";
        List<String> varsList = List.of(), tagsList;

        for (String bash : bashList) {
            if (bash.startsWith(keyEnd)) {
                matchIndex--;
                if (matchIndex == 0) {
                    tagsList = bashCache.stream()
                            .filter(s -> s.startsWith("::"))
                            .map(s -> s.substring(2))
                            .toList();

                    stdFuncStream funcStream = stdFuncStream.of(funcName, bashCache, varsList, tagsList);
                    int funcHash = Objects.hash(funcName, funcStream.varsNum());
                    funcMap.put(funcHash, funcStream);
                    bashCache = new ArrayList<>();
                }
            }

            if (matchIndex > 0) bashCache.add(bash.trim());

            if (bash.startsWith(keyStart)) {
                String[] funcSplit = bash.split(" ");
                if (funcSplit.length < 3) {
                    Utils.printError("Bad definition of function <anonymous>");
                    return funcMap;
                }
                String returnValue = funcSplit[1], funcHead = funcSplit[2];
                int argsStart = funcHead.indexOf("("), argsEnd = Utils.getEndBracket(funcHead, argsStart);
                funcName = funcHead.substring(0, argsStart + 1);
                if (argsEnd == -1) {
                    Utils.printError("Bad definition of function " + funcHead);
                }
                String[] funcArgs = funcHead.substring(argsStart + 1, argsEnd).split(",");

                varsList = new ArrayList<>() {{
                    add(returnValue);
                    addAll(Arrays.asList(funcArgs));
                }};

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
