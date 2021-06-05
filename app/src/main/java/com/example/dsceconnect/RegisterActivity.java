package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.auth.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText UserEmail,UserPassword,UserCOnfirmPassword;
    private Button CreateAccountbutton;
    private ProgressDialog loadingBar;
    private String EncKey,EncID;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        UserEmail=(EditText)findViewById(R.id.register_email);
        UserPassword=(EditText)findViewById(R.id.register_password);
        UserCOnfirmPassword=(EditText)findViewById(R.id.register_confirm_password);
        CreateAccountbutton=(Button)findViewById(R.id.register_create_account);

        mAuth=FirebaseAuth.getInstance();

        loadingBar=new ProgressDialog(this);
        CreateAccountbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                CreateNewAccount();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        FirebaseUser currentUser= mAuth.getCurrentUser();

        if(currentUser!=null)
        {
            String userUID=mAuth.getCurrentUser().getUid();
            try {
                EncKey=EncModel.generateEncryptionKey(userUID);
                EncID=EncModel.genreateID(EncKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SendUserToMainActivity(EncKey,EncID);
        }
    }

    private void SendUserToMainActivity(String encKey, String encID)
    {
        Intent mainIntent= new Intent(RegisterActivity.this,MainActivity.class);
        mainIntent.putExtra("key_encKey",encKey);
        mainIntent.putExtra("key_encID",encID);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void CreateNewAccount()
    {
        String email=UserEmail.getText().toString();
        String password= UserPassword.getText().toString();
        String confirmPassword=UserCOnfirmPassword.getText().toString();

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(this,"Please give email",Toast.LENGTH_SHORT).show();

        }
        else if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this,"Please provide password",Toast.LENGTH_SHORT).show();
        }
        else if(password.length()<8)
        {
            Toast.makeText(this,"Minimum password length is 8",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(confirmPassword))
        {
            Toast.makeText(this,"Please confirm password",Toast.LENGTH_SHORT).show();
        }
        else if(!password.equals(confirmPassword))

        {
            Toast.makeText(this,"Password and confirm password should match",Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait, while we create your new account");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful()) {
                        SendEmailVerificationMessage();
                        loadingBar.dismiss();
                    }
                    else
                    {
                        String message= task.getException().getMessage();
                        Toast.makeText(RegisterActivity.this,"Error Occured: " +message,Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    public void SendEmailVerificationMessage()
    {
        FirebaseUser user=mAuth.getCurrentUser();

        if(user !=null)
        {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if(task.isSuccessful())
                    {
                        Toast.makeText(RegisterActivity.this,"Registration successful. We have sent you a mail. Please verify your account",Toast.LENGTH_SHORT).show();
                        SendUserToLoginActivity();
                        mAuth.signOut();
                    }
                    else
                    {
                        String message= task.getException().getMessage();
                        Toast.makeText(RegisterActivity.this,"Error: "+message,Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                }
            });
        }
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent=new Intent(RegisterActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}