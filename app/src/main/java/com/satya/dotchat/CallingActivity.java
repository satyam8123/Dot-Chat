package com.satya.dotchat;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.satya.dotchat.fragments.ChatListFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class CallingActivity extends AppCompatActivity {

    TextView nameContact;
    ImageView profileImage,cancelCallBtn,acceptCallBtn;

    String receiverUserid="",receiverUserImage="",receiverUserName="";
    String senderUserid="",senderUserImage="",senderUserName="",checker="";
    String callingID="",ringingID="";
    DatabaseReference userRef;

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        senderUserid= FirebaseAuth.getInstance().getCurrentUser().getUid();

        receiverUserid=getIntent().getExtras().get("id").toString();
        userRef= FirebaseDatabase.getInstance().getReference().child("Users");

        mediaPlayer=MediaPlayer.create(this,R.raw.ringtone);

        nameContact=findViewById(R.id.name_calling);
        profileImage=findViewById(R.id.profile_image_calling);
        cancelCallBtn=findViewById(R.id.cancel_call);
        acceptCallBtn=findViewById(R.id.make_call);


        cancelCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                checker="clicked";
                cancelCallingUser();
            }
        });

        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mediaPlayer.stop();
                HashMap<String,Object> callingPickUpMap=new HashMap<>();
                callingPickUpMap.put("picked","picked");

                userRef.child(senderUserid).child("Ringing")
                        .updateChildren(callingPickUpMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    Intent intent =new Intent(CallingActivity.this,VideoChatActivity.class);
                                    startActivity(intent);
                                }
                            }
                        });
            }
        });

        getAndSetUserProfileInfo();
    }

    private void getAndSetUserProfileInfo() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(receiverUserid).exists()){
                    receiverUserImage=snapshot.child(receiverUserid).child("image").getValue().toString();
                    receiverUserName=snapshot.child(receiverUserid).child("name").getValue().toString();

                    nameContact.setText(receiverUserName);
                    Picasso.get().load(receiverUserImage).placeholder(R.drawable.ic_profile_black).into(profileImage);
                }
                if (snapshot.child(senderUserid).exists()){
                    senderUserImage=snapshot.child(senderUserid).child("image").getValue().toString();
                    senderUserName=snapshot.child(senderUserid).child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mediaPlayer.start();

        userRef.child(receiverUserid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!checker.equals("clicked") && !snapshot.hasChild("Calling") && !snapshot.hasChild("Ringing")){

                            HashMap<String,Object> callingInfo=new HashMap<>();
                            callingInfo.put("calling",receiverUserid);

                            userRef.child(senderUserid)
                                    .child("Calling")
                                    .updateChildren(callingInfo)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                HashMap<String,Object> ringingInfo=new HashMap<>();
                                                ringingInfo.put("ringing",senderUserid);

                                                userRef.child(receiverUserid)
                                                        .child("Ringing")
                                                        .updateChildren(ringingInfo);
                                            }
                                        }
                                    });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(senderUserid).hasChild("Ringing") && !snapshot.child(senderUserid).hasChild("Calling")){

                    acceptCallBtn.setVisibility(View.VISIBLE);

                }

                if (snapshot.child(receiverUserid).child("Ringing").hasChild("picked")){

                    mediaPlayer.stop();

                    Intent intent=new Intent(CallingActivity.this,VideoChatActivity.class);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void cancelCallingUser() {
        userRef.child(senderUserid)
                .child("Calling")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("calling")){
                            callingID=snapshot.child("calling").getValue().toString();

                            userRef.child(callingID)
                                    .child("Ringing")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                userRef.child(senderUserid)
                                                        .child("Calling")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                startActivity(new Intent(CallingActivity.this, ChatListFragment.class));
                                                                finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });

                        }

                        else {
                            startActivity(new Intent(CallingActivity.this, ChatListFragment.class));
                            finish();
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        userRef.child(senderUserid)
                .child("Ringing")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("ringing")){
                            ringingID=snapshot.child("ringing").getValue().toString();

                            userRef.child(ringingID)
                                    .child("Calling")
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                userRef.child(senderUserid)
                                                        .child("Ringing")
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                startActivity(new Intent(CallingActivity.this, ChatListFragment.class));
                                                                finish();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                        else {
                            startActivity(new Intent(CallingActivity.this, ChatListFragment.class));
                            finish();
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}