package dev.abzikel.sistemaetr.pojos;

public class User {
    private String userId;
    private String email;
    private String username;
    private double averageShotTime;
    private double accuracy;

    // Empty constructor required for Firebase
    public User() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

}
