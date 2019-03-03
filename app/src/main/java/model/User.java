package model;

public class User {
    private boolean isRegistered;
    private String userId;

    public User(boolean isRegistered, String userId) {
        this.isRegistered = isRegistered;
        this.userId = userId;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
