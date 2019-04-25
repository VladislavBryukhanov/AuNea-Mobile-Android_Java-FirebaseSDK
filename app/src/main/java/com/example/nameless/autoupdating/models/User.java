package com.example.nameless.autoupdating.models;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.Serializable;

/**
 * Created by nameless on 07.04.18.
 */

public class User implements Serializable{

    private String login;
    private String uid;
    private String email;
    private String nickname;
    private String bio;
    private String avatarUrl;
//    private Bitmap avatar;
    private String status;
    private String avatar;
    private boolean banned;


    public User(){}

    public User(String uid, String login) {
        this.uid = uid;
        this.login = login;
    }

//    public Bitmap getAvatar() {
//        return avatar;
//    }

//    public void setAvatar(Bitmap avatar) {
//        this.avatar = avatar;
//    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}
