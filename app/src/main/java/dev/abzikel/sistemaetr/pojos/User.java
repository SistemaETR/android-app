package dev.abzikel.sistemaetr.pojos;

import java.util.Date;

public class User {
    private String userId;
    private String email;
    private String photoUrl;
    private String username;
    private int totalTrainings;
    private double averageShotTime;
    private double accuracy;
    private Date createdAt;

    // Empty constructor required for Firebase
    public User() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalTrainings() {
        return totalTrainings;
    }

    public void setTotalTrainings(int totalTrainings) {
        this.totalTrainings = totalTrainings;
    }

    public double getAverageShotTime() {
        return averageShotTime;
    }

    public void setAverageShotTime(double averageShotTime) {
        this.averageShotTime = averageShotTime;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
