package model;

public class Device {
    private String userId;
    private String permPairCode;
    private String pairCode;
    private String deviceName;
    private String deviceName2;
    private boolean pairStatus;
    private String pushKey;

    public Device(String userId, String permPairCode, String pairCode, String deviceName, String deviceName2, boolean pairStatus) {
        this.userId = userId;
        this.permPairCode = permPairCode;
        this.pairCode = pairCode;
        this.deviceName = deviceName;
        this.deviceName2 = deviceName2;
        this.pairStatus = pairStatus;
    }

    public Device() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPermPairCode() {
        return permPairCode;
    }

    public void setPermPairCode(String permPairCode) {
        this.permPairCode = permPairCode;
    }

    public String getPairCode() {
        return pairCode;
    }

    public void setPairCode(String pairCode) {
        this.pairCode = pairCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName2() {
        return deviceName2;
    }

    public void setDeviceName2(String deviceName2) {
        this.deviceName2 = deviceName2;
    }

    public boolean getPairStatus() {
        return pairStatus;
    }

    public void setPairStatus(boolean pairStatus) {
        this.pairStatus = pairStatus;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }

    public String getPushKey() {
        return pushKey;
    }
}
