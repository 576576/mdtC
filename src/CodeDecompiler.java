import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CodeDecompiler {
    static void main() {
        //todo: 将连串带有mid.x的op及set转为赋值
        //todo: 转换jump为原始带标签jump
        //todo: 逆转换原生的jump为if/while
    }

    /**
     * 主转换函数入口
     */
    public static stdIOStream decompile(String codeBlock){
        ArrayList<String> bashList = new ArrayList<>();

        for (String line : codeBlock.split("\n")) {
            String lineToAdd = line.trim();
            if (!lineToAdd.isEmpty()) {
                bashList.add(lineToAdd);
            }
        }

        var result_ctrl_free = convertCtrl(stdIOStream.of(bashList));

        return result_ctrl_free;
    }

    /**
     * 逆转换输入代码中的{@code CtrlCode}
     */
    public static stdIOStream convertCtrl(stdIOStream stream) {
        ArrayList<String> bashList = stream.bash();
        String expr = stream.expr();

        Map<String, Function<String, String>> funcHandlers = new HashMap<>();
        funcHandlers.put("print", s -> "print(" + s + ")");
        funcHandlers.put("printchar", s -> "printchar(" + s + ")");
        funcHandlers.put("format", s -> "format(" + s + ")");
        funcHandlers.put("wait", s -> "wait(" + s + ")");
        funcHandlers.put("stop", _ -> "stop()");
        funcHandlers.put("end", _ -> "end()");

        funcHandlers.put("ubind", s -> "ubind(" + s + ")");
        funcHandlers.put("ucontrol", s -> "uctrl(" + s.replace(' ', ',') + ")");

        funcHandlers.put("draw", s -> "draw(" + s.replace(' ', ',') + ")");

        for (Map.Entry<String, Function<String, String>> entry : funcHandlers.entrySet()) {
            if (expr.startsWith(entry.getKey())) {
                int start = expr.indexOf(entry.getKey());
                String s = expr.substring(start + entry.getKey().length() + 1).trim();
                bashList.add(entry.getValue().apply(s));
            }
        }

        return stdIOStream.of(bashList);
    }

    /**
     * 逆转换输入代码中的{@code DotCtrlCode}
     */
    public static stdIOStream convertDotCtrl(stdIOStream stream) {
        return stream;
    }

    /**
     * 逆转换多重计算语句({@code DotCode} {@code FrontCode} {@code MidCode}),
     * 并重整为含=赋值行
     */
    public static stdIOStream foldOperation(stdIOStream stream) {
        return stream;
    }

    /**
     * 将原始jump绝对跳转转换为带标签的jump()
     */
    public static stdIOStream convertJump(stdIOStream stream) {
        return stream;
    }

    /**
     * 重整涉及@counter的op/set语句为jump2()
     */
    public static stdIOStream convertJump2(stdIOStream stream) {
        return stream;
    }

    /**
     * 逆转换原生的jump为if/while
     */
    public static stdIOStream convertPreJump(stdIOStream stream) {
        return stream;
    }
}
