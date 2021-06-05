package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView myFriendList;
    private DatabaseReference FriendsRef,UsersRef;

    private myDbAdapter helper;

    private String EncKey,EncID;
    //private String online_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        helper=new myDbAdapter(this);

        EncKey=getIntent().getStringExtra("key_encKey");
        EncID=getIntent().getStringExtra("key_encID");

        if(EncKey==null||EncID==null)
        {
            try {
                String finalString=helper.getEncData();
                String[] encparam=finalString.split(" ");
                EncKey=encparam[0];
                EncID=encparam[1];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        FriendsRef=FirebaseDatabase.getInstance().getReference().child("Friends").child(EncID);
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");

        myFriendList=(RecyclerView)findViewById(R.id.friend_list);
        myFriendList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myFriendList.setLayoutManager(linearLayoutManager);

        DisplayAllFriends();
    }



    private void DisplayAllFriends()
    {
        Query query= FriendsRef;
        FirebaseRecyclerOptions<Friends> foptions =
                new FirebaseRecyclerOptions.Builder<Friends>()
                        .setQuery(query, Friends.class)
                        .build();

        FirebaseRecyclerAdapter<Friends,FriendsViewHolder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(foptions) {
            @Override
            protected void onBindViewHolder(@NonNull FriendsViewHolder holder, int position, @NonNull Friends model)
            {
                holder.setDate(model.getDate());

                final String usersIDs=getRef(position).getKey();

                UsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        if(snapshot.exists())
                        {
                            final String userName= snapshot.child("fullname").getValue().toString();
                            final String profileImage= snapshot.child("profileimage").getValue().toString();
                            final String designation=snapshot.child("designation").getValue().toString();
                            final String type;

                            if(snapshot.hasChild("userState"))
                            {
                                type=snapshot.child("userState").child("type").getValue().toString();
                                if(type.equals("online"))
                                {
                                    holder.onlineStatusView.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    holder.onlineStatusView.setVisibility(View.INVISIBLE);
                                }
                            }

                            holder.setFullname(userName);
                            holder.setProfileimage(getApplicationContext(),profileImage);
                            holder.setDesignation(designation);

                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {
                                    CharSequence options[]=new CharSequence[]
                                            {
                                                    userName+"'s Profile",
                                                    "Send Message"
                                            };
                                    AlertDialog.Builder builder=new AlertDialog.Builder(FriendsActivity.this);
                                    builder.setTitle("Select Option");

                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            if(i==0)
                                            {
                                                Intent profileintent=new Intent(FriendsActivity.this,PersonProfileActivity.class);
                                                profileintent.putExtra("visit_user_id",usersIDs);
                                                startActivity(profileintent);
                                            }
                                            if(i==1)
                                            {
                                                Intent chatintent=new Intent(FriendsActivity.this,ChatActivity.class);
                                                chatintent.putExtra("visit_user_id",usersIDs);
                                                chatintent.putExtra("userName",userName);
                                                startActivity(chatintent);
                                            }
                                        }
                                    });
                                    builder.show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.all_user_display_layout,parent,false);
                return new FriendsActivity.FriendsViewHolder(view);
            }
        };
        firebaseRecyclerAdapter.startListening();
        myFriendList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;
        ImageView onlineStatusView;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView=itemView;
            onlineStatusView=(ImageView)mView.findViewById(R.id.all_user_online_icon);
        }

        public void setFullname(String fullname)
        {
            TextView username=(TextView)mView.findViewById(R.id.all_users_profile_full_name);
            username.setText(fullname);
        }

        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView image=(CircleImageView)mView.findViewById(R.id.all_users_profile_image);
            Picasso.get().load(profileimage).into(image);
        }

        public void setDesignation(String designation)
        {
            TextView myDesignation=(TextView)mView.findViewById(R.id.all_users_designation);
            myDesignation.setText(designation);
        }

        public void setDate(String date)
        {
            TextView friendsDate=(TextView)mView.findViewById(R.id.all_users_status);
            friendsDate.setText("Friends Since: "+date);
        }
    }
}