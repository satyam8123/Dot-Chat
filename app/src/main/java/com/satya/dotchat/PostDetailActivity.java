package com.satya.dotchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.adapters.AdapterComments;
import com.satya.dotchat.models.ModelComment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {

    String hisUid,myUid,myEmail,myName,myDp,postId,pLikes,hisDp,hisName,pImage;
    ProgressDialog pd;

    boolean mProcessComment=false;
    boolean mProcessLike=false;

    ImageView uPictureIv,pImageIv;
    TextView uNameTv,pTimeTiv,pTitleTv,pDescriptionTv,pLikesTv,pCommentsTv;
    ImageView moreBtn;
    Button likeBtn,shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    EditText commentEt;
    ImageButton sendBtn;
    DatabaseReference userRef;
    String calledBy="";
    ImageView cAvatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        Intent intent=getIntent();
        postId=intent.getStringExtra("postId");

        uPictureIv=findViewById(R.id.uPictureIv);
        pImageIv=findViewById(R.id.pImageIv);
        uNameTv=findViewById(R.id.uNameTv);
        pTimeTiv=findViewById(R.id.pTimeTv);
        pTitleTv=findViewById(R.id.pTitleTv);
        pDescriptionTv=findViewById(R.id.pDescriptionTv);
        pLikesTv=findViewById(R.id.pLikesTv);
        pCommentsTv=findViewById(R.id.pCommentsTv);
        moreBtn=findViewById(R.id.moreBtn);
        likeBtn=findViewById(R.id.likeBtn);
        shareBtn=findViewById(R.id.shareBtn);
        profileLayout=findViewById(R.id.profileLayout);
        recyclerView=findViewById(R.id.recyclerView);

        commentEt=findViewById(R.id.commentEt);
        sendBtn=findViewById(R.id.sendBtn);
        cAvatarIv=findViewById(R.id.cAvatarIv);

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();

        loadComments();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(PostDetailActivity.this, PostLikedByActivity.class);
                intent.putExtra("postId",postId);
                startActivity(intent);
            }
        });

        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pTitle=pTitleTv.getText().toString().trim();
                String pDescription=pDescriptionTv.getText().toString().trim();

                BitmapDrawable bitmapDrawable=(BitmapDrawable)pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    shareTextOnly(pTitle,pDescription);
                }
                else {
                    Bitmap bitmap=bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pDescription,bitmap);
                }
            }
        });
    }

    private void addToHisNotifications(String hisUid,String pId,String notification){
        String timestamp=""+System.currentTimeMillis();

        HashMap<Object,String> hashMap=new HashMap<>();
        hashMap.put("pId",pId);
        hashMap.put("timestamp",timestamp);
        hashMap.put("pUid",hisUid);
        hashMap.put("notification",notification);
        hashMap.put("sUid",myUid);

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }

    private void shareTextOnly(String pTitle, String pDescription) {
        String shareBody=pTitle +"\n"+pDescription;

        Intent sIntent=new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        startActivity(Intent.createChooser(sIntent,"Share Via"));
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        String shareBody=pTitle +"\n"+pDescription;

        Uri uri=saveImageToShare(bitmap);

        Intent sIntent=new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,"Share Via"));

    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder=new File(getCacheDir(),"images");
        Uri uri=null;
        try {
            imageFolder.mkdirs();
            File file=new File(imageFolder,"shared_image.png");
            FileOutputStream stream=new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri= FileProvider.getUriForFile(this,"com.satya.dotchat.fileprovider",file);


        }
        catch (Exception e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }



    private void loadComments() {
        LinearLayoutManager layoutManager=new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        commentList=new ArrayList<>();
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelComment modelComment=ds.getValue(ModelComment.class);
                    commentList.add(modelComment);
                    adapterComments=new AdapterComments(getApplicationContext(),commentList,myUid,postId);
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions() {
        PopupMenu popupMenu=new PopupMenu(this,moreBtn, Gravity.END);
        if (hisUid.equals(myUid)){
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit");
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int id=item.getItemId();
            if (id==0){
                beginDelete();
            }
            else if (id==1){
                Intent intent=new Intent(PostDetailActivity.this, AddPostActivity.class);
                intent.putExtra("key","editPost");
                intent.putExtra("editPostId",postId);
                startActivity(intent);
            }
            return false;
        });
        popupMenu.show();
    }

    private void beginDelete() {
        if (pImage.equals("noImage")){
            deleteWithoutImage();
        }
        else {
            deleteWithImage();
        }

    }

    private void deleteWithImage() {
        ProgressDialog pd=new ProgressDialog(this);
        pd.setMessage("Deleting...");

        StorageReference picRef= FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Query fquery= FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                    fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds:snapshot.getChildren()){
                                ds.getRef().removeValue();
                            }
                            Toast.makeText(PostDetailActivity.this, "Deleted Successfully :)", Toast.LENGTH_SHORT).show();
                            pd.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteWithoutImage() {
        ProgressDialog pd=new ProgressDialog(this);
        pd.setMessage("Deleting...");

        Query fquery= FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    ds.getRef().removeValue();
                }
                Toast.makeText(PostDetailActivity.this, "Deleted Successfully :)", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        DatabaseReference likesRef=FirebaseDatabase.getInstance().getReference().child("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)){
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like,0,0,0);
                    likeBtn.setText("Liked");
                }
                else {
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void likePost() {
        mProcessLike=true;
        DatabaseReference likesRef=FirebaseDatabase.getInstance().getReference().child("Likes");
        DatabaseReference postsRef=FirebaseDatabase.getInstance().getReference().child("Posts");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessLike){
                    if (snapshot.child(postId).hasChild(myUid)){
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike=false;
                    }
                    else {
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike=false;

                        addToHisNotifications(""+hisUid,""+postId,"Liked your post");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {
        pd=new ProgressDialog(this);
        pd.setMessage("Adding comment...");

        String comment=commentEt.getText().toString().trim();
        if (TextUtils.isEmpty(comment)){
            Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStammp=String.valueOf(System.currentTimeMillis());
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("cId",timeStammp);
        hashMap.put("comment",comment);
        hashMap.put("timestamp",timeStammp);
        hashMap.put("uid",myUid);
        hashMap.put("uEmail",myEmail);
        hashMap.put("uDp",myDp);
        hashMap.put("uName",myName);

        ref.child(timeStammp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, "Comment Added.. :)", Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();
                        addToHisNotifications(""+hisUid,""+postId,"Commented on your post");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void updateCommentCount() {
        mProcessComment=true;
        final DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessComment){
                    String comments=""+snapshot.child("pComments").getValue();
                    int newCommentVal=Integer.parseInt(comments)+1;
                    ref.child("pComments").setValue(""+newCommentVal);
                    mProcessComment=false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void loadUserInfo() {
        Query myRef=FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    myName=""+ds.child("name").getValue();
                    myDp=""+ds.child("image").getValue();
                    try {
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_face_black).into(cAvatarIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face_black).into(cAvatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPostInfo() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Posts");
        Query query=ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    String pTitle=""+ds.child("pTitle").getValue();
                    String pDescr=""+ds.child("pDescr").getValue();
                    pLikes=""+ds.child("pLikes").getValue();
                    String pTimeStamp=""+ds.child("pTime").getValue();
                    pImage=""+ds.child("pImage").getValue();
                    hisDp=""+ds.child("uDp").getValue();
                    hisUid=""+ds.child("uid").getValue();
                    String uEmail=""+ds.child("uEmail").getValue();
                    hisName=""+ds.child("uName").getValue();
                    String commentCount=""+ds.child("pComments").getValue();

                    Calendar calendar=Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime= DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes +" Loves");
                    pTimeTiv.setText(pTime);
                    pCommentsTv.setText(commentCount+" Comments");

                    uNameTv.setText(hisName);
                    if (pImage.equals("noImage")){
                        pImageIv.setVisibility(View.GONE);
                    }
                    else {
                        pImageIv.setVisibility(View.VISIBLE);
                        try{
                            Picasso.get().load(pImage).into(pImageIv);
                        }
                        catch (Exception e){

                        }
                    }
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_face_black).into(uPictureIv);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face_black).into(uPictureIv);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null){
            myEmail=user.getEmail();
            myUid=user.getUid();
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_post_detail);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_post_detail);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_call).setVisible(false);
        menu.findItem(R.id.action_create_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_video_call).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if (id==R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        else if (id==R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}