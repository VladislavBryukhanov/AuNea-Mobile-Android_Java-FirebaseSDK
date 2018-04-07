package com.example.nameless.autoupdating;

import java.io.Serializable;

/**
 * Created by nameless on 07.04.18.
 */

public class User implements Serializable{

    private String login;
    private String password;
    //avatar, bio ...

    public User(){}

    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
