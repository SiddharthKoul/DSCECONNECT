package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;


public class PostActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ProgressDialog loadingBar;

    private ImageButton SelectPostImage;
    private Button UpdatePostButton;
    private EditText PostDescription;

    private static final int Gallery_Pic=1;
    private Uri ImageUri;
    private String Description;
    private String saveCurrentDate,saveCurrentTime,postRandomName,downloadURL,current_user_id;
    private long countPosts=0;


    private StorageReference PostImagesReference;
    private DatabaseReference UsersRef,PostsRef,indref;
    //private FirebaseAuth mAuth;

    private String EncKey,EncID;

    myDbAdapter helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        //mAuth=FirebaseAuth.getInstance();
        //current_user_id=mAuth.getCurrentUser().getUid();
        PostImagesReference= FirebaseStorage.getInstance().getReference();
        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef= FirebaseDatabase.getInstance().getReference().child("Posts");


        SelectPostImage=(ImageButton)findViewById(R.id.select_post_image);
        UpdatePostButton=(Button)findViewById(R.id.update_post_button);
        PostDescription=(EditText)findViewById(R.id.post_description);
        loadingBar=new ProgressDialog(this);


        EncKey=getIntent().getStringExtra("key_encKey");
        EncID=getIntent().getStringExtra("key_encID");


        if(EncKey==null||EncID==null)
        {
            helper = new myDbAdapter(this);
            String finalString=helper.getEncData();
            String[] encparam=finalString.split(" ");
            EncKey=encparam[0];
            EncID=encparam[1];
        }



        mToolbar=(Toolbar)findViewById(R.id.update_post_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Update Post");

        SelectPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                OpenGallery();
            }
        });


        UpdatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                ValidatePostInfo();
            }
        });
    }

    private void ValidatePostInfo()
    {
        Description=PostDescription.getText().toString();

        if(ImageUri==null)
        {
            Toast.makeText(this,"Please Select Post Image",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(Description))
        {
            Toast.makeText(this,"Please Enter Caption",Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Add new Post");
            loadingBar.setMessage("Please wait, while we updating your new post");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            StoreingImageToFirebaseStorage();
        }
    }

    private void StoreingImageToFirebaseStorage()
    {
        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyy");
        saveCurrentDate=currentDate.format(calForDate.getTime());
        Calendar calForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH-mm");
        saveCurrentTime=currentTime.format(calForDate.getTime());
        postRandomName=saveCurrentDate+saveCurrentTime;
        StorageReference filePath=PostImagesReference.child("Post Images").child(ImageUri.getLastPathSegment()+postRandomName+".jpg");
        filePath.putFile(ImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>(){
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful()){
                    throw task.getException();
                }
                return filePath.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task)
            {
                if(task.isSuccessful())
                {
                    downloadURL=task.getResult().toString();
                    Toast.makeText(PostActivity.this,"IMAGE UPLOADED SUCCESSFULLY",Toast.LENGTH_SHORT).show();
                    SavingPostInformationToDatabase(EncKey,EncID);
                }
                else
                {
                    String message=task.getException().getMessage();
                    Toast.makeText(PostActivity.this,"Error occurred: "+message,Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void SavingPostInformationToDatabase(String encKey, String encID)
    {
        UsersRef.child(EncID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {

                if(snapshot.exists())
                {
                    Log.i("status","entered");
                    String userProfileImage=snapshot.child("profileimage").getValue().toString();
                    String userFullName=snapshot.child("fullname").getValue().toString();

                    HashMap postsMap= new HashMap();
                    postsMap.put("uid",encID.toString());
                    postsMap.put("date",saveCurrentDate.toString());
                    postsMap.put("time",saveCurrentTime.toString());
                    postsMap.put("description",Description.toString());
                    postsMap.put("postimage",downloadURL.toString());
                    postsMap.put("profileimage",userProfileImage.toString());
                    postsMap.put("fullname",userFullName.toString());
                    postsMap.put("timestamp", ServerValue.TIMESTAMP);

                    PostsRef.child(encID+postRandomName).updateChildren(postsMap).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task)
                        {
                            if(task.isSuccessful())
                            {
                                Log.i("finalize","done");
                                SendUserToMainActivity();
                                Toast.makeText(PostActivity.this,"New post is updated successfully",Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                            else
                            {
                                Toast.makeText(PostActivity.this,"Error occurred while updating your post",Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();;
                            }
                        }
                    });


                }
                else
                {
                    Log.i("error message","Snapshot doesnot exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void OpenGallery()
    {
        Intent galleryIntent=new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,Gallery_Pic);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Gallery_Pic && resultCode==RESULT_OK && data!=null)
        {
            ImageUri=data.getData();
            SelectPostImage.setImageURI(ImageUri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id= item.getItemId();
        if(id==android.R.id.home)
        {
            SendUserToMainActivity();
        }

        return super.onOptionsItemSelected(item);

    }

    private void SendUserToMainActivity() {
        Intent mainIntent=new Intent(PostActivity.this,MainActivity.class);
        startActivity(mainIntent);
        finish();

    }


}