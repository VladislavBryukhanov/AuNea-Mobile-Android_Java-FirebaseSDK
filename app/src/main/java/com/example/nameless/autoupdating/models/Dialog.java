package com.example.nameless.autoupdating.models;

public class Dialog {
    public String uid;
    public Message lastMessage;
    public int unreadCounter;
    public User speaker;

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCounter() {
        return unreadCounter;
    }

    public void setUnreadCounter(int unreadCounter) {
        this.unreadCounter = unreadCounter;
    }

    public User getSpeaker() {
        return speaker;
    }

    public void setSpeaker(User speaker) {
        this.speaker = speaker;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Dialog(String uid, Message lastMessage, int unreadCounter, User speaker) {
        this.uid = uid;
        this.lastMessage = lastMessage;
        this.unreadCounter = unreadCounter;
        this.speaker = speaker;
    }
}
