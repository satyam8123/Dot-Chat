package com.satya.dotchat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.satya.dotchat.adapters.AdapterChat;
import com.satya.dotchat.models.ModelChat;
import com.satya.dotchat.models.ModelUser;
import com.satya.dotchat.notifications.Data;
import com.satya.dotchat.notifications.Sender;
import com.satya.dotchat.notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int REQUEST_CALL=1;
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileTv,blockIv;
    TextView nameTv,userStatusTv;
    EditText messageEt;
    ImageButton sendBtn,attachBtn,mic;
    RelativeLayout chatbackground;
    String calledBy="";

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    StorageReference mStorageRef;
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;
    ProgressDialog pd;

    private MediaRecorder mRecorder;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;
    boolean isBlocked=false;

    private RequestQueue requestQueue;

    private boolean notify=false;
    private static final int CAMERA_REQUEST_CODE=100;
    private static final int STORAGE_REQUEST_CODE=200;

    private static final int IMAGE_PICK_CAMERA_CODE=300;
    private static final int IMAGE_PICK_GALLERY_CODE=400;
    private static final int PDF_PICK_CODE=438;
    private static final int MSWORD_PICK_CODE=500;

    String[] cameraPermissions;
    String[] storagePermissions;
    Uri image_uri = null;
    Uri pdf_uri = null;
    Uri ms_uri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView=findViewById(R.id.chat_recyclerView);
        profileTv=findViewById(R.id.profileTv);
        blockIv=findViewById(R.id.blockIv);

        nameTv=findViewById(R.id.nameTv);
        userStatusTv=findViewById(R.id.userStatusTv);
        messageEt=findViewById(R.id.messageEt);
        sendBtn=findViewById(R.id.sendBtn);
        attachBtn=findViewById(R.id.attachBtn);
        mic=findViewById(R.id.mic);
        chatbackground=findViewById(R.id.chatbackground);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        firebaseDatabase=FirebaseDatabase.getInstance();

        requestQueue= Volley.newRequestQueue(getApplicationContext());

        cameraPermissions=new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        Intent intent=getIntent();
        hisUid=intent.getStringExtra("hisUid");

        firebaseAuth=FirebaseAuth.getInstance();

        firebaseDatabase=FirebaseDatabase.getInstance();
        usersDbRef=firebaseDatabase.getReference("Users");

        Query userQuery=usersDbRef.orderByChild("uid").equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    String name=""+ds.child("name").getValue();
                    hisImage=""+ds.child("image").getValue();
                    String phonenumber=""+ds.child("phone").getValue();
                    String typingStatus=""+ds.child("typingTo").getValue();

                    if (typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }
                    else {
                        String onlineStatus=""+ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")){
                            userStatusTv.setText(onlineStatus);
                        }
                        else {
                            Calendar cal=Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime= DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();
                            userStatusTv.setText("Last seen at:"+dateTime);
                        }
                    }
                    nameTv.setText(name);
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_person_white).into(profileTv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_person_white).into(profileTv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify=true;
                String message=messageEt.getText().toString().trim();

                if (TextUtils.isEmpty(message)){
                    Toast.makeText(ChatActivity.this, "Can't send the empty message :(", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendMessage(message);
                }
                messageEt.setText("");
            }
        });

        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options ={"Image","Pdf Files","MS Word Files"};

                android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select File");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which==0){
                            showImagePickDialog();
                        }
                        else if (which ==1){
                            Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf/*");
                            startActivityForResult(intent,PDF_PICK_CODE);
                        }
                        else if (which ==2){
                            Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword/*");
                            startActivityForResult(intent,MSWORD_PICK_CODE);
                        }
                    }
                });
                builder.create().show();
            }
        });

        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mic.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length()==0){
                    checkTypingStatus("noOne");
                    mic.setVisibility(View.GONE);
                }
                else {
                    checkTypingStatus(hisUid);
                    mic.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                mic.setVisibility(View.GONE);
            }
        });

        blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBlocked){
                    unBlockUser();
                }
                else {
                    blockUser();
                }
            }
        });
        checkIsBlocked();
        readMessages();
        seenMessage();
        checkForReceivingCall();
    }

    private void speak() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi! Speack Something");

        try {
            startActivityForResult(intent,REQUEST_CODE_SPEECH_INPUT);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void checkIsBlocked() {
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            if (ds.exists()){
                                blockIv.setImageResource(R.drawable.ic_blocked_red);
                                isBlocked=true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void blockUser() {
        HashMap<String,String> hashMap=new HashMap<>();
        hashMap.put("uid",hisUid);

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChatActivity.this, "Blocked Successfully...", Toast.LENGTH_SHORT).show();
                        blockIv.setImageResource(R.drawable.ic_blocked_red);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatActivity.this, "Failed \n"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(ChatActivity.this, "Unblocked Successfully  :)", Toast.LENGTH_SHORT).show();
                                                blockIv.setImageResource(R.drawable.ic_unblocked_green);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ChatActivity.this, "Failed \n"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showImagePickDialog() {
        String[] options={"Camera","Gallery"};
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which==0){
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();
                    }
                }
                if (which==1){
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else {
                        pickFromGallary();
                    }
                }
            }
        });
        builder.create().show();
    }

    private void pickFromGallary() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues cv=new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp descr");
        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result= ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result= ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1= ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void seenMessage() {
        userRefForSeen=FirebaseDatabase.getInstance().getReference("Chats");
        seenListener=userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelChat chat=ds.getValue(ModelChat.class);
                    assert chat != null;
                    if (myUid.equals(chat.getReceiver()) && hisUid.equals(chat.getSender())){
                        HashMap<String,Object> hasSeenHashMap=new HashMap<>();
                        hasSeenHashMap.put("isSeen","true");
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void readMessages() {
        chatList=new ArrayList<>();
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelChat chat=ds.getValue(ModelChat.class);
                    if (myUid.equals(chat.getReceiver()) && hisUid.equals(chat.getSender()) ||
                            hisUid.equals(chat.getReceiver()) && myUid.equals(chat.getSender())){
                        chatList.add(chat);
                    }
                    adapterChat=new AdapterChat(ChatActivity.this,chatList,hisImage);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(adapterChat);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(final String message) {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
        String timestamp=String.valueOf(System.currentTimeMillis());

        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",hisUid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("isSeen","false");
        hashMap.put("type","text");
        hashMap.put("name", "");
        databaseReference.child("Chats").push().setValue(hashMap);

        DatabaseReference database=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelUser user=snapshot.getValue(ModelUser.class);
                if (notify){
                    sendNotification(hisUid,user.getName(),message);
                }
                notify=false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DatabaseReference chatRef1=FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(myUid)
                .child(hisUid);

        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        DatabaseReference chatRef2=FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(hisUid)
                .child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendImageMessage(Uri image_uri) throws IOException {
        notify =true;
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Sending image...");
        progressDialog.show();
        String timeStamp=""+System.currentTimeMillis();
        String fileNameAndPath="ChatImage/"+"post_"+timeStamp;
        StorageReference ref=FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putFile(image_uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri=uriTask.getResult().toString();
                        if (uriTask.isSuccessful()){
                            DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
                            HashMap<String,Object> hashMap=new HashMap<>();
                            hashMap.put("sender",myUid);
                            hashMap.put("receiver",hisUid);
                            hashMap.put("message",downloadUri);
                            hashMap.put("timestamp",timeStamp);
                            hashMap.put("type","image");
                            hashMap.put("name", "");
                            hashMap.put("isSeen","false");
                            databaseReference.child("Chats").push().setValue(hashMap);
                            DatabaseReference database=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                            database.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ModelUser user=snapshot.getValue(ModelUser.class);
                                    if (notify){
                                        sendNotification(hisUid,user.getName(),"Sent you a photo...");
                                    }
                                    notify=false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                            DatabaseReference chatRef1=FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(myUid)
                                    .child(hisUid);

                            chatRef1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()){
                                        chatRef1.child("id").setValue(hisUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                            DatabaseReference chatRef2=FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(hisUid)
                                    .child(myUid);

                            chatRef2.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()){
                                        chatRef2.child("id").setValue(myUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                    }
                });


    }

    private void sendMsfFile(Uri image_uri) throws IOException{
        notify =true;
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Sending Doc...");
        progressDialog.show();
        String timeStamp=""+System.currentTimeMillis();
        String fileNameAndPath="DocumentFile/"+timeStamp+".Doc";
        StorageReference ref=FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putFile(image_uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.dismiss();
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUri=uriTask.getResult().toString();
                if (uriTask.isSuccessful()){
                    DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
                    HashMap<String,Object> hashMap=new HashMap<>();
                    hashMap.put("sender",myUid);
                    hashMap.put("receiver",hisUid);
                    hashMap.put("message",downloadUri);
                    hashMap.put("timestamp",timeStamp);
                    hashMap.put("type","doc");
                    hashMap.put("name", FilenameUtils.getName(image_uri.getPath()));
                    hashMap.put("isSeen","false");
                    databaseReference.child("Chats").push().setValue(hashMap);
                    DatabaseReference database=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                    database.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ModelUser user=snapshot.getValue(ModelUser.class);
                            if (notify){
                                sendNotification(hisUid,user.getName(),"Sent you a Doc...");
                            }
                            notify=false;
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    DatabaseReference chatRef1=FirebaseDatabase.getInstance().getReference("Chatlist")
                            .child(myUid)
                            .child(hisUid);

                    chatRef1.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()){
                                chatRef1.child("id").setValue(hisUid);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    DatabaseReference chatRef2=FirebaseDatabase.getInstance().getReference("Chatlist")
                            .child(hisUid)
                            .child(myUid);

                    chatRef2.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()){
                                chatRef2.child("id").setValue(myUid);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
            }
        });
    }

    private void sendPdfFile(Uri image_uri) throws IOException {
        notify = true;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending Pdf...");
        progressDialog.show();
        String timeStamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "PdfFile/" + timeStamp + ".pdf";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);

        ref.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
            progressDialog.dismiss();
            Task<Uri> uriTask =taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            String downloadUri = uriTask.getResult().toString();
            if (uriTask.isSuccessful()) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("sender", myUid);
                hashMap.put("receiver", hisUid);
                hashMap.put("message", downloadUri);
                hashMap.put("timestamp", timeStamp);
                hashMap.put("type", "pdf");
                hashMap.put("name", FilenameUtils.getName(image_uri.getPath()));
                hashMap.put("isSeen", "false");
                databaseReference.child("Chats").push().setValue(hashMap);
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ModelUser user = snapshot.getValue(ModelUser.class);
                        if (notify) {
                            sendNotification(hisUid, user.getName(), "Sent you a pdf...");
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


                DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(myUid)
                        .child(hisUid);

                chatRef1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef1.child("id").setValue(hisUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


                DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(hisUid)
                        .child(myUid);

                chatRef2.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            chatRef2.child("id").setValue(myUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(ChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void sendNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens=FirebaseDatabase.getInstance().getReference("Tokens");
        Query query=allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    Token token=ds.getValue(Token.class);
                    Data data=new Data(
                            ""+myUid,
                            ""+name+": "+message,
                            "New Message",
                            ""+hisUid,
                            "ChatNotification",
                            R.drawable.appiconblack);

                    assert token != null;
                    Sender body=new Sender(data,token.getToken());

                    try {
                        JSONObject senderJsonObj=new JSONObject(new Gson().toJson(body));
                        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.d("JSON_RESPONSE","onResponse: "+response.toString());
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE","onResponse: "+error.toString());

                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                HashMap<String,String> headers=new HashMap<String, String>();
                                headers.put("Content-Type","application/json");
                                headers.put("Authorization","key=AAAA4Uy-_N8:APA91bE211i0-U92UZDneD2fTRNVuPpTc3XyQJbLTNRVfhd0H-NgBuNM3v9-hNWsPKNXRC-jzroagKxFYrNs76Cr1q2elxlB-ydQRNYTrPw-SXnXDk4sf1m5sYP3BVVbMT5heGs-yMQN");
                                return headers;
                            }
                        };

                        requestQueue.add(jsonObjectRequest);
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if (user!=null){
            //profileTv.setText(user.getEmail());
            myUid=user.getUid();
        }
        else {
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("onlineStatus",status);
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing){
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("typingTo",typing);
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        checkForReceivingCall();
        super.onStart();
    }



    @Override
    protected void onPause() {
        super.onPause();
        String timestamp=String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length>0){
                    boolean cameraAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted=grantResults[1]==PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        pickFromCamera();
                    }
                    else {
                        Toast.makeText(this, "Camera & Storage both permissions are neccessary :(", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }

            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean storageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        pickFromGallary();
                    }
                    else {
                        Toast.makeText(this, "Storage permissions neccessary", Toast.LENGTH_SHORT).show();
                    }
                }
                else {

                }
            }
            break;
            case REQUEST_CALL:{
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users").child(hisUid).child("phone");
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String phonenumber=snapshot.getValue(String.class);
                            if (phonenumber.trim().length()<10){
                                Toast.makeText(ChatActivity.this, "number is not Available", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                makephonecall(phonenumber);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_chat);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_chat);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (resultCode==RESULT_OK){
            if (requestCode==IMAGE_PICK_GALLERY_CODE){
                image_uri=data.getData();
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                image_uri=data.getData();
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode==REQUEST_CODE_SPEECH_INPUT){
                if (resultCode ==RESULT_OK && null!=data){
                    ArrayList<String> result=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    messageEt.setText(result.get(0));
                }
            }
            else if (requestCode==PDF_PICK_CODE){
                pdf_uri=data.getData();
                try {
                    sendPdfFile(pdf_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode==MSWORD_PICK_CODE){
                ms_uri=data.getData();
                try {
                    sendMsfFile(ms_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if (id==R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        else if (id==R.id.action_call){
            DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users").child(hisUid).child("phone");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phonenumber=snapshot.getValue(String.class);
                        if (phonenumber.trim().length()<10){
                            Toast.makeText(ChatActivity.this, "number is not Available", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            makephonecall(phonenumber);
                        }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        else if (id==R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }
        else if (id==R.id.action_video_call){
            Intent intent=new Intent(ChatActivity.this,CallingActivity.class);
            intent.putExtra("id",hisUid);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void makephonecall(String phonenumber) {
        if (ContextCompat.checkSelfPermission(ChatActivity.this,
                Manifest.permission.CALL_PHONE) !=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(ChatActivity.this,
                    new String[] {Manifest.permission.CALL_PHONE},REQUEST_CALL);
        }
        else {
            String dial="tel:"+ phonenumber;
            startActivity(new Intent(Intent.ACTION_CALL,Uri.parse(dial)));
        }

    }

    private void checkForReceivingCall() {
        FirebaseUser user=firebaseAuth.getCurrentUser();
        usersDbRef.child(user.getUid())
                .child("Ringing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("ringing")){
                            calledBy=snapshot.child("ringing").getValue().toString();
                            Intent callingIntent=new Intent(ChatActivity.this,CallingActivity.class);
                            callingIntent.putExtra("id",calledBy);
                            startActivity(callingIntent);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}