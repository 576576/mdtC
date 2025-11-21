public class CodeFormatter {
    static void main() {

    }

    public static String format(String codeBlock) {
        final String[] keysStart = {"do{", "for(", "if(", "else{", "repeat(", "function "};
        final String[] keysEnd = {"}while(", "}"};
        final String[] lines = codeBlock.split("\n");
        int matchIndex = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                sb.append("\n");
                continue;
            }

            for (var key : keysEnd)
                if (line.startsWith(key)) {
                    matchIndex--;
                    break;
                }
            if (matchIndex < 0) {
                Utils.printError("Match brackets fail.\nat line" + i + ". " + line);
                return "";
            }

            sb.append("\t".repeat(matchIndex)).append(line).append("\n");

            for (var key : keysStart)
                if (line.startsWith(key)) {
                    matchIndex++;
                    break;
                }
        }
        while (matchIndex > 0) {
            matchIndex--;
            sb.append("\t".repeat(matchIndex)).append("}\n");
        }

        return sb.toString().trim();
    }
}
