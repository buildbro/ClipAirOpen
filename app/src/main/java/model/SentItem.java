package model;

public class SentItem {
    private String text;
    private int status;

    public SentItem(String text, int status) {
        this.text = text;
        this.status = status;
    }

    public SentItem() {

    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
