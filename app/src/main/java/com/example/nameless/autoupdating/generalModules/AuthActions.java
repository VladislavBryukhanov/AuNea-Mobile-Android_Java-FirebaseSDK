package com.example.nameless.autoupdating.generalModules;

import com.example.nameless.autoupdating.models.User;

public interface AuthActions {
    void onAuthenticateAction(int requestCode, User user);
}
