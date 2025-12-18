import java.util.List;

public record stdFuncStream(String funcName, List<String> funcBody, List<String> varsList, List<String> tagsList) {

    public static stdFuncStream of(String funcName, List<String> funcBody, List<String> varsList, List<String> tagsList) {
        return new stdFuncStream(funcName, funcBody, varsList, tagsList);
    }

    public String name() {
        return funcName;
    }

    public int varsCount() {
        return varsList.size();
    }
}
