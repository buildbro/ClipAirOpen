package model;

public class Pairing {
    private String pairCode;
    private String permPairCode;
    private String deviceName;

    public Pairing(String pairCode, String permPairCode, String deviceName) {
        this.pairCode = pairCode;
        this.permPairCode = permPairCode;
        this.deviceName = deviceName;
    }

    public Pairing() {
    }

    public String getPairCode() {
        return pairCode;
    }

    public void setPairCode(String pairCode) {
        this.pairCode = pairCode;
    }

    public String getPermPairCode() {
        return permPairCode;
    }

    public void setPermPairCode(String permPairCode) {
        this.permPairCode = permPairCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
