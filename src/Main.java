import picocli.CommandLine;

public class Main {
    private static final String VERSION_TAG = "1.31";
    static boolean isToFormat;
    static boolean isFormatOnly;
    static boolean isOpenOutput;
    static String filePath = "";
    static String outPath = "";
    static int primeCodeLevel = 0;

    static void main(String[] args) {
        CliHelper cliHelper = new CliHelper();
        CommandLine cmd = new CommandLine(cliHelper);
        cmd.parseArgs(args);

        isToFormat = cliHelper.isToFormat;
        isFormatOnly = cliHelper.isFormatOnly;
        filePath = cliHelper.filePath;
        outPath = cliHelper.outPath;
        isOpenOutput = cliHelper.isOpenOutput;
        primeCodeLevel = cliHelper.primeCodeLevel;

        if (cliHelper.versionInfo) IO.println("MdtC Compiler v" + Main.VERSION_TAG);

        if (filePath.isEmpty())
            filePath = IO.readln("Enter input file path below.\n> ");
        if (filePath.isEmpty()) {
            Utils.printError("No input file is detected.");
            return;
        }
        if (!Constants.supportFormats.contains(filePath.substring(filePath.lastIndexOf(".")))) {
            Utils.printError("Unsupported formats(.mdtc, .mdtcode, .libmdtc).\n> " + filePath);
            return;
        }

        if (filePath.endsWith(".libmdtc") || isFormatOnly)
            formatFile(filePath, outPath);
        else if (filePath.endsWith(".mdtc"))
            compileFile(filePath, outPath);
        else if (filePath.endsWith(".mdtcode"))
            decompileFile(filePath, outPath);
    }

    static void formatFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath;
        String inputContent = Utils.readFile(filePath);
        String outContent = CodeFormatter.format(inputContent);

        if (outContent.isEmpty() || outContent.equals(inputContent)) {
            IO.println("Nothing to format with, skipped.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Formatted output at:\n> " + outPath);
    }

    static void compileFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath.replace(".mdtc", ".mdtcode");
        String inputContent = Utils.readFile(filePath);
        String outputRead = Utils.readFile(outPath);
        String outContent = CodeCompiler.compile(inputContent);

        if (outContent.equals(outputRead)) {
            IO.println("Identical output, skipped.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Compiled output at:\n> " + outPath);

        if (isToFormat) formatFile(filePath, filePath);
        if (isOpenOutput) Utils.openWithExplorer(outPath);
    }

    static void decompileFile(String filePath, String outPath) {
        if (outPath.isEmpty()) outPath = filePath.replace(".mdtcode", ".mdtc");
        String inputContent = Utils.readFile(filePath);
        String outputRead = Utils.readFile(outPath);
        String outContent = CodeDecompiler.decompile(inputContent);

        if (outContent.equals(outputRead)) {
            IO.println("Identical output, skipped.");
            return;
        }

        Utils.writeFile(outPath, outContent);
        IO.println("Decompiled output at:\n> " + outPath);

        if (isOpenOutput) Utils.openWithExplorer(outPath);
    }
}
