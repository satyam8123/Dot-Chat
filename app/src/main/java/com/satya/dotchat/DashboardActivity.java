package com.satya.dotchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.satya.dotchat.fragments.ChatListFragment;
import com.satya.dotchat.fragments.HomeFragment;
import com.satya.dotchat.fragments.NotificationsFragment;
import com.satya.dotchat.fragments.ProfileFragment;
import com.satya.dotchat.fragments.UsersFragment;
import com.satya.dotchat.fragments.VideoFragment;
import com.satya.dotchat.models.ModelChat;
import com.satya.dotchat.notifications.Token;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference reference,userRef;
    String  mUID;
    BottomNavigationView  navigationView;
    ActionBar actionBar;
    String calledBy="";
    List<ModelChat> chatList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        /*binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());*/

        actionBar=getSupportActionBar();
        actionBar.setTitle("Dot Chat");
        firebaseAuth=FirebaseAuth.getInstance();
        navigationView=findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);
        HomeFragment fragment1=new HomeFragment();
        FragmentTransaction ft1=getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content,fragment1,"");
        ft1.commit();
        FirebaseUser user=firebaseAuth.getCurrentUser();
        reference=FirebaseDatabase.getInstance().getReference("Chats");
        userRef=FirebaseDatabase.getInstance().getReference("Users");
        checkUserStatus();
        checkForReceivingCall();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_dashboard);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_dashboard);
        }
    }

    /*@Override
    protected void onDestroy() {
        super.onDestroy();
        binding=null;
    }*/

    @Override
    protected void onResume() {
        checkUserStatus();
        checkForReceivingCall();
        super.onResume();
    }

    public void updateToken(String token){
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken=new Token(token);
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if (user!=null){
            ref.child(mUID).setValue(mToken);
        }
    }
    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            (MenuItem item) -> {
                switch (item.getItemId()){
                    case R.id.nav_home:
                        HomeFragment fragment1=new HomeFragment();
                        FragmentTransaction ft1=getSupportFragmentManager().beginTransaction();
                        ft1.replace(R.id.content,fragment1,"");
                        ft1.commit();
                        return true;
                    case R.id.nav_chat:
                        ChatListFragment fragment2=new ChatListFragment();
                        FragmentTransaction ft2=getSupportFragmentManager().beginTransaction();
                        ft2.replace(R.id.content,fragment2,"");
                        ft2.commit();
                        return true;
                    case R.id.nav_videopost:
                        VideoFragment fragment3=new VideoFragment();
                        FragmentTransaction ft3=getSupportFragmentManager().beginTransaction();
                        ft3.replace(R.id.content,fragment3,"");
                        ft3.commit();
                        return true;
                    case R.id.nav_profile:
                        ProfileFragment fragment4=new ProfileFragment();
                        FragmentTransaction ft4=getSupportFragmentManager().beginTransaction();
                        ft4.replace(R.id.content,fragment4,"");
                        ft4.commit();
                        return true;
                    case R.id.nav_more:
                        showMoreOptions();
                        return true;
                }
                return false;
            };

    private void showMoreOptions() {
        PopupMenu popupMenu=new PopupMenu(this,navigationView, Gravity.END);

        popupMenu.getMenu().add(Menu.NONE,0,0,"Notification");
        popupMenu.getMenu().add(Menu.NONE,1,0,"Group Chats");
        popupMenu.getMenu().add(Menu.NONE,2,0,"Users");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id=item.getItemId();
                if (id==0){
                    NotificationsFragment fragment5=new NotificationsFragment();
                    FragmentTransaction ft5=getSupportFragmentManager().beginTransaction();
                    ft5.replace(R.id.content,fragment5,"");
                    ft5.commit();
                }
                else if (id==1){
                    GroupChatsFragment fragment6=new GroupChatsFragment();
                    FragmentTransaction ft6=getSupportFragmentManager().beginTransaction();
                    ft6.replace(R.id.content,fragment6,"");
                    ft6.commit();
                }
                else if (id==2){
                    UsersFragment fragment7=new UsersFragment();
                    FragmentTransaction ft7=getSupportFragmentManager().beginTransaction();
                    ft7.replace(R.id.content,fragment7,"");
                    ft7.commit();
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void checkUserStatus(){
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if (user!=null){
            //profileTv.setText(user.getEmail());
            mUID=user.getUid();
            SharedPreferences sp=getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor=sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();

            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                String deviceToken = instanceIdResult.getToken();
                updateToken(deviceToken);
            });
        }
        else {
            mUID=null;
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        checkForReceivingCall();
        super.onStart();
    }

    private void checkForReceivingCall() {
        FirebaseUser user=firebaseAuth.getCurrentUser();
        userRef.child(user.getUid())
                .child("Ringing")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("ringing")){
                            calledBy=snapshot.child("ringing").getValue().toString();
                            Intent callingIntent=new Intent(DashboardActivity.this,CallingActivity.class);
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