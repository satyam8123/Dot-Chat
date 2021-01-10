package com.satya.dotchat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import com.satya.dotchat.adapters.AdapterGroupChat;
import com.satya.dotchat.models.ModelGroupChat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class GroupChatActivity extends AppCompatActivity {

    private String groupId,myGroupRole="";

    FirebaseAuth firebaseAuth;

    private static final int REQUEST_CODE_SPEECH_INPUT = 4000;
    Toolbar toolbar;
    ImageView groupIconIv;
    TextView groupTitleTv;
    ImageButton attachBtn,sendBtn,mic;
    DatabaseReference userRef;
    String calledBy="";
    EditText messageEt;
    RecyclerView chatRv;

    private ArrayList<ModelGroupChat> groupChatList;
    private AdapterGroupChat adapterGroupChat;

    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;

    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=2000;
    private static final int PDF_PICK_CODE=545;
    private static final int MSWORD_PICK_CODE=454;

    private String[] cameraPermission;
    private String[] storagePermission;

    private Uri image_uri=null;
    Uri pdf_uri = null;
    Uri ms_uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        toolbar=findViewById(R.id.toolbar);
        groupIconIv=findViewById(R.id.groupIconIv);
        groupTitleTv=findViewById(R.id.groupTitleTv);
        attachBtn=findViewById(R.id.attachBtn);
        messageEt=findViewById(R.id.messageEt);
        sendBtn=findViewById(R.id.sendBtn);
        chatRv=findViewById(R.id.chatRv);
        mic=findViewById(R.id.mic);

        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        chatRv.setHasFixedSize(true);
        chatRv.setLayoutManager(linearLayoutManager);

        setSupportActionBar(toolbar);

        Intent intent=getIntent();
        groupId=intent.getStringExtra("groupId");

        cameraPermission=new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        storagePermission=new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        firebaseAuth=FirebaseAuth.getInstance();
        loadGroupInfo();
        loadGroupMessages();
        loadMyGroupRole();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=messageEt.getText().toString().trim();
                if (TextUtils.isEmpty(message)){
                    Toast.makeText(GroupChatActivity.this, "Can't send empty message..", Toast.LENGTH_SHORT).show();
                }
                else {
                    sendMessage(message);
                }
            }
        });
        attachBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String[] options ={"Image","Pdf Files","MS Word Files"};

                android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(GroupChatActivity.this);
                builder.setTitle("Select File");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which==0){
                            showImageImportDialog();
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
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
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
                    mic.setVisibility(View.VISIBLE);
                }
                else {
                    mic.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                mic.setVisibility(View.GONE);
            }
        });
        checkForReceivingCall();
    }
    private void speak() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi! Speack Something");

        try {
            startActivityForResult(intent,REQUEST_CODE_SPEECH_INPUT);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void showImageImportDialog() {
        String[] options={"Camera","Gallery"};

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Pick Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which==0){
                            if (!checkStoragePermission()){
                                requestCameraPermission();
                            }else {
                                pickCamera();
                            }
                        }
                        else {
                            if (!checkStoragePermission()){
                                requestStoragePermission();
                            }
                            else {
                                pickGallery();
                            }

                        }
                    }
                })
                .show();
    }

    private void pickGallery(){
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera(){
        ContentValues contentValues=new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"GroupImageTitle");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"GroupImageDescription");

        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result= ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }
    private boolean checkCameraPermission(){
        boolean result=ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) ==(PackageManager.PERMISSION_GRANTED);
        boolean result1=ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void sendImageMessage() {
        ProgressDialog pd=new ProgressDialog(this);
        pd.setTitle("Please wait");
        pd.setMessage("Sending Image...");
        pd.setCanceledOnTouchOutside(false);
        pd.show();

        String filenamePath="ChatImages/"+""+System.currentTimeMillis();

        StorageReference storageReference=FirebaseStorage.getInstance().getReference(filenamePath);

        storageReference.putFile(image_uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> p_uriTask=taskSnapshot.getStorage().getDownloadUrl();
                        while (!p_uriTask.isSuccessful());
                        Uri p_downloadUri=p_uriTask.getResult();
                        if (p_uriTask.isSuccessful()){

                            String timestamp=""+System.currentTimeMillis();

                            HashMap<String,Object> hashMap=new HashMap<>();
                            hashMap.put("sender",""+firebaseAuth.getUid());
                            hashMap.put("message",""+p_downloadUri);
                            hashMap.put("timestamp",""+timestamp);
                            hashMap.put("type",""+"image");
                            hashMap.put("name","");

                            DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Groups");
                            ref.child(groupId).child("Messages").child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            messageEt.setText("");
                                            pd.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                });

    }

    private void sendMsfFile(Uri image_uri) throws IOException{
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Sending Doc...");
        progressDialog.show();
        String timeStamp=""+System.currentTimeMillis();
        String fileNameAndPath="DocumentFile/"+timeStamp;
        StorageReference ref=FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putFile(image_uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.dismiss();
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUri=uriTask.getResult().toString();
                if (uriTask.isSuccessful()) {
                    String timestamp = "" + System.currentTimeMillis();
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("sender",firebaseAuth.getUid());
                    hashMap.put("message",downloadUri);
                    hashMap.put("timestamp",timestamp);
                    hashMap.put("type","doc");
                    hashMap.put("name", FilenameUtils.getName(image_uri.getPath()));

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                    ref.child(groupId).child("Messages").child(timestamp)
                            .setValue(hashMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    messageEt.setText("");
                                    progressDialog.dismiss();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending Pdf...");
        progressDialog.show();
        String timeStamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "PdfFile/" + timeStamp + ".pdf";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);

        ref.putFile(image_uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressDialog.dismiss();
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                String downloadUri=uriTask.getResult().toString();
                if (uriTask.isSuccessful()) {
                    String timestamp = "" + System.currentTimeMillis();
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("sender",firebaseAuth.getUid());
                    hashMap.put("message",downloadUri);
                    hashMap.put("timestamp",timestamp);
                    hashMap.put("type", "pdf");
                    hashMap.put("name", FilenameUtils.getName(image_uri.getPath()));

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                    ref.child(groupId).child("Messages").child(timestamp)
                            .setValue(hashMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    messageEt.setText("");
                                    progressDialog.dismiss();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(GroupChatActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadMyGroupRole() {
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants")
                .orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            myGroupRole=""+ds.child("role").getValue();
                            invalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadGroupMessages() {

        groupChatList =new ArrayList<>();

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groupChatList.clear();
                        for (DataSnapshot ds:snapshot.getChildren()){
                            ModelGroupChat model=ds.getValue(ModelGroupChat.class);
                            groupChatList.add(model);
                        }
                        adapterGroupChat=new AdapterGroupChat(GroupChatActivity.this,groupChatList);
                        adapterGroupChat.notifyDataSetChanged();
                        chatRv.setAdapter(adapterGroupChat);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void sendMessage(String message) {
        String timestamp=""+System.currentTimeMillis();

        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("sender",""+firebaseAuth.getUid());
        hashMap.put("message",""+message);
        hashMap.put("timestamp",""+timestamp);
        hashMap.put("type",""+"text");

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        messageEt.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadGroupInfo() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Groups");
        ref.orderByChild("groupId").equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            String groupTitle=""+ds.child("groupTitle").getValue();
                            String groupDescription=""+ds.child("groupDescription").getValue();
                            String groupIcon=""+ds.child("groupIcon").getValue();
                            String timestamp=""+ds.child("timestamp").getValue();
                            String createdBy=""+ds.child("createdBy").getValue();

                            groupTitleTv.setText(groupTitle);
                            try {
                                Picasso.get().load(groupIcon).placeholder(R.drawable.ic_face_black).into(groupIconIv);
                            }
                            catch (Exception e){
                                groupIconIv.setImageResource(R.drawable.ic_users_black);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_group_chat);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_group_chat);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_call).setVisible(false);
        menu.findItem(R.id.action_video_call).setVisible(false);

        if (myGroupRole.equals("creator") || myGroupRole.equals("admin")){
            menu.findItem(R.id.action_add_participant).setVisible(true);
        }
        else {
            menu.findItem(R.id.action_add_participant).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if (id==R.id.action_add_participant){
            Intent intent=new Intent(this,GroupParticipantAddActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        else if (id==R.id.action_groupinfo){
            Intent intent=new Intent(this,GroupInfoActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            image_uri=data.getData();
            if (requestCode==IMAGE_PICK_GALLERY_CODE){
                assert data != null;
                sendImageMessage();
            }
            else if (requestCode==IMAGE_PICK_CAMERA_CODE){
                sendImageMessage();
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted=grantResults[1]==PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }
                    else {
                        Toast.makeText(this, "Camera & Storage permissions are required..", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean writeStorageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickGallery();
                    }
                    else {
                        Toast.makeText(this, "Storage permissions required..", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        checkForReceivingCall();
        super.onStart();
    }

    @Override
    protected void onResume() {
        checkForReceivingCall();
        super.onResume();
    }

    private void checkForReceivingCall() {
        FirebaseUser user=firebaseAuth.getCurrentUser();
        userRef= FirebaseDatabase.getInstance().getReference("Users");
        userRef.child(user.getUid())
                .child("Ringing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("ringing")){
                            calledBy=snapshot.child("ringing").getValue().toString();
                            Intent callingIntent=new Intent(GroupChatActivity.this,CallingActivity.class);
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