package com.example.nameless.autoupdating.models;

import java.util.Map;

public class Dialog {
    private String uid;
    private Message lastMessage;
    private User speaker;
    private Map<String, Long> interlocutors;
    private boolean notify;

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
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

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public Map<String, Long> getInterlocutors() {
        return this.interlocutors;
    }

    public void setInterlocutors(Map<String, Long> interlocutors) {
        this.interlocutors = interlocutors;
    }

    public Dialog() {}

    public Dialog(String uid, Map<String, Long> interlocutors, Message lastMessage, User speaker) {
        this.uid = uid;
        this.lastMessage = lastMessage;
        this.interlocutors = interlocutors;
        this.speaker = speaker;
    }

    public Dialog(Message lastMessage, Map<String, Long> interlocutors, boolean notify) {
        this.lastMessage = lastMessage;
        this.interlocutors = interlocutors;
        this.notify = notify;
    }
}
