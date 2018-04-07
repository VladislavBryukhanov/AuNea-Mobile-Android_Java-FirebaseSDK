package com.example.nameless.autoupdating;

import java.util.Date;

/**
 * Created by nameless on 07.04.18.
 */

public class Message {
    private String content;
    private Date dateOfSend;
    private String who;
    private String to;

    public Message(){}
    public Message(String content, Date dateOfSend, String who, String to) {
        this.content = content;
        this.dateOfSend = dateOfSend;
        this.who = who;
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDateOfSend() {
        return dateOfSend;
    }

    public void setDateOfSend(Date dateOfSend) {
        this.dateOfSend = dateOfSend;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
