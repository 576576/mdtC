public class Main {
    public static final boolean openAfterCompile = false;
    public static final boolean isDebug = true;
    static final String versionTag = "0.8";
    public static String fileDefault = "sample_cases/testcase.mdtc";

    static void main(String[] args) {
        if (args.length > 0 && args[0].endsWith(".mdtc"))
            fileDefault = args[0];
        Utils.convertFile();
    }
}
