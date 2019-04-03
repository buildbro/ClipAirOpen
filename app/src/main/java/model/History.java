package model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

public class History {

    private String id;
    private String mainText;
    private boolean shareStatus;
    private String pushKey;
    private final Object timePosted = ServerValue.TIMESTAMP;

    public History(String id, String mainText, boolean shareStatus) {
        this.id = id;
        this.mainText = mainText;
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

    public boolean isShareStatus() {
        return shareStatus;
    }

    public void setShareStatus(boolean shareStatus) {
        this.shareStatus = shareStatus;
    }

    public String getPushKey() {
        return pushKey;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }

    public Object getTimePosted() {
        return timePosted;
    }

    @Exclude
    public long getTimePostedLong() {
        return (long)timePosted;
    }
}
