public class MdtcToFormat {
    static void main(String[] args) {
        if (args.length > 0 && args[0].endsWith(".mdtc"))
            Main.fileDefault = args[0];
        Utils.formatFile();
    }

    public static String convertToFormat(String codeBlock) {
        final String[] keysStart = {"do{", "for(", "if(","function ","repeat("};
        final String[] keysEnd = {"}while(", "}"};
        final String[] lines = codeBlock.split("\n");
        int matchIndex = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
            String line = lines[i];
            line = line.trim();
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
                Utils.printRedError("Match brackets fail.\nat line" + i + ". " + line);
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
