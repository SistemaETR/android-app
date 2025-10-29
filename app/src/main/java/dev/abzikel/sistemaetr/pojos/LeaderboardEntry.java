package dev.abzikel.sistemaetr.pojos;

import java.util.Date;

public class LeaderboardEntry {
    private String leaderboardId;
    private String userId;
    private String username;
    private String photoUrl;
    private int modality;
    private int hits;
    private int misses;
    private int score;
    private long trainingTime;
    private double averageShotTime;
    private Date createdAt;

    // Empty constructor required for Firebase
    public LeaderboardEntry() {
    }

    public String getLeaderboardId() {
        return leaderboardId;
    }

    public void setLeaderboardId(String leaderboardId) {
        this.leaderboardId = leaderboardId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getModality() {
        return modality;
    }

    public void setModality(int modality) {
        this.modality = modality;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getTrainingTime() {
        return trainingTime;
    }

    public void setTrainingTime(long trainingTime) {
        this.trainingTime = trainingTime;
    }

    public double getAverageShotTime() {
        return averageShotTime;
    }

    public void setAverageShotTime(double averageShotTime) {
        this.averageShotTime = averageShotTime;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
