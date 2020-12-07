package io.netty.example.aaron.websocket;

public class DataContent {
    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getChatMsg() {
        return chatMsg;
    }

    public void setChatMsg(String chatMsg) {
        this.chatMsg = chatMsg;
    }

    public String getExtand() {
        return extand;
    }

    public void setExtand(String extand) {
        this.extand = extand;
    }

    private int action;
    private String chatMsg;
    private String extand;
}
