package com.satya.dotchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.satya.dotchat.R;
import com.satya.dotchat.models.ModelGroupChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class AdapterGroupChat extends RecyclerView.Adapter<AdapterGroupChat.HolderGroupChat> {
    private static final int MSG_TYPE_LEFT=0;
    private static final int MSG_TYPE_RIGHT=1;

    private Context context;
    private ArrayList<ModelGroupChat> modelGroupChatList;

    private FirebaseAuth firebaseAuth;

    public AdapterGroupChat(Context context, ArrayList<ModelGroupChat> modelGroupChatList) {
        this.context = context;
        this.modelGroupChatList = modelGroupChatList;

        firebaseAuth=FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderGroupChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType==MSG_TYPE_RIGHT){
            View view= LayoutInflater.from(context).inflate(R.layout.row_groupchat_right,parent,false);
            return new HolderGroupChat(view);
        }
        else {
            View view= LayoutInflater.from(context).inflate(R.layout.row_groupchat_left,parent,false);
            return new HolderGroupChat(view);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull HolderGroupChat holder, int position) {
        ModelGroupChat model=modelGroupChatList.get(position);
        String timestamp=model.getTimestamp();
        String message=model.getMessage();
        String senderUid=model.getSender();
        String messageType=model.getType();
        String filename=modelGroupChatList.get(position).getName();

        Calendar cal=Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timestamp));
        String dateTime= DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();


        if (messageType.equals("text")){
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);
            holder.pdfMessage.setVisibility(View.GONE);
            holder.messageTv.setText(message);
        }
        else if (messageType.equals("image")){
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
        else if (messageType.equals("pdf")){
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
        else if (messageType.equals("doc")){
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

        holder.timeTv.setText(dateTime);
        setUserName(model,holder);
    }

    private void setUserName(ModelGroupChat model, HolderGroupChat holder) {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(model.getSender())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){
                            String name=""+ds.child("name").getValue();
                            Random r = new Random();
                            int red=r.nextInt(255 - 0 + 1)+0;
                            int green=r.nextInt(255 - 0 + 1)+0;
                            int blue=r.nextInt(255 - 0 + 1)+0;
                            holder.nameTv.setText(name);
                            holder.nameTv.setTextColor(Color.rgb(red,green,blue));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return modelGroupChatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (modelGroupChatList.get(position).getSender().equals(firebaseAuth.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }

    class HolderGroupChat extends RecyclerView.ViewHolder{

        private TextView nameTv,messageTv,timeTv,filename;
        private ImageView messageIv,messagePdf;
        RelativeLayout pdfMessage;

        public HolderGroupChat(@NonNull View itemView) {
            super(itemView);
            nameTv=itemView.findViewById(R.id.nameTv);
            messageTv=itemView.findViewById(R.id.messageTv);
            timeTv=itemView.findViewById(R.id.timeTv);
            messageIv=itemView.findViewById(R.id.messageIv);
            messagePdf=itemView.findViewById(R.id.messagePdf);
            filename=itemView.findViewById(R.id.filename);
            pdfMessage=itemView.findViewById(R.id.pdfMessage);

        }
    }
}
