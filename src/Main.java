public class Main {
    public static final boolean openAfterCompile = false;
    public static final boolean isDebug = true;
    public static final boolean generatePrimeCode = false;
    public static final boolean formatOnExecute = true;
    static final String versionTag = "1.0";
    public static String fileDefault = "sample_cases/testcase.mdtc";

    static void main(String[] args) {
        if (args.length > 0 && args[0].endsWith(".mdtc"))
            fileDefault = args[0];
        Utils.convertFile();
    }
}
