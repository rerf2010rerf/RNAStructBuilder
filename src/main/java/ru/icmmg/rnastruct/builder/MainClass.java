package ru.icmmg.rnastruct.builder;

import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {

    private static final String DEFAULT_PATTERN = "RNA_TREE_BEGIN\\n\" +\n" +
            "            \"F\\n\" +\n" +
            "            \" E              len:0..30\\n\" +\n" +
            "            \" S[m]              len:3..30     msl:30\\n\" +\n" +
            "            \"  L              len:0..30\\n\" +\n" +
            "            \"  S              len:3..30       msl:30\\n\" +\n" +
            "            \"   L              len:0..30   \\n\" +\n" +
            "            \"   S              len:3..30     msl:30\\n\" +\n" +
            "            \"    L              len:3..30\\n\" +\n" +
            "            \"   L              len:0..30\\n\" +\n" +
            "            \"  L              len:0..30\\n\" +\n" +
            "            \" E              len:0..30\\n\" +\n" +
            "            \"RNA_TREE_END\\n\" +\n" +
            "            \"PSEUDOKNOTS_BEGIN\\n\" +\n" +
            "            \"PSEUDOKNOTS_END";

    private static final Pattern RS_DIR = Pattern.compile("-rscanDir:([^\\s]?)");
    private static final Pattern RS_CONFIG_FILE = Pattern.compile("-rscanConfigFile:([^\\s]?)");
    private static final Pattern SEQ_FILE = Pattern.compile("-seqFile:([^\\s]?)");
    private static final Pattern SEQ_NUMBER = Pattern.compile("-minSeqs:([^\\s]?)");
    private static final Pattern ITERATION_TO_STOP = Pattern.compile("-iterationToStop:([^\\s]?)");
    private static final Pattern TEMP_KOEFF = Pattern.compile("-temperatureKoeff:([^\\s]?)");
    private static final Pattern RS_TIME_LIMIT = Pattern.compile("-rscanTimeLimit:([^\\s]?)");
    private static final Pattern ENERGY_KOEFF = Pattern.compile("-energyKoeff:([^\\s]?)");
    private static final Pattern TIME_KOEFF = Pattern.compile("-timeKoeff:([^\\s]?)");
    private static final Pattern NUMBER_KOEFF = Pattern.compile("-numberKoeff:([^\\s]?)");
    private static final Pattern LOG_FILE_NAME = Pattern.compile("-logFile:([^\\s]?)");
    private static final Pattern INITIAL_PATTERN_FILE = Pattern.compile("-initialPatternFile:([^\\s]?)");
    private static final Pattern RESULT_FILE = Pattern.compile("-resultFile:([^\\s]?)");

    public static void main(String[] args) throws Exception {
        AnnealingParameters params = parseCommandLine(args);

        String initialPatternFile = findStringValue(INITIAL_PATTERN_FILE, args);
        String initialPattern = initialPatternFile != null
                ? readPatternFile(initialPatternFile)
                : DEFAULT_PATTERN;
        TreeItem F = PatternUtils.readTree(initialPattern, new HashSet<>());

        String logFileName = findStringValue(LOG_FILE_NAME, args);

        if (logFileName != null) {
            try (FileOutputStream fos = new FileOutputStream(logFileName);
                 PrintWriter logWriter = new PrintWriter(fos, true)) {
                Annealing an = new Annealing(params, F, logWriter);
                F = an.start();
            }
        } else {
            Annealing an = new Annealing(params, F);
            F = an.start();
        }

        String result = PatternUtils.writeTree(F);
        System.out.println("Resulting pattern:\n" + result);
        String resultFileName = findStringValue(RESULT_FILE, args);
        if (null != resultFileName) {
            try (PrintWriter pw = new PrintWriter(resultFileName)){
                pw.println(result);
            }
        }
    }

    private static AnnealingParameters parseCommandLine(String[] args) {
        AnnealingParameters params = new AnnealingParameters();

        String rsDir = findStringValue(RS_DIR, args);
        params.setRsDir(rsDir != null ? rsDir : ".");

        String rsConfigFile = findStringValue(RS_CONFIG_FILE, args);
        params.setRsConfigFile(rsConfigFile != null ? rsConfigFile : "rscan.cfg");

        String seqFile = findStringValue(SEQ_FILE, args);
        if (seqFile == null) {
            System.out.println("Parameter -seqFile is not found");
            System.exit(0);
        }
        params.setSeqFile(seqFile);

        Long seqNumber = findNumericValue("-minSeqs", SEQ_NUMBER, args);
        params.setSeqNumber(seqNumber);

        Long iterationToStop = findNumericValue("-iterationToStop", ITERATION_TO_STOP, args);
        params.setNumberIterationToStop(iterationToStop != null ? iterationToStop : 100);

        Long temperatureKoeff = findNumericValue("-temperatureKoeff", TEMP_KOEFF, args);
        params.setTempKoeff(temperatureKoeff != null ? temperatureKoeff : 3000);

        Long rscanTimeLimit = findNumericValue("-rscanTimeLimit", RS_TIME_LIMIT, args);
        params.setRsTimeLimit(rscanTimeLimit != null ? rscanTimeLimit : 3000);

        Long energyKoeff = findNumericValue("-energyKoeff", ENERGY_KOEFF, args);
        params.setK1(energyKoeff != null ? energyKoeff : 10);

        Long timeKoeff = findNumericValue("-timeKoeff", TIME_KOEFF, args);
        params.setK2(timeKoeff != null ? timeKoeff : 1);

        Long numberKoeff = findNumericValue("-numberKoeff", NUMBER_KOEFF, args);
        params.setK3(numberKoeff != null ? numberKoeff : 10);

        return params;
    }

    private static Long findNumericValue(String paramName, Pattern pattern, String[] args) {
        String value = findStringValue(pattern, args);
        if (null == value) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            System.out.println(String.format("Incorrect value of %s parameter", paramName));
            System.exit(0);
        }
        return null;
    }

    private static String findStringValue(Pattern pattern, String[] args) {
        for (String arg : args) {
            Matcher m;
            if ((m = pattern.matcher(arg)).matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    private static String readPatternFile(String fileName) throws FileNotFoundException {
        StringBuilder builder = new StringBuilder();
        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine()).append("\n");
            }
        }
        return builder.toString();
    }

}
