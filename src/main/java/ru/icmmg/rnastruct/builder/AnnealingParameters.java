package ru.icmmg.rnastruct.builder;

public class AnnealingParameters {

    private String rsDir;
    private String rsConfigFile;
    private String seqFile;
    private Long seqNumber;
    private long numberIterationToStop;
    private long tempKoeff;
    private long rsTimeLimit;
    private boolean enableOpenedEnding;
    private long k1;
    private long k2;
    private long k3;

    public String getRsDir() {
        return rsDir;
    }

    public void setRsDir(String rsDir) {
        this.rsDir = rsDir;
    }

    public String getRsConfigFile() {
        return rsConfigFile;
    }

    public void setRsConfigFile(String rsConfigFile) {
        this.rsConfigFile = rsConfigFile;
    }

    public String getSeqFile() {
        return seqFile;
    }

    public void setSeqFile(String seqFile) {
        this.seqFile = seqFile;
    }

    public Long getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(Long seqNumber) {
        this.seqNumber = seqNumber;
    }

    public long getNumberIterationToStop() {
        return numberIterationToStop;
    }

    public void setNumberIterationToStop(long numberIterationToStop) {
        this.numberIterationToStop = numberIterationToStop;
    }

    public long getTempKoeff() {
        return tempKoeff;
    }

    public void setTempKoeff(long tempKoeff) {
        this.tempKoeff = tempKoeff;
    }

    public long getRsTimeLimit() {
        return rsTimeLimit;
    }

    public void setRsTimeLimit(long rsTimeLimit) {
        this.rsTimeLimit = rsTimeLimit;
    }

    public long getK1() {
        return k1;
    }

    public void setK1(long k1) {
        this.k1 = k1;
    }

    public long getK2() {
        return k2;
    }

    public void setK2(long k2) {
        this.k2 = k2;
    }

    public long getK3() {
        return k3;
    }

    public void setK3(long k3) {
        this.k3 = k3;
    }

    public boolean isEnableOpenedEnding() {
        return enableOpenedEnding;
    }

    public void setEnableOpenedEnding(boolean enableOpenedEnding) {
        this.enableOpenedEnding = enableOpenedEnding;
    }
}
