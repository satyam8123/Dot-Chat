package com.satya.dotchat.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.ChatActivity;
import com.satya.dotchat.R;
import com.satya.dotchat.ThereProfileActivity;
import com.satya.dotchat.models.ModelChat;
import com.satya.dotchat.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.MyHolder> {

    Context context;
    List<ModelUser> userList;
    private HashMap<String,String> lastMessageMap;
    FirebaseAuth firebaseAuth;
    DatabaseReference reference,usersDbRef;
    FirebaseDatabase firebaseDatabase;
    String myUid;

    public AdapterChatlist(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        lastMessageMap = new HashMap<>();
        firebaseAuth= FirebaseAuth.getInstance();
        myUid=firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.row_chatlist,parent,false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        String hisUid=userList.get(position).getUid();
        String userImage=userList.get(position).getImage();
        String userName=userList.get(position).getName();
        String lastMessage=lastMessageMap.get(hisUid);

        holder.nameTv.setText(userName);
        if (lastMessage==null || lastMessage.equals("default")){
            holder.lastMessageTv.setVisibility(View.GONE);
            holder.unseencount.setVisibility(View.GONE);
        }
        else {
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText(lastMessage);
        }
        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_face_black).into(holder.profileIv);
        }
        catch (Exception e){
            Picasso.get().load(R.drawable.ic_face_black).into(holder.profileIv);
        }
        if (userList.get(position).getOnlineStatus().equals("online")){
            holder.onlineStatusIv.setImageResource(R.drawable.diamond_online);
        }
        else {
            holder.onlineStatusIv.setImageResource(R.drawable.diamond_offline);
        }

        checkIsBlocked(hisUid,holder,position);

        reference=FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unread=0;
                for(DataSnapshot snapshot1:snapshot.getChildren()){
                    ModelChat chat=snapshot1.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) && !"true".equals(chat.getIsSeen())){
                        unread++;
                    }
                }
                if (0<unread){
                    holder.lastMessageTv.setTextColor(Color.GREEN);
                    holder.unseencount.setText(String.valueOf(unread));
                    holder.unseencount.setVisibility(View.VISIBLE);
                    unread--;
                }
                else {
                    holder.lastMessageTv.setTextColor(Color.BLACK);
                    holder.unseencount.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        firebaseDatabase=FirebaseDatabase.getInstance();
        usersDbRef=firebaseDatabase.getReference("Users");
        Query userQuery=usersDbRef.orderByChild("uid").equalTo(hisUid);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    String typingStatus=""+ds.child("typingTo").getValue();
                    if (typingStatus.equals(myUid)){
                        holder.lastMessageTv.setTextColor(Color.GREEN);
                        holder.lastMessageTv.setText("typing...");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder=new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which==0){
                            Intent intent=new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid",hisUid);
                            context.startActivity(intent);
                        }
                        if (which==1){
                            imBlockedOrNot(hisUid);
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    private void imBlockedOrNot(final String hisUID){
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            if (ds.exists()){
                                Toast.makeText(context, "You're Blocked by that user,can't send message",  Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        Intent intent=new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid",hisUID);
                        context.startActivity(intent);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, AdapterChatlist.MyHolder holder, int position) {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            if (ds.exists()){
                                userList.get(position).setBlocked(true);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public void setLastMessageMap(String userId,String lastMessage){
        lastMessageMap.put(userId,lastMessage);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    class MyHolder extends RecyclerView.ViewHolder{
        ImageView profileIv,onlineStatusIv;
        TextView nameTv,lastMessageTv,unseencount;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            profileIv=itemView.findViewById(R.id.profileIv);
            onlineStatusIv=itemView.findViewById(R.id.onlineStatusIv);
            nameTv=itemView.findViewById(R.id.nameTv);
            lastMessageTv=itemView.findViewById(R.id.lastMessageTv);
            unseencount=itemView.findViewById(R.id.unseencount);
        }
    }
}
