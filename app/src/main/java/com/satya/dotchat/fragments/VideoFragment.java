package com.satya.dotchat.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.satya.dotchat.R;
import com.satya.dotchat.adapters.AdapterVideoPosts;
import com.satya.dotchat.models.ModelVideoPost;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;


public class VideoFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    List<ModelVideoPost> videopostList;
    List<String> followingList;
    AdapterVideoPosts adapterVideoPosts;
    SimpleExoPlayer exoPlayer;
    String myid;
    FirebaseUser user;
    ViewPager2 videosViewPager;

    public VideoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_video, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        firebaseAuth= FirebaseAuth.getInstance();
        user=firebaseAuth.getCurrentUser();
        myid=user.getUid();
        videosViewPager=view.findViewById(R.id.videosViewPager);
        videopostList=new ArrayList<>();
        deletePosts();
        checkFollowing();
        return view;
    }

    private void deletePosts() {
        String filePathAndName="VideoPosts";
        StorageReference storageReference= FirebaseStorage.getInstance().getReference();
        StorageReference listRef = storageReference.child(filePathAndName);
        listRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for (StorageReference prefix : listResult.getPrefixes()) {
                    // All the prefixes under listRef.
                    // You may call listAll() recursively on them.
                }
                for (StorageReference item : listResult.getItems()) {
                    // All the items under listRef.
                    item.getMetadata()
                            .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    long creationtime =storageMetadata.getCreationTimeMillis();
                                    String ss=storageMetadata.getName();
                                    long currenttime=System.currentTimeMillis();
                                    long deference=currenttime-creationtime;
                                    long i=86400000;
                                    //86400000
                                    if (i<deference){
                                        item.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Query fquery = FirebaseDatabase.getInstance().getReference("VideoPosts").orderByChild("pTime").equalTo(ss);
                                                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot ds : snapshot.getChildren()){
                                                            ds.getRef().removeValue();
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

                                            }
                                        });
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                }
                            });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });  //load posts
    }

    private void checkFollowing(){
        followingList=new ArrayList<>();

        DatabaseReference reference=FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("friends");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                followingList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){
                    followingList.add(ds.getKey());
                }
                loadPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPosts(){
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("VideoPosts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                videopostList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelVideoPost modelPost=ds.getValue(ModelVideoPost.class);
                    if (modelPost.getUid().equals(myid)){
                        videopostList.add(0,modelPost);
                        videosViewPager.setAdapter(new AdapterVideoPosts(getActivity(),videopostList));
                    }
                    for (String id:followingList){
                        if (modelPost.getUid().equals(id)){
                            videopostList.add(0,modelPost);
                            videosViewPager.setAdapter(new AdapterVideoPosts(getActivity(),videopostList));
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
    }
}