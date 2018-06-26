package com.example.nameless.autoupdating;

import java.util.Date;

/**
 * Created by nameless on 07.04.18.
 */

public class Message {

    private String uid;
    private String content;
    private String fileUrl;
    private String fileType;
    private String fileMediaSides;
    private Date dateOfSend;
    private String who;
    private String to;

    public Message(){}

    public Message(String content, String fileUrl, Date dateOfSend, String who, String to, String fileType, String fileMediaSides) {
        this.uid = uid;
        this.content = content;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileMediaSides = fileMediaSides;
        this.dateOfSend = dateOfSend;
        this.who = who;
        this.to = to;
    }

    public Message(String uid, Message msg) {
        this.uid = uid;
        this.content = msg.content;
        this.fileUrl = msg.fileUrl;
        this.dateOfSend = msg.dateOfSend;
        this.who = msg.who;
        this.to = msg.to;
        this.fileType = msg.fileType;
        this.fileMediaSides = msg.fileMediaSides;
    }

    public String getFileMediaSides() {
        return fileMediaSides;
    }

    public void setFileMediaSides(String fileMediaSides) {
        this.fileMediaSides = fileMediaSides;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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
