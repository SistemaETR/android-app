package dev.abzikel.sistemaetr.pojos;

public class Training {
    private String trainingId;
    private String modality;
    private int hits;
    private int misses;
    private int score;
    private double trainingTime;
    private double averageShotTime;

    // Empty constructor required for Firebase
    public Training() {
    }

    public String getTrainingId() {
        return trainingId;
    }

    public void setTrainingId(String trainingId) {
        this.trainingId = trainingId;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getTrainingTime() {
        return trainingTime;
    }

    public void setTrainingTime(double trainingTime) {
        this.trainingTime = trainingTime;
    }

    public double getAverageShotTime() {
        return averageShotTime;
    }

    public void setAverageShotTime(double averageShotTime) {
        this.averageShotTime = averageShotTime;
    }

}
