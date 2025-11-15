import org.apache.commons.cli.*;

public class Main {
    private static final String VERSION_TAG = "1.0.1";

    public static boolean isGeneratePrimeCode = false;
    public static boolean isOpenOutput;
    public static boolean isToFormat;
    private static boolean isFormatOnly;

    static void main(String[] args) {
        String filePath = "", outPath = "";

        if (args.length != 0) filePath = args[0];

        Options options = new Options();
        options.addOption("v", "version", false, "显示版本信息");
        options.addOption("f", "format", false, "格式化代码");
        options.addOption("fo", "format-only", false, "仅格式化代码");
        options.addOption("i", "file", true, "指定文件路径");
        options.addOption("o", "output", true, "指定输出路径");
        options.addOption("oo", "open-out", false, "编译后打开输出");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("v")) IO.println("MdtC Compiler v" + Main.VERSION_TAG);
            isToFormat = cmd.hasOption("f");
            isFormatOnly = cmd.hasOption("fo");
            if (cmd.hasOption("i")) filePath = cmd.getOptionValue("i");
            if (cmd.hasOption("o")) outPath = cmd.getOptionValue("o");
            isOpenOutput = cmd.hasOption("oo");
        } catch (ParseException e) {
            System.err.println("解析命令行参数失败: " + e.getMessage());
        }

        if (filePath.isEmpty())
            filePath = IO.readln("Enter input file path below.\n> ");
        if (filePath.isEmpty()) {
            IO.println("\u001B[31mNo input file is detected.\u001B[0m");
            return;
        }

        if (isFormatOnly) {
            Utils.formatFile(filePath, outPath);
            return;
        }

        if (filePath.endsWith(".mdtc")) {
            if (outPath.isEmpty()) outPath = filePath + "ode";
            Utils.convertFile(filePath, outPath);
        } else if (filePath.endsWith(".mdtcode")) {
            if (outPath.isEmpty()) outPath = filePath.substring(0, filePath.length() - 3);
            Utils.convertFileReverse(filePath, outPath);
        } else IO.println("> " + filePath + "\n\u001B[31mNot supported formats(.mdtc, .mdtcode)\u001B[0m");
    }
}
