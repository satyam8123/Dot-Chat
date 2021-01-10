package com.satya.dotchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.ChatActivity;
import com.satya.dotchat.R;
import com.satya.dotchat.models.ModelVideoPost;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AdapterVideoPosts extends RecyclerView.Adapter<AdapterVideoPosts.ViewHolder>{

    Context context;
    List<ModelVideoPost> videopostList;
    String myUid;
    private DatabaseReference likesRef;
    private DatabaseReference postsRef;
    boolean mProcessLike=false;
    MediaController mediaController;



    public AdapterVideoPosts(Context context,List<ModelVideoPost> videopostList) {
        this.context = context;
        this.videopostList = videopostList;
        myUid= FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef= FirebaseDatabase.getInstance().getReference().child("VideoLikes");
        postsRef=FirebaseDatabase.getInstance().getReference().child("VideoPosts");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.row_videoposts,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String uid=videopostList.get(position).getUid();
        String uEmail=videopostList.get(position).getuEmail();
        String uName=videopostList.get(position).getuName();
        String uDp=videopostList.get(position).getuDp();
        String pId=videopostList.get(position).getpId();
        String pTitle=videopostList.get(position).getpTitle();
        String pDescription=videopostList.get(position).getpDescr();
        String pVideo=videopostList.get(position).getpVideo();
        String pTimeStamp=videopostList.get(position).getpTime();
        String pLikes=videopostList.get(position).getpLikes();
        String pComments=videopostList.get(position).getpComments();
        String hisUID=videopostList.get(position).getUid();


        Calendar calendar=Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime= DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        holder.pTitleTv.setText(pTitle);
        holder.pDescriptionTv.setText(pDescription);
        holder.pLikesTv.setText(pLikes+" Loves");
        holder.pCommentsTv.setText(pComments+" Comments");
        /*mediaController = new MediaController(context);
        holder.pVideoVv.setMediaController(mediaController);*/

        if (myUid.equals(hisUID)){
            holder.commentBtn.setVisibility(View.GONE);
            holder.pCommentsTv.setVisibility(View.GONE);
        }

        setLikes(holder,pId);

        try{
            Picasso.get().load(uDp).placeholder(R.drawable.ic_face_black).into(holder.uPictureIv);
        }
        catch (Exception e){

        }
        try{
            holder.pVideoVv.setVideoPath(pVideo);
            holder.pVideoVv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    holder.videoProgrssBar.setVisibility(View.GONE);
                    mp.start();

                    float videoRatio=mp.getVideoWidth()/(float) mp.getVideoHeight();
                    float screenRatio=holder.pVideoVv.getWidth() / (float) holder.pVideoVv.getHeight();

                    float scale=videoRatio/screenRatio;
                    if (scale>=1f){
                        holder.pVideoVv.setScaleX(scale);
                    }
                    else {
                        holder.pVideoVv.setScaleY(1f / scale);
                    }
                }
            });
            holder.pVideoVv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.start();
                }
            });
        }
        catch (Exception e){

        }

        if (myUid.equals(hisUID)){
            holder.commentBtn.setVisibility(View.GONE);
            holder.pCommentsTv.setVisibility(View.GONE);
        }
        else{
            holder.commentBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(context, ChatActivity.class);
                    intent.putExtra("hisUid",hisUID);
                    context.startActivity(intent);
                }
            });
        }


        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pLikes= Integer.parseInt(videopostList.get(position).getpLikes());
                mProcessLike=true;
                String postIde=videopostList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mProcessLike){
                            if (snapshot.child(postIde).hasChild(myUid)){
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes-1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike=false;
                                addToHisNotifications(""+uid,""+pId,"Unliked your story");
                            }
                            else {
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue("Liked");
                                mProcessLike=false;
                                addToHisNotifications(""+uid,""+pId,"Liked your story");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
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

    private void setLikes(ViewHolder myholder, String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(String.valueOf(postKey)).hasChild(myUid)){
                    myholder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like,0,0,0);
                }
                else {
                    myholder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_video_like,0,0,0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public int getItemCount() {
        return videopostList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        ImageView uPictureIv;
        TextView uNameTv,pTimeTv,pTitleTv,pDescriptionTv,pLikesTv,pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn,commentBtn,shareBtn;
        LinearLayout profileLayout;
        ProgressBar videoProgrssBar;
        VideoView pVideoVv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            uPictureIv=itemView.findViewById(R.id.uPictureIv);
            videoProgrssBar=itemView.findViewById(R.id.videoProgrssBar);
            pVideoVv=itemView.findViewById(R.id.pVideoVv);
            uNameTv=itemView.findViewById(R.id.uNameTv);
            pTimeTv=itemView.findViewById(R.id.pTimeTv);
            pTitleTv=itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv=itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv=itemView.findViewById(R.id.pLikesTv);
            pCommentsTv=itemView.findViewById(R.id.pCommentsTv);
            moreBtn=itemView.findViewById(R.id.moreBtn);
            likeBtn=itemView.findViewById(R.id.likeBtn);
            commentBtn=itemView.findViewById(R.id.commentBtn);
            shareBtn=itemView.findViewById(R.id.shareBtn);
            profileLayout=itemView.findViewById(R.id.profileLayout);
        }
        void setVideoData(String url){

        }
    }
}
