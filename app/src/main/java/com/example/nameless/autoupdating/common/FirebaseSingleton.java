package com.example.nameless.autoupdating.common;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseSingleton {
    private static FirebaseDatabase firebase;

    public static FirebaseDatabase getFirebaseInstanse() {
        if (firebase == null) {
            firebase = firebase.getInstance();
//            firebase.setPersistenceEnabled(true);
        }
        return firebase;
    }
}
