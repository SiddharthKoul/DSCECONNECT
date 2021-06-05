package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonProfileActivity extends AppCompatActivity {

    private TextView userName,userProfName,userStatus,userGender,designation;
    private CircleImageView userProfileImage;
    private Button SendFriendRequestButton,DeclineFriendRequestButton;

    private DatabaseReference profileUserRef,UsersRef,FriendRequestRef,FriendsRef;
    private myDbAdapter helper;

    private String EncKey,EncID,senderUserId,receiverUserID,CURRENT_STATE,saveCurrentDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_profile);

        InitializeFields();

        helper=new myDbAdapter(this);

        receiverUserID=getIntent().getStringExtra("visit_user_id");
        EncKey=getIntent().getStringExtra("key_encKey");
        EncID=getIntent().getStringExtra("key_encID");

        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        FriendRequestRef=FirebaseDatabase.getInstance().getReference().child("FriendRequests");
        FriendsRef=FirebaseDatabase.getInstance().getReference().child("Friends");

        if(EncKey==null||EncID==null)
        {
            try {
                String finalString=helper.getEncData();
                String[] encparam=finalString.split(" ");
                EncKey=encparam[0];
                EncID=encparam[1];

//                EncKey=EncModel.generateEncryptionKey(currentUserID);
//                EncID=EncModel.genreateID(EncKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        senderUserId=EncID;

        UsersRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    String myProfileImage=snapshot.child("profileimage").getValue().toString();
                    String myUserName=snapshot.child("username").getValue().toString();
                    String myProfileName=snapshot.child("fullname").getValue().toString();
                    String myProfileStatus=snapshot.child("status").getValue().toString();
                    String myDesignation=snapshot.child("designation").getValue().toString();
                    String myGender=snapshot.child("gender").getValue().toString();

                    Picasso.get().load(myProfileImage).placeholder(R.drawable.profile).into(userProfileImage);
                    userName.setText("@"+myUserName);
                    userProfName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    designation.setText(myDesignation);
                    userGender.setText("Gender: "+myGender);


                    MaintenanceofButtons();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
        DeclineFriendRequestButton.setEnabled(false);

        if(!senderUserId.equals(receiverUserID))
        {
            SendFriendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    SendFriendRequestButton.setEnabled(false);

                    if(CURRENT_STATE.equals("not_friends"))
                    {
                        SendFriendRequestToaPerson();
                    }
                    if(CURRENT_STATE.equals("request_sent"))
                    {
                        CancelFriendRequest();
                    }
                    if(CURRENT_STATE.equals("request_received"))
                    {
                        AcceptFriendRequest();
                    }
                    if(CURRENT_STATE.equals("friends"))
                    {
                        UnFriendAnExistingFriend();
                    }
                }
            });
        }
        else
        {
            DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
            SendFriendRequestButton.setVisibility(View.INVISIBLE);
        }
    }

    private void UnFriendAnExistingFriend()
    {
        FriendsRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {
                    FriendsRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                SendFriendRequestButton.setEnabled(true);
                                CURRENT_STATE="not_friends";
                                SendFriendRequestButton.setText("Send Friend Request");

                                DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                DeclineFriendRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void AcceptFriendRequest()
    {
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyy");
        saveCurrentDate=currentDate.format(calForDate.getTime());

        FriendsRef.child(senderUserId).child(receiverUserID).child("date").setValue(saveCurrentDate).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {
                    FriendsRef.child(receiverUserID).child(senderUserId).child("date").setValue(saveCurrentDate).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                FriendRequestRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {
                                            FriendRequestRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if(task.isSuccessful())
                                                    {
                                                        SendFriendRequestButton.setEnabled(true);
                                                        CURRENT_STATE="friends";
                                                        SendFriendRequestButton.setText("Unfriend");

                                                        DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                                        DeclineFriendRequestButton.setEnabled(false);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void CancelFriendRequest()
    {
        FriendRequestRef.child(senderUserId).child(receiverUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {
                    FriendRequestRef.child(receiverUserID).child(senderUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                SendFriendRequestButton.setEnabled(true);
                                CURRENT_STATE="not_friends";
                                SendFriendRequestButton.setText("Send Friend Request");

                                DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                DeclineFriendRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void MaintenanceofButtons()
    {
        FriendRequestRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.hasChild(receiverUserID))
                {
                    String request_type=snapshot.child(receiverUserID).child("request_type").getValue().toString();

                    if(request_type.equals("sent"))
                    {
                        CURRENT_STATE="request_sent";
                        SendFriendRequestButton.setText("Cancel Friend Request");

                        DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                        DeclineFriendRequestButton.setEnabled(false);
                    }
                    else  if(request_type.equals("received"))
                    {
                        CURRENT_STATE="request_received";
                        SendFriendRequestButton.setText("Accept Friend Request");

                        DeclineFriendRequestButton.setVisibility(View.VISIBLE);
                        DeclineFriendRequestButton.setEnabled(true);

                        DeclineFriendRequestButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view)
                            {
                                CancelFriendRequest();
                            }
                        });
                    }
                }
                else
                {
                    FriendsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot)
                        {
                            if(snapshot.hasChild(receiverUserID))
                            {
                                CURRENT_STATE="friends";
                                SendFriendRequestButton.setText("Unfriend");

                                DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                DeclineFriendRequestButton.setEnabled(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SendFriendRequestToaPerson()
    {
        FriendRequestRef.child(senderUserId).child(receiverUserID).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
                if(task.isSuccessful())
                {
                    FriendRequestRef.child(receiverUserID).child(senderUserId).child("request_type").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                SendFriendRequestButton.setEnabled(true);
                                CURRENT_STATE="request_sent";
                                SendFriendRequestButton.setText("Cancel Friend Request");

                                DeclineFriendRequestButton.setVisibility(View.INVISIBLE);
                                DeclineFriendRequestButton.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }

    private void InitializeFields()
    {
        userName = (TextView) findViewById(R.id.person_username);
        userProfName = (TextView) findViewById(R.id.person_full_name);
        userStatus = (TextView) findViewById(R.id.person_profile_status);
        designation = (TextView) findViewById(R.id.person_designation);
        userGender = (TextView) findViewById(R.id.person_gender);
        userProfileImage = (CircleImageView) findViewById(R.id.person_profile_pic);
        SendFriendRequestButton=(Button)findViewById(R.id.person_send_friend_request_btn);
        DeclineFriendRequestButton=(Button)findViewById(R.id.person_decline_friend_request);

        CURRENT_STATE="not_friends";
    }
}