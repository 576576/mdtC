import org.apache.commons.cli.*;

public class Main {
    private static final String VERSION_TAG = "1.1";

    public static boolean isGeneratePrimeCode = false;
    public static boolean isOpenOutput, isToFormat, isFormatOnly;
    public static String filePath = "", outPath = "";

    static void main(String[] args) {
        if (args.length != 0 && args[0].endsWith(".mdtc")) filePath = args[0];

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
            Utils.printError("No input file is detected.");
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
        } else Utils.printError("> " + filePath + "\nNot supported formats(.mdtc, .mdtcode)");
    }
}
