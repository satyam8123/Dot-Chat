package com.satya.dotchat.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.Profile;
import com.satya.dotchat.R;
import com.satya.dotchat.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder>{

    private static final int MSG_TYPE_LEFT=0;
    private static final int MSG_TYPE_RIGHT=1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType==MSG_TYPE_RIGHT){
            view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
        }
        else {
            view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);

        }
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        String message=chatList.get(position).getMessage();
        String timeStamp=chatList.get(position).getTimestamp();
        String type=chatList.get(position).getType();
        String filename=chatList.get(position).getName();


        Calendar cal=Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timeStamp));
        String dateTime= DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();

        if (type.equals("text")){
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);
            holder.pdfMessage.setVisibility(View.GONE);
            holder.messageTv.setText(message);
        }
        else if (type.equals("image")){
            holder.messageTv.setVisibility(View.GONE);
            holder.pdfMessage.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);
            Picasso.get().load(message).placeholder(R.drawable.ic_face_black).into(holder.messageIv);
            holder.messageIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                    holder.messageIv.getContext().startActivity(intent);
                }
            });
        }
        else if (type.equals("pdf")){
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.GONE);
            holder.pdfMessage.setVisibility(View.VISIBLE);
            holder.filename.setText(filename);
            holder.messagePdf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                    holder.messagePdf.getContext().startActivity(intent);
                }
            });
        }
        else if (type.equals("doc")){
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.GONE);
            holder.messagePdf.setVisibility(View.VISIBLE);
            holder.filename.setText(filename);
            holder.messagePdf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(message));
                    holder.messagePdf.getContext().startActivity(intent);
                }
            });
        }

        holder.messageTv.setText(message);
        holder.timeTv.setText(dateTime);
        try {
            Picasso.get().load(imageUrl).into(holder.profileTv);
        }
        catch (Exception e){

        }

        holder.messageLayout.setOnLongClickListener(v -> {
            AlertDialog.Builder builder=new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to Delete this message?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteMessage(position);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.create().show();
            return false;
        });

        holder.profileTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Profile.class);
                intent.putExtra("imageUrl", imageUrl);
                context.startActivity(intent);
            }
        });

        if (position==chatList.size()-1){
            if (chatList.get(position).getIsSeen().equals("true")){
                holder.isSeenTv.setText("Seen");
            }
            else {
                holder.isSeenTv.setText("Delivered");
            }
        }
        else {
            holder.isSeenTv.setVisibility(View.GONE);
        }

    }

    private void deleteMessage(int position) {
        final String myUID=FirebaseAuth.getInstance().getCurrentUser().getUid();
        String msgTimeStamp=chatList.get(position).getTimestamp();
        DatabaseReference dbRef= FirebaseDatabase.getInstance().getReference("Chats");
        Query query=dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds:snapshot.getChildren()){
                    if (ds.child("sender").getValue().equals(myUID)){
                        ds.getRef().removeValue();
                        /*HashMap<String,Object> hashMap=new HashMap<>();
                        hashMap.put("message","This message was deleted...");
                        ds.getRef().updateChildren(hashMap);*/
                        Toast.makeText(context, "message  deleted...", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "You can delete only your messages...", Toast.LENGTH_SHORT).show();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser= FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }

    static class MyHolder extends RecyclerView.ViewHolder{

        ImageView profileTv,messageIv,messagePdf;
        TextView messageTv,timeTv,isSeenTv,filename;
        RelativeLayout messageLayout;
        RelativeLayout pdfMessage;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileTv=itemView.findViewById(R.id.profileTv);
            messageIv=itemView.findViewById(R.id.messageIv);
            messagePdf=itemView.findViewById(R.id.messagePdf);
            filename=itemView.findViewById(R.id.filename);
            messageTv=itemView.findViewById(R.id.messageTv);
            timeTv=itemView.findViewById(R.id.timeTv);
            isSeenTv=itemView.findViewById(R.id.isSeenTv);
            messageLayout=itemView.findViewById(R.id.messageLayout);
            pdfMessage=itemView.findViewById(R.id.pdfMessage);
        }
    }

}
