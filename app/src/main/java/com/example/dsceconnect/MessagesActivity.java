package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.Timestamp;
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

public class MessagesActivity extends AppCompatActivity {
    private RecyclerView myMessagesList;
    private DatabaseReference MessagesRef,UsersRef;

    private myDbAdapter helper;

    private String EncKey,EncID;
    //private String online_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

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

        MessagesRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(EncID);
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users");

        myMessagesList =(RecyclerView)findViewById(R.id.request_list);
        myMessagesList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myMessagesList.setLayoutManager(linearLayoutManager);

        DisplayAllMessages();
    }



    private void DisplayAllMessages()
    {
        Query query= MessagesRef.orderByChild("chatstatus/timestamp2");
        FirebaseRecyclerOptions<MessagesListModel> foptions =
                new FirebaseRecyclerOptions.Builder<MessagesListModel>()
                        .setQuery(query, MessagesListModel.class)
                        .build();

        FirebaseRecyclerAdapter<MessagesListModel, MessagesViewHolder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<MessagesListModel, MessagesViewHolder>(foptions) {
            @Override
            protected void onBindViewHolder(@NonNull MessagesViewHolder holder, int position, @NonNull MessagesListModel model)
            {
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
                            final String status=snapshot.child("status").getValue().toString();
                            final String type;
                            MessagesRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot)
                                {
                                    if(snapshot.exists())
                                    {
                                        if(snapshot.hasChild("chatstatus"))
                                        {
                                            final String readState=snapshot.child("chatstatus").child("readstatus").getValue().toString();
                                            holder.setReadstatus(readState);
                                        }
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                            holder.setFullname(userName);
                            holder.setProfileimage(getApplicationContext(),profileImage);
                            holder.setDesignation(designation);
                            holder.setStatus(status);


                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view)
                                {
                                    Intent chatintent=new Intent(MessagesActivity.this,ChatActivity.class);
                                    chatintent.putExtra("visit_user_id",usersIDs);
                                    chatintent.putExtra("userName",userName);
                                    startActivity(chatintent);
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
            public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.all_user_display_layout,parent,false);
                return new MessagesViewHolder(view);
            }
        };
        firebaseRecyclerAdapter.startListening();
        myMessagesList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class MessagesViewHolder extends RecyclerView.ViewHolder
    {
        View mView;
        ImageView onlineStatusView;

        public MessagesViewHolder(@NonNull View itemView) {
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

        public void setStatus(String status)
        {
            TextView myStatus=(TextView)mView.findViewById(R.id.all_users_status);
            myStatus.setText(status);
        }

        public void setReadstatus(String readstatus){
            TextView myReadState=(TextView)mView.findViewById(R.id.read_state);
            if(readstatus.equals("no"))
            {
                myReadState.setVisibility(View.VISIBLE);
            }
            else
            {
                myReadState.setVisibility(View.INVISIBLE);
            }
        }
    }
}