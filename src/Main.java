import org.apache.commons.cli.*;

import java.util.List;

public class Main {
    private static final String VERSION_TAG = "1.2";
    public static boolean isToFormat, isFormatOnly, isOpenOutput;
    public static String filePath = "", outPath = "";
    public static int primeCodeLevel = 0;

    static void main(String[] args) {
        final List<String> supportFormatList = List.of(new String[]{".mdtc", ".mdtcode", ".libmdtc"});
        if (args.length != 0 && supportFormatList.contains(args[0])) filePath = args[0];

        Options options = new Options()
                .addOption("v", "version", false, "显示版本信息")
                .addOption("f", "format", false, "格式化代码")
                .addOption("fo", "format-only", false, "仅格式化代码")
                .addOption("i", "file", true, "指定文件路径")
                .addOption("o", "output", true, "指定输出路径")
                .addOption("oo", "open-out", false, "编译后打开输出")
                .addOption("gpc", "generate-prime-code", true, "产生中间代码(硬链接前)");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("v")) IO.println("MdtC Compiler v" + Main.VERSION_TAG);
            isToFormat = cmd.hasOption("f");
            isFormatOnly = cmd.hasOption("fo");
            if (cmd.hasOption("i")) filePath = cmd.getOptionValue("i");
            if (cmd.hasOption("o")) outPath = cmd.getOptionValue("o");
            isOpenOutput = cmd.hasOption("oo");
            if (cmd.hasOption("gpc")) primeCodeLevel = Integer.parseInt(cmd.getOptionValue("gpc"));
        } catch (ParseException e) {
            System.err.println("解析命令行参数失败: " + e.getMessage());
        }

        if (filePath.isEmpty())
            filePath = IO.readln("Enter input file path below.\n> ");
        if (filePath.isEmpty()) {
            Utils.printError("No input file is detected.");
            return;
        }

        if (filePath.endsWith(".libmdtc") || isFormatOnly)
            formatFile(filePath, outPath);
        else if (filePath.endsWith(".mdtc"))
            compileFile(filePath, outPath);
        else if (filePath.endsWith(".mdtcode"))
            decompileFile(filePath, outPath);
        else Utils.printError("Not supported formats(.mdtc, .mdtcode, .libmdtc).\n> " + filePath);
    }

    static void formatFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath;
        String inputContent = Utils.readFile(filePath);

        String outContent = CodeFormatter.format(inputContent);
        if (outContent.isEmpty()) {
            IO.println("Nothing to format.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Formatted output at:\n> " + outPath);
    }

    static void compileFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath + "ode";
        String inputContent = Utils.readFile(filePath);

        stdIOStream content = CodeCompiler.compile(inputContent);
        String outContent = content.toString();
        if (outContent.isEmpty()) {
            Utils.printError("Null content, output cancelled.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Compiled output at:\n> " + outPath);

        if (isToFormat) formatFile(filePath, filePath);
        if (isOpenOutput) Utils.openWithExplorer(outPath);
    }

    static void decompileFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath.substring(0, filePath.length() - 3);
        String inputContent = Utils.readFile(filePath);

        stdIOStream content = CodeDecompiler.decompile(inputContent);
        String outContent = content.toString();
        if (outContent.isEmpty()) {
            Utils.printError("Null content, output cancelled.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Reversed output at:\n> " + outPath);

        if (isToFormat) formatFile(outPath, outPath);
        if (isOpenOutput) Utils.openWithExplorer(outPath);
    }
}
