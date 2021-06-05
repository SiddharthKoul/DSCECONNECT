package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private Spinner designation;
    private TextView userDOB;
    private int year,month,day;
    private ImageView calImage;
    private EditText userPhoneNumber,UserName,FullName;
    private Button SaveInformationButton;
    private CircleImageView ProfileImage;
    private String EncKey,EncID;

    //private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;
    private StorageReference UserProfileImageRef;
    private ProgressDialog loadingBar;

    String currentUserID;
    myDbAdapter helper;
    final static int Gallery_Pic=1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        //mAuth=FirebaseAuth.getInstance();

        helper = new myDbAdapter(this);

        //currentUserID=mAuth.getCurrentUser().getUid();
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
        UsersRef=FirebaseDatabase.getInstance().getReference().child("Users").child(EncID);

        loadingBar=new ProgressDialog(this);
        UserProfileImageRef= FirebaseStorage.getInstance().getReference().child("profile Images");

        UserName= (EditText)findViewById(R.id.setup_username);
        FullName=(EditText)findViewById(R.id.setup_full_name);
        userPhoneNumber=(EditText)findViewById(R.id.userPhoneNumber);
        SaveInformationButton=(Button)findViewById(R.id.setup_information_button);
        ProfileImage=(CircleImageView)findViewById(R.id.setup_profile_image);
        designation=(Spinner)findViewById(R.id.designation);
        userDOB=(TextView)findViewById(R.id.userDOB);

        SaveInformationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    SaveAccountSetupInformation(EncKey,EncID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(SetupActivity.this);
            }
        });


        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    if(snapshot.hasChild("profileimage"))
                    {
                        String image=snapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(ProfileImage);
                    }
                    else
                    {
                        Toast.makeText(SetupActivity.this,"Please select profile IMage first",Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(SetupActivity.this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.desigs));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        designation.setAdapter(myAdapter);

        userDOB= (TextView) findViewById(R.id.userDOB);
        calImage=(ImageView)findViewById(R.id.calImage);
        Calendar cal = Calendar.getInstance();
        calImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                year=cal.get(Calendar.YEAR);
                month=cal.get(Calendar.MONTH);
                day =cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog=new DatePickerDialog(SetupActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
//                        userDOB.setText(SimpleDateFormat.getDateInstance().format(cal.getTime()));
                        userDOB.setText(i2+"-"+(i1+1)+"-"+i);
                    }
                },year,month,day);
                datePickerDialog.show();
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
                            Toast.makeText(SetupActivity.this,"Profile Image Uploaded",Toast.LENGTH_SHORT).show();
                            final String downloadUri = task.getResult().toString();
                            UsersRef.child("profileimage").setValue(downloadUri).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        Intent selfIntent=new Intent(SetupActivity.this,SetupActivity.class);
                                        startActivity(selfIntent);
                                        Toast.makeText(SetupActivity.this,"profile image stored to database successfully",Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                    else
                                    {
                                        String message=task.getException().getMessage();
                                        Toast.makeText(SetupActivity.this,"error occurred: "+message,Toast.LENGTH_SHORT).show();
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

    private void SaveAccountSetupInformation(String encKey, String encID)throws Exception
    {
        String username=UserName.getText().toString();
        String fullname=FullName.getText().toString();
        String phoneNumber=userPhoneNumber.getText().toString();
        String userdesignation=designation.getSelectedItem().toString();
        String userdob=userDOB.getText().toString();

        if(TextUtils.isEmpty(username)||username.length()>255||username.length()<4)
        {
            Toast.makeText(this,"Username length should be greater than 3 and less than 256",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(fullname)||fullname.length()<1||fullname.length()>256||fullname.equals("DSCE"))
        {
            Toast.makeText(this,"Full Name length should be greater than 3 and less than 256",Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(phoneNumber)||phoneNumber.length()<10||phoneNumber.length()>10)
        {
            Toast.makeText(this,"Please enter your valid Phone Number",Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(userdesignation))
        {
            Toast.makeText(this,"Please enter your designation",Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(userdob))
        {
            Toast.makeText(this,"Please enter your DOB",Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Saving Information");
            loadingBar.setMessage("Please wait, while we create your new account");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            String encPhone=CryptoUtils.encrypt(phoneNumber.getBytes(StandardCharsets.UTF_8),EncKey);
            String encDOB=CryptoUtils.encrypt(userdob.getBytes(StandardCharsets.UTF_8),EncKey);
            String status="Hey there";
            String gender="none";
            String relation="none";
            HashMap<String,Object> userMap = new HashMap<String,Object>();
            userMap.put("username",username.toString());
            userMap.put("fullname",fullname.toString());
            userMap.put("phoneNumber",encPhone);
            userMap.put("designation",userdesignation.toString());
            userMap.put("dob",encDOB.toString());
            userMap.put("status",status.toString());
            userMap.put("gender",gender.toString());
            userMap.put("relationshipstatus",relation.toString());

            UsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if(task.isSuccessful())
                    {
                        SendUserTomainActivity();
                        Toast.makeText(SetupActivity.this,"Your Account is created successfully",Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                    }
                    else{
                        String message=task.getException().getMessage();
                        Toast.makeText(SetupActivity.this,"Error occured: "+message,Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }

    }

    private void SendUserTomainActivity()
    {
        Intent mainIntent= new Intent(SetupActivity.this,MainActivity.class);
        mainIntent.putExtra("key_encKey",EncKey);
        mainIntent.putExtra("key_encID",EncID);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}