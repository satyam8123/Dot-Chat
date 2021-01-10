package com.satya.dotchat.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.GroupCreateActivity;
import com.satya.dotchat.MainActivity;
import com.satya.dotchat.R;
import com.satya.dotchat.SettingsActivity;
import com.satya.dotchat.adapters.AdapterChatlist;
import com.satya.dotchat.models.ModelChat;
import com.satya.dotchat.models.ModelChatlist;
import com.satya.dotchat.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView recyclerView;
    LinearLayout nochats;
    List<ModelChatlist> chatlistList;
    List<ModelUser> userList;
    DatabaseReference reference;
    FirebaseUser currentUser;
    Button createchat;
    AdapterChatlist adapterChatlist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_chat_list, container, false);

        firebaseAuth=FirebaseAuth.getInstance();
        currentUser=FirebaseAuth.getInstance().getCurrentUser();

        recyclerView=view.findViewById(R.id.recyclerView);
        nochats=view.findViewById(R.id.nochats);
        createchat=view.findViewById(R.id.createchat);

        chatlistList=new ArrayList<>();
        nochats.setVisibility(View.GONE);

        reference= FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    chatlistList.clear();
                    for (DataSnapshot ds:snapshot.getChildren()){
                        ModelChatlist chatlist=ds.getValue(ModelChatlist.class);
                        chatlistList.add(chatlist);
                    }
                    loadChats();
                }
                else {
                    nochats.setVisibility(View.VISIBLE);
                    createchat.setVisibility(View.GONE);
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return view;
    }
    private void loadChats() {
        userList=new ArrayList<>();
        reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelUser user=ds.getValue(ModelUser.class);
                    for (ModelChatlist chatlist1:chatlistList){
                        assert user != null;
                        if (user.getUid() !=null && user.getUid().equals(chatlist1.getId())){
                            userList.add(user);
                            break;
                        }
                    }
                    adapterChatlist =new AdapterChatlist(getContext(),userList);
                    recyclerView.setAdapter(adapterChatlist);
                    for (int i=0;i<userList.size();i++){
                        lastMessage(userList.get(i).getUid());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    private void lastMessage(String userid) {
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String theLastMessage="default";
                for (DataSnapshot ds:snapshot.getChildren()){
                    ModelChat chat=ds.getValue(ModelChat.class);
                    if (chat==null){
                        continue;
                    }
                    String sender=chat.getSender();
                    String receiver=chat.getReceiver();
                    if (sender==null || receiver==null){
                        continue;
                    }
                    if (chat.getReceiver().equals(currentUser.getUid()) &&
                            chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) &&
                                    chat.getSender().equals(currentUser.getUid())){
                        if (chat.getType().equals("image")){
                            theLastMessage="Sent a photo";
                        }
                        else if (chat.getType().equals("pdf")){
                            theLastMessage="Sent a pdf";
                        }
                        else if (chat.getType().equals("doc")){
                            theLastMessage="Sent a Document";
                        }
                        else {
                            theLastMessage=chat.getMessage();
                        }

                    }
                }
                adapterChatlist.setLastMessageMap(userid,theLastMessage);
                adapterChatlist.notifyDataSetChanged();
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
        }
        else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
        menu.findItem(R.id.action_call).setVisible(false);
        menu.findItem(R.id.action_video_call).setVisible(false);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        if (id==R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        else if (id==R.id.action_settings){
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        else if (id==R.id.action_create_group){
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}