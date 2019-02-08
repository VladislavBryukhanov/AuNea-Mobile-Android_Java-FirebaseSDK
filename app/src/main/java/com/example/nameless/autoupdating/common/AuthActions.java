package com.example.nameless.autoupdating.common;

import com.example.nameless.autoupdating.models.User;

public interface AuthActions {
    void onAuthenticateAction(int requestCode, User user);
}
