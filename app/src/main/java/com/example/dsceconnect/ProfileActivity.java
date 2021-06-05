package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private TextView userName,userProfName,userStatus,phoneNumber,userGender,userRelation,userDOB,designation;
    private CircleImageView userProfileImage;

    private DatabaseReference profileUserRef, FriendsRef, PostsRef;
    private myDbAdapter helper;
    private Button MyPosts,MyFriends;

    private String EncKey,EncID;
    private int countFriends=0,countPosts=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        helper = new myDbAdapter(this);
        EncKey=getIntent().getStringExtra("key_encKey");
        EncID=getIntent().getStringExtra("key_encID");

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

        profileUserRef= FirebaseDatabase.getInstance().getReference().child("Users").child(EncID);
        FriendsRef=FirebaseDatabase.getInstance().getReference().child("Friends");
        PostsRef=FirebaseDatabase.getInstance().getReference().child("Posts");
        userName = (TextView) findViewById(R.id.my_username);
        userProfName = (TextView) findViewById(R.id.my_profile_full_name);
        userStatus = (TextView) findViewById(R.id.my_profile_status);
        designation = (TextView) findViewById(R.id.my_designation);
        userGender = (TextView) findViewById(R.id.my_gender);
        userProfileImage = (CircleImageView) findViewById(R.id.my_profile_pic);
        phoneNumber=(TextView)findViewById(R.id.my_phone_number);
        userDOB=(TextView)findViewById(R.id.my_DOB);
        userRelation=(TextView)findViewById(R.id.my_relation);

        MyFriends=(Button)findViewById(R.id.my_friends_button);
        MyPosts=(Button)findViewById(R.id.my_post_button);

        MyFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToFriendsActivity(EncKey,EncID);
            }
        });

        MyPosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToMyPostsActivity(EncKey,EncID);
            }
        });

        PostsRef.orderByChild("uid").startAt(EncID).endAt(EncID+"\uf8ff").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    countPosts=(int)snapshot.getChildrenCount();
                    MyPosts.setText(Integer.toString(countPosts)+" Posts");
                }else
                {
                    MyPosts.setText("0 Posts");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        FriendsRef.child(EncID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.exists())
                {
                    countFriends=(int)snapshot.getChildrenCount();
                    MyFriends.setText(Integer.toString(countFriends)+" connections");
                }
                else
                {
                    MyFriends.setText("0 Connections");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        profileUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    String myProfileImage=snapshot.child("profileimage").getValue().toString();
                    String myUserName=snapshot.child("username").getValue().toString();
                    String myProfileName=snapshot.child("fullname").getValue().toString();
                    String myProfileStatus=snapshot.child("status").getValue().toString();
                    String myDOB=snapshot.child("dob").getValue().toString();
                    String myDesignation=snapshot.child("designation").getValue().toString();
                    String myRelationStatus=snapshot.child("relationshipstatus").getValue().toString();
                    String myPhoneNumber=snapshot.child("phoneNumber").getValue().toString();
                    String myGender=snapshot.child("gender").getValue().toString();

                    try {
                        myDOB=CryptoUtils.decrypt(myDOB,EncKey);
                        myPhoneNumber=CryptoUtils.decrypt(myPhoneNumber,EncKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Picasso.get().load(myProfileImage).placeholder(R.drawable.profile).into(userProfileImage);
                    userName.setText("@"+myUserName);
                    userProfName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    userDOB.setText("DOB: "+myDOB);
                    designation.setText(myDesignation);
                    userRelation.setText("Relationship: "+myRelationStatus);
                    phoneNumber.setText("Phone number: "+myPhoneNumber);
                    userGender.setText("Gender: "+myGender);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SendUserToFriendsActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(ProfileActivity.this,FriendsActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    private void SendUserToMyPostsActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(ProfileActivity.this,MyPostsActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }
}