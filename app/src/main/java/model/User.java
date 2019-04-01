package model;

public class User {
    private boolean isRegistered;
    private String userId;
    private String deviceName;
    private String permPairCode;

    public User(boolean isRegistered, String userId, String deviceName, String permPairCode) {
        this.isRegistered = isRegistered;
        this.userId = userId;
        this.deviceName = deviceName;
        this.permPairCode = permPairCode;
    }

    public User() {

    }

    public boolean getRegistered() {
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPermPairCode() {
        return permPairCode;
    }

    public void setPermPairCode(String permPairCode) {
        this.permPairCode = permPairCode;
    }
}
