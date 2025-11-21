import java.util.ArrayList;
import java.util.List;

public record stdFuncStream(String funcName, int varsNum, ArrayList<String> funcBody, List<String> varsList, List<String> tagsList) {

    public stdFuncStream(String funcName,ArrayList<String> funcBody, List<String> varsList,List<String> tagsList) {
        this(funcName,varsList.size(),funcBody,  varsList, tagsList);
    }

    public static stdFuncStream of(String funcName,ArrayList<String> funcBody, List<String> varsList,List<String> tagsList) {
        return new stdFuncStream(funcName,funcBody, varsList,tagsList);
    }

    public String name(){
        return funcName;
    }

    public int vars(){
        return varsNum;
    }
}
