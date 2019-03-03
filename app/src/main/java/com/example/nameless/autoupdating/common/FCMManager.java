package com.example.nameless.autoupdating.common;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

public class FCMManager {

    private static String interlocutor = ""; //собеседник

    public static String getInterlocutor() {
        return interlocutor;
    }

    public static void setInterlocutor(String interlocutor) {
        FCMManager.interlocutor = interlocutor;
    }

    public static void subscribeToNotificationService() {
        FirebaseInstanceId.getInstance().getInstanceId()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult().getToken();
                    sendTokenToServer(token);
                }
            });
    }

    public static void sendTokenToServer(String token) {
        Query getUser = FirebaseDatabase
                .getInstance()
                .getReference("Users")
                .orderByChild("uid")
                .equalTo(FirebaseAuth.getInstance().getUid());


        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    getUser.getRef().child(data.getKey()).child("registrationTokenId").setValue(token);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
