package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private Toolbar ChattoolBar;
    private ImageButton SendMesssageButton, SendImagefileButton;
    private EditText userMessageInput;
    private RecyclerView userMessagesList;
    private final List<Messages> messagesList=new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private  MessagesAdapter messageAdapter;
    private String messageReceiverID, getMessageReceiverName,messageSenderID,saveCurrentDate,saveCurrentTime;

    private TextView receiverName, userLastSeen;
    private CircleImageView receiverProfileImage;
    private DatabaseReference RootRef,UsersRef;
    private myDbAdapter helper;
    private String EncKey=null,EncID=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if(EncKey==null||EncID==null)
        {
            helper = new myDbAdapter(this);
            String finalString=helper.getEncData();
            String[] encparam=finalString.split(" ");
            EncKey=encparam[0];
            EncID=encparam[1];
        }

        messageSenderID=EncID;

        RootRef= FirebaseDatabase.getInstance().getReference();
        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");

        messageReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        getMessageReceiverName = getIntent().getExtras().get("userName").toString();

        IntializeFields();

        DisplayReceiverInfo();

        SendMesssageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SendMessage();
            }
        });



        Map senderchatStat =new HashMap();
        senderchatStat.put("readstatus","yes".toString());

        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).child("chatstatus").updateChildren(senderchatStat);

        FetchMessages();

    }



    private void FetchMessages()
    {
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).child("Messages2").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName)
            {
                if(snapshot.exists())
                {
                    Messages messages=snapshot.getValue(Messages.class);
                    messagesList.add(messages);
                    messageAdapter.notifyDataSetChanged();
                    userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void SendMessage()
    {
        String messageText=userMessageInput.getText().toString();
        if(TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this,"Please type a message",Toast.LENGTH_SHORT).show();
        }
        else
        {
            String message_sender_ref="Messages/"+messageSenderID+"/"+messageReceiverID+"/Messages2";
            String message_receiver_ref="Messages/"+messageReceiverID+"/"+messageSenderID+"/Messages2";

            DatabaseReference user_message_key=RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();
            String message_push_id=user_message_key.getKey();

            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyy");
            saveCurrentDate=currentDate.format(calForDate.getTime());

            Calendar calForTime = Calendar.getInstance();
            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm aa");
            saveCurrentTime=currentTime.format(calForDate.getTime());

            Map messageTextBody=new HashMap();
            messageTextBody.put("message",messageText.toString());
            messageTextBody.put("time",saveCurrentTime.toString());
            messageTextBody.put("date",saveCurrentDate.toString());
            messageTextBody.put("type","text".toString());
            messageTextBody.put("from",messageSenderID.toString().toString());

            Map senderchatStat =new HashMap();
            senderchatStat.put("timestamp2",ServerValue.TIMESTAMP);
            senderchatStat.put("readstatus","yes".toString());

            Map receivererchatStat =new HashMap();
            receivererchatStat.put("timestamp2",ServerValue.TIMESTAMP);
            receivererchatStat.put("readstatus","no".toString());

            Map messageBodyDetails=new HashMap();
            messageBodyDetails.put(message_sender_ref+"/"+message_push_id,messageTextBody);
            messageBodyDetails.put("Messages/"+messageSenderID+"/"+messageReceiverID+"/"+"chatstatus", senderchatStat);

            messageBodyDetails.put(message_receiver_ref+"/"+message_push_id,messageTextBody);
            messageBodyDetails.put("Messages/"+messageReceiverID+"/"+messageSenderID+"/"+"chatstatus",receivererchatStat);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if(task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this,"Message Sent Successfully",Toast.LENGTH_SHORT).show();
                        userMessageInput.setText("");
                    }
                    else
                    {
                        String message=task.getException().getMessage();
                        Toast.makeText(ChatActivity.this,"Error: "+message,Toast.LENGTH_SHORT).show();
                        userMessageInput.setText("");
                    }
                }
            });
        }
    }



    private void DisplayReceiverInfo()
    {
        receiverName.setText(getMessageReceiverName);

        RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    final String profileImage=snapshot.child("profileimage").getValue().toString();
                    Picasso.get().load(profileImage).placeholder(R.drawable.profile).into(receiverProfileImage);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void IntializeFields()
    {
        ChattoolBar = (Toolbar) findViewById(R.id.chat_bar_layout);
        setSupportActionBar(ChattoolBar);

        ActionBar actionBar=getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater=(LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view=layoutInflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(action_bar_view);

        receiverName=(TextView)findViewById(R.id.custom_profile_name);
        //userLastSeen=(TextView)findViewById(R.id.custom_user_last_seen);
        receiverProfileImage=(CircleImageView)findViewById(R.id.custom_profile_image);

        SendMesssageButton = (ImageButton) findViewById(R.id.send_message_button);
        //SendImagefileButton = (ImageButton) findViewById(R.id.send_image_file_button);
        userMessageInput = (EditText) findViewById(R.id.input_message);

        messageAdapter=new MessagesAdapter(messagesList);
        userMessagesList=(RecyclerView)findViewById(R.id.messages_list_users);
        linearLayoutManager=new LinearLayoutManager(this);
        userMessagesList.setHasFixedSize(true);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

    }
}