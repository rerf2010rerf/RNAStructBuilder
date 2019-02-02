package ru.icmmg.rnastruct.builder;


import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Annealing {
    private static final String PAT_FILE = "$$rnastructbuildertemp.pat~1";

    private static final Pattern ES_PAT = Pattern.compile("ES:(\\-?\\d+)");
    private static final Pattern X_PAT = Pattern.compile("X:(\\d+)");
    private static final Pattern LEN_PAT = Pattern.compile("L:(\\d+)");
    private static final Pattern SEQ_NAME_PAT = Pattern.compile("NM:\\[(.+)\\]");

    private final AnnealingParameters params;
    private final Map<String, Integer> seqLengths;
    private final long requiredSeqNumber;

    private PrintWriter logWriter;
    private TreeItem currentPat;
    private double currentValue;

    public Annealing(
            AnnealingParameters params,
            TreeItem initialPattern
    ) throws IOException {
        this(params, initialPattern, null);
    }

    public Annealing(
            AnnealingParameters params,
            TreeItem initialPattern,
            PrintWriter logWriter
    ) throws IOException {
        this.params = params;
        seqLengths = readLengths(params.getSeqFile());
        requiredSeqNumber = params.getSeqNumber() != null ? params.getSeqNumber() : seqLengths.size() - 1;
        this.logWriter = logWriter;
        currentPat = initialPattern;
        try {
            currentValue = function(currentPat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TreeItem start() throws IOException {

        double temp = 1;
        int changeCounter = 0;
        int iterationCounter = 0;
        while (changeCounter < params.getNumberIterationToStop()) {
            iterationCounter++;
            logStringLn("Current iteration: " + String.valueOf(iterationCounter));
            TreeItem probNext;
            if (currentValue < Double.MAX_VALUE) {
                probNext = PatternModifier.modify_(currentPat, true, false);
            } else {
                probNext = PatternModifier.modify(currentPat, true, false);
            }
            double probNextFunc = function(probNext);

            logStringLn("next pattern: \n" + PatternUtils.writeTree(probNext));
            logStringLn("prob next func value = " + probNextFunc);
            logStringLn("current func value = " + currentValue);

            double p = probability(probNextFunc, currentValue, temp);

            changeCounter++;
            if (p == 1) {
                changeCounter = 0;
                currentValue = probNextFunc;
                currentPat = probNext;
            } else {
                double r = RandomSingleton.getInstance().nextDouble();
                if (r < p) {
                    changeCounter = 0;
                    currentValue = probNextFunc;
                    currentPat = probNext;
                }
            }
            if (probNextFunc != Double.MAX_VALUE) {
                temp = tempModify(temp);
            }
        }
        logStringLn(PatternUtils.writeTree(currentPat));
        return currentPat;
    }

    private double probability(double fProbNext, double fThis, double temperature) {
        if (fProbNext < fThis) {
            return 1;
        } else {
            return Math.exp(-(fProbNext - fThis) / (params.getTempKoeff() * temperature));
        }
    }


    private double tempModify(double temp) {
        return currentValue == Double.MAX_VALUE ? temp : temp * 0.99;
    }

    double function(TreeItem pattern) throws IOException {
        try (FileWriter fw = new FileWriter(PAT_FILE, false)) {
            fw.append(PatternUtils.writeTree(pattern));
        }
        ProcessBuilder procBuilder = new ProcessBuilder(
                params.getRsDir() + "\\rscan",
                params.getSeqFile(),
                PAT_FILE,
                "-o:" + params.getRsConfigFile(),
                "-out_best:1"
        ).directory(new File("."));
        Process proc = procBuilder.start();

        new Thread(() -> {
            InputStream is = proc.getErrorStream();
            Scanner s = new Scanner(is);
            if (s.hasNextLine()) {
                logStringLn(s.nextLine());
            }
        }).start();

        InputStream rsIs = proc.getInputStream();
        Scanner scanner = new Scanner(rsIs);
        int countFound = 0;
        int countInBegin = 0;
        List<Integer> ESs = new ArrayList<>();
        logStringLn("");
        long timeStart = System.nanoTime();

        Map<String, Integer> foundMap = new HashMap<>();
        while (scanner.hasNextLine()) {
            String str = scanner.nextLine();


            Matcher esM = ES_PAT.matcher(str);
            if (esM.find()) {
                logString("*");
                countFound++;
            }

            Matcher nameM = SEQ_NAME_PAT.matcher(str);
            Matcher xM = X_PAT.matcher(str);
            Matcher lenM = LEN_PAT.matcher(str);
            nameM.find();
            if (xM.find() && xM.group(1).equals("0")
                    && lenM.find()
                    && (params.isEnableOpenedEnding() || Integer.valueOf(lenM.group(1)).equals(seqLengths.get(nameM.group(1))))) {
                logString("+");
                ESs.add(Integer.valueOf(esM.group(1)));
                countInBegin++;

                String name = nameM.group(1);
                if (foundMap.containsKey(name)) {
                    foundMap.put(name, foundMap.get(name) + 1);
                } else {
                    foundMap.put(name, 0);
                }
            }


        }

        int esEstimation = 0;
        if (!ESs.isEmpty()) {
            for (int es : ESs) {
                esEstimation += es;
            }
            esEstimation /= countInBegin;
        }

        long timeStop = System.nanoTime();
        long time = (timeStop - timeStart) / 1000000;

        logStringLn("\ncountFound: " + countFound
                + " countInBegin: " + countInBegin + " time: " + time
                + " esEstimation: " + (countInBegin > 0 ? esEstimation : "inf")
                + " unique countInBegin: " + foundMap.entrySet().size());


        if (countInBegin < requiredSeqNumber) {
            return Double.MAX_VALUE;
        }

        if (foundMap.entrySet().size() < requiredSeqNumber) {
            return Double.MAX_VALUE;
        }

        if (time > params.getRsTimeLimit()) {
            return Double.MAX_VALUE;
        }


        double ret = -params.getK1() * esEstimation
                + params.getK2() * time
                - params.getK3() * foundMap.entrySet().size();

        return ret;
    }

    private static Map<String, Integer> readLengths(String fileName) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        String name = "kfljd;sajfkl;dsa";
        StringBuilder seq = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(fileName); Scanner scan = new Scanner(fis)) {
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if (line.startsWith(">")) {
                    result.put(name.substring(1), seq.length());
                    name = line;
                    seq = new StringBuilder();
                } else {
                    seq.append(line);
                }
            }
            result.put(name.substring(1), seq.length());
        }
        result.remove("kfljd;sajfkl;dsa");
        return result;
    }

    private void logString(String string) {
        System.out.print(string);
        if (logWriter != null) {
            logWriter.print(string);
        }
    }

    private void logStringLn(String string) {
        System.out.println(string);
        if (logWriter != null) {
            logWriter.println(string);
        }
    }

}
