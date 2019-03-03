package model;

public class History {

    private String id;
    private String mainText;
    private String timeAdded;
    private boolean shareStatus;

    public History(String id, String mainText, String timeAdded, boolean shareStatus) {
        this.id = id;
        this.mainText = mainText;
        this.timeAdded = timeAdded;
        this.shareStatus = shareStatus;
    }

    public History() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMainText() {
        return mainText;
    }

    public void setMainText(String mainText) {
        this.mainText = mainText;
    }

    public String getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(String timeAdded) {
        this.timeAdded = timeAdded;
    }

    public boolean isShareStatus() {
        return shareStatus;
    }

    public void setShareStatus(boolean shareStatus) {
        this.shareStatus = shareStatus;
    }
}
