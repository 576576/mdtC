public class Main {
    public static final boolean openAfterCompile = false;
    public static final boolean isDebug = true;
    public static String fileDefault = "sample_cases/case5.mdtc";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].endsWith(".mdtc"))
            fileDefault = args[0];
        //Utils.clearConsole();
        Utils.fileIO();
    }
}
