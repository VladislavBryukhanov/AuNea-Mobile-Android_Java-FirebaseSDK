package com.example.nameless.autoupdating.common;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NetworkUtil {

    public final static String ONLINE_STATUS = "Online";
    public final static String OFFLINE_STATUS  = "Last seen at";
    public final static String AFK_STATUS  = "AFK";

    private interface callbackAction {
        void doAction();
    }

    private static final NetworkUtil instance = new NetworkUtil();
    private static String currentStatus;

    private DatabaseReference userRef;
    private DateFormat dateFormat;

    public static void setNetworkStatus() {
        currentStatus = ONLINE_STATUS;
        instance.getInstance(instance::setProfileStatus);
    }

    public static void setAfkStatus() {
        currentStatus = MessageFormat.format(AFK_STATUS  + "({0})", instance.getLastSeen());
        instance.getInstance(() -> instance.userRef.setValue(currentStatus));
    }

    public static void setOnlineStatus() {
        currentStatus = ONLINE_STATUS;
        instance.getInstance(() -> instance.userRef.setValue(currentStatus));
    }

    public static void setOfflineStatus() {
        currentStatus = instance.getLastSeen();
        instance.getInstance(() -> instance.userRef.setValue(currentStatus));
    }

    private String getLastSeen() {
        return OFFLINE_STATUS  + dateFormat.format(new Date());
    }

    private void getInstance(callbackAction action) {
        if (userRef != null) {
            action.doAction();
            return;
        }

        dateFormat = (new SimpleDateFormat(" HH:mm dd MMM", Locale.ENGLISH));

        final Query getUser = FirebaseDatabase
                .getInstance()
                .getReference("Users")
                .orderByChild("uid")
                .equalTo(FirebaseAuth.getInstance().getUid());

        getUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    userRef = getUser.getRef().child(data.getKey()).child("status");
                    action.doAction();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    private void setProfileStatus() {
        final DatabaseReference connectedRef = FirebaseDatabase
                .getInstance()
                .getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {

                final String lastSeen = getLastSeen();

                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    userRef.onDisconnect().setValue(lastSeen);
                    userRef.setValue(currentStatus);
                } else { //Вроде как else никогда не отрабатывает
                    userRef.setValue(lastSeen);
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
}
