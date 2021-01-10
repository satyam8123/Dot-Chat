package com.satya.dotchat;

import android.app.Application;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        DatabaseReference scoresRef = FirebaseDatabase.getInstance().getReference();
        scoresRef.keepSynced(true);

    }
}
