package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private ImageView calImage;
    private TextView userDOB,designation;
    private int year,month,day;
    private EditText userName,userProfName,userStatus,phoneNumber,userGender,userRelation;
    private Button UpdateAccountSettingsButton;
    private CircleImageView userProfImage;
    private Toolbar mToolbar;
    private DatabaseReference SettingsUserRef;
    private StorageReference UserProfileImageRef;


    private ProgressDialog loadingBar;

    private myDbAdapter helper;

    private String EncKey,EncID;

    final static int Gallery_Pic=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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


        SettingsUserRef= FirebaseDatabase.getInstance().getReference().child("Users").child(EncID);

        mToolbar=(Toolbar)findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        userName=(EditText)findViewById(R.id.settings_username);
        userProfName=(EditText)findViewById(R.id.settings_profile_full_name);
        designation=(TextView)findViewById(R.id.settings_designation);
        userStatus=(EditText)findViewById(R.id.settings_status);
        userGender=(EditText)findViewById(R.id.settings_gender);
        userRelation=(EditText)findViewById(R.id.settings_relationship_status);
        phoneNumber=(EditText)findViewById(R.id.settings_phone_number);
        userProfImage=(CircleImageView)findViewById(R.id.settings_profile_image);
        UpdateAccountSettingsButton=(Button) findViewById(R.id.update_account_settings_button);
        userDOB=(TextView)findViewById(R.id.settings_dob);
        calImage=(ImageView)findViewById(R.id.settings_calImage);
        Calendar cal = Calendar.getInstance();calImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                year=cal.get(Calendar.YEAR);
                month=cal.get(Calendar.MONTH);
                day =cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog=new DatePickerDialog(SettingsActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
//                        userDOB.setText(SimpleDateFormat.getDateInstance().format(cal.getTime()));
                        userDOB.setText(i2+"-"+(i1+1)+"-"+i);
                    }
                },year,month,day);
                datePickerDialog.show();
            }
        });

        loadingBar=new ProgressDialog(this);
        UserProfileImageRef= FirebaseStorage.getInstance().getReference().child("profile Images");

        SettingsUserRef.addValueEventListener(new ValueEventListener() {
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
//                    Log.i("username",myUserName);
                    try {
                        myDOB=CryptoUtils.decrypt(myDOB,EncKey);
                        myPhoneNumber=CryptoUtils.decrypt(myPhoneNumber,EncKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Picasso.get().load(myProfileImage).placeholder(R.drawable.profile).into(userProfImage);
                    userName.setText(myUserName);
                    userProfName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    userDOB.setText(myDOB);
                    designation.setText(myDesignation);
                    userRelation.setText(myRelationStatus);
                    phoneNumber.setText(myPhoneNumber);
                    userGender.setText(myGender);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        UpdateAccountSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                try {
                    ValidateAccountInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        userProfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(SettingsActivity.this);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE&& resultCode==RESULT_OK && data!=null)
        {
            Uri ImageUri= data.getData();
            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            if(resultCode== RESULT_OK)
            {
                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we Upload your profile image");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();


                Uri resultUri = result.getUri();
                StorageReference filePath = UserProfileImageRef.child(EncID+ ".jpg");

                filePath.putFile(resultUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
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
                            Toast.makeText(SettingsActivity.this,"Profile Image Uploaded",Toast.LENGTH_SHORT).show();
                            final String downloadUri = task.getResult().toString();
                            SettingsUserRef.child("profileimage").setValue(downloadUri).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        Intent selfIntent=new Intent(SettingsActivity.this,SettingsActivity.class);
                                        startActivity(selfIntent);
                                        Toast.makeText(SettingsActivity.this,"profile image stored to database successfully",Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                    else
                                    {
                                        String message=task.getException().getMessage();
                                        Toast.makeText(SettingsActivity.this,"error occurred: "+message,Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                }
                            });
                        }
                    }
                });
            }
            else
            {
                Toast.makeText(this,"Error occurred: Image can't be cropped. try again",Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }


    private void ValidateAccountInfo() throws Exception {
        String username=userName.getText().toString();
        String profilename=userProfName.getText().toString();
        String status=userStatus.getText().toString();
        String relation=userRelation.getText().toString();
        String gender=userGender.getText().toString();
        String dob=userDOB.getText().toString();
        String userphoneNumber=phoneNumber.getText().toString();
        String userdesignation=designation.getText().toString();

        if(TextUtils.isEmpty(username))
        {
            Toast.makeText(this,"Please enter username",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(profilename))
        {
            Toast.makeText(this,"Please enter Profile Name",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(status))
        {
            Toast.makeText(this,"Please enter status",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(relation))
        {
            Toast.makeText(this,"Please enter Relationship Status",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(gender))
        {
            Toast.makeText(this,"Please enter Gender",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(dob))
        {
            Toast.makeText(this,"Please enter DOB",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(userphoneNumber))
        {
            Toast.makeText(this,"Please enter Phone Number",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(userdesignation))
        {
            Toast.makeText(this,"Please enter designation",Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Updating account");
            loadingBar.setMessage("Please wait, while we Upload your profile image");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            String encPhone=CryptoUtils.encrypt(userphoneNumber.getBytes(StandardCharsets.UTF_8),EncKey);
            String encDOB=CryptoUtils.encrypt(dob.getBytes(StandardCharsets.UTF_8),EncKey);
            UpdateAccountInfo(username,profilename,status,relation,gender,encDOB,encPhone,userdesignation);
        }
    }

    private void UpdateAccountInfo(String username, String profilename, String status, String relation, String gender, String dob, String userphoneNumber, String userdesignation)
    {
        HashMap userMap=new HashMap();
        userMap.put("username",username.toString());
        userMap.put("fullname",profilename.toString());
        userMap.put("status",status.toString());
        userMap.put("relationshipstatus",relation.toString());
        userMap.put("gender",gender.toString());
        userMap.put("dob",dob.toString());
        userMap.put("phoneNumber",userphoneNumber.toString());
        userMap.put("designation",userdesignation.toString());

        SettingsUserRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if(task.isSuccessful())
                {
                    SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this,"Account updated successfully",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
                else
                {
                    Toast.makeText(SettingsActivity.this,"Error occurred while updating account",Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });


    }
    private void SendUserToMainActivity()
    {
        Intent mainIntent= new Intent(SettingsActivity.this,MainActivity.class);
        mainIntent.putExtra("key_encKey",EncKey);
        mainIntent.putExtra("key_encID",EncID);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}