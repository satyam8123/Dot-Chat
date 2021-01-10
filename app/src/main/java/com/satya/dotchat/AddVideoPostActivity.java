package com.satya.dotchat;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.satya.dotchat.fragments.VideoFragment;
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

import java.util.HashMap;
import java.util.Objects;

public class AddVideoPostActivity extends AppCompatActivity {

    private static final int PICK_VIDEO = 1;
    MediaController mediaController;

    ActionBar actionBar;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;

    EditText titleEt,descriptionEt;
    VideoView videoVv;
    ImageView addvideo;
    Button uploadBtn;
    Uri video_uri = null;
    ProgressDialog pd;
    DatabaseReference userRef;
    String calledBy="";

    String name,email,uid,dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video_post);

        actionBar=getSupportActionBar();
        actionBar.setTitle("Add New Video Post");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        pd=new ProgressDialog(this);
        pd.setCanceledOnTouchOutside(false);


        firebaseAuth=FirebaseAuth.getInstance();
        checkUserStatus();

        userDbRef= FirebaseDatabase.getInstance().getReference("Users");
        Query query=userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    name=""+ds.child("name").getValue();
                    email=""+ds.child("email").getValue();
                    dp=""+ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        titleEt=findViewById(R.id.pTitleEt);
        descriptionEt=findViewById(R.id.pDescriptionEt);
        videoVv=findViewById(R.id.pVideoVv);
        uploadBtn=findViewById(R.id.pUploadBtn);
        addvideo=findViewById(R.id.addvideo);
        mediaController = new MediaController(this);
        videoVv.setMediaController(mediaController);
        videoVv.start();

        addvideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChooseVideo(v);
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title=titleEt.getText().toString().trim();
                String description=descriptionEt.getText().toString().trim();
                if (TextUtils.isEmpty(title)){
                    Toast.makeText(AddVideoPostActivity.this, "Enter Title..", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(description)){
                    Toast.makeText(AddVideoPostActivity.this, "Enter description..", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadVideoData(title,description,video_uri);
            }
        });
        checkForReceivingCall();
    }

    private String getExt(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return  mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadVideoData(String title, String description, Uri uri) {
        pd.setMessage("Publishing post...");
        pd.show();
        String timeStamp=String.valueOf(System.currentTimeMillis());
        String filePathAndName="VideoPosts/"+timeStamp;
            StorageReference ref= FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putFile(uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            String downloadUri= Objects.requireNonNull(uriTask.getResult()).toString();
                            if (uriTask.isSuccessful()){
                                HashMap<Object,String> hashMap=new HashMap<>();
                                hashMap.put("uid",uid);
                                hashMap.put("uName",name);
                                hashMap.put("uEmail",email);
                                hashMap.put("uDp",dp);
                                hashMap.put("pId",timeStamp);
                                hashMap.put("pTitle",title);
                                hashMap.put("pDescr",description);
                                hashMap.put("pVideo",downloadUri);
                                hashMap.put("pTime",timeStamp);
                                hashMap.put("pLikes","0");
                                hashMap.put("pComments","0");

                                DatabaseReference ref=FirebaseDatabase.getInstance().getReference("VideoPosts");
                                ref.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                pd.dismiss();
                                                startActivity(new Intent(AddVideoPostActivity.this, VideoFragment.class));
                                                finish();
                                                Toast.makeText(AddVideoPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                                                titleEt.setText("");
                                                descriptionEt.setText("");
                                                videoVv.setVideoURI(null);
                                                video_uri=null;

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        pd.dismiss();
                                        Toast.makeText(AddVideoPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(AddVideoPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO || resultCode == RESULT_OK ||
                data != null || data.getData() != null ){
            assert data != null;
            video_uri = data.getData();
            String[] mediaColumns = {MediaStore.Video.Media.SIZE};
            Cursor cursor = this.getContentResolver().query(video_uri, mediaColumns, null, null, null);
            cursor.moveToFirst();
            int sizeColInd = cursor.getColumnIndex(mediaColumns[0]);
            int fileSize = cursor.getInt(sizeColInd);
            int size= (int)(fileSize/1e+6);
            cursor.close();
            if (size > 100){
                String str="video size must be less then 100 MB";
                Intent intent=new Intent(this,AddVideoPostActivity.class);
                intent.putExtra("message_key", str);
                startActivity(intent);
                finish();
            }
            if (size>100){
                Intent intent =getIntent();
                String str = intent.getStringExtra("message_key");
                Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "Video size:"+size+"MB", Toast.LENGTH_LONG).show();
            videoVv.setVideoURI(video_uri);
            videoVv.setFocusable(true);
            videoVv.start();
        }

    }

    public void ChooseVideo(View view) {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_VIDEO);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkForReceivingCall();
        super.onStart();
    }

    @Override
    protected void onResume() {
        checkUserStatus();
        checkForReceivingCall();
        super.onResume();
    }

    private void checkUserStatus(){
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if (user!=null){
            //profileTv.setText(user.getEmail());
            email=user.getEmail();
            uid=user.getUid();
        }
        else {
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_call).setVisible(false);
        menu.findItem(R.id.action_video_call).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected(item);
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
                            Intent callingIntent=new Intent(AddVideoPostActivity.this,CallingActivity.class);
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