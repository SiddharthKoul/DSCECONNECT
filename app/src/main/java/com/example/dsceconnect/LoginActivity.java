package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity {

    private Button LoginButton;
    private EditText UserEmail,UserPassword;
    private TextView NeedNewAccountLink,ForgetPasswordLink;
    private FirebaseAuth mAuth;
    private Boolean emailAddressChecker;

    private ProgressDialog loadingBar;
    private String EncKey,EncID;
    myDbAdapter helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth=FirebaseAuth.getInstance();

        NeedNewAccountLink = (TextView)findViewById(R.id.register_account_link);
        ForgetPasswordLink= (TextView)findViewById(R.id.forgot_password_link);
        UserEmail = (EditText)findViewById(R.id.login_email);
        UserPassword = (EditText)findViewById(R.id.login_password);
        LoginButton=(Button)findViewById(R.id.login_button);
        loadingBar=new ProgressDialog(this);


        NeedNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToRegisterActivity();
            }
        });

        ForgetPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(LoginActivity.this,ResetPasswordActivity.class));
            }
        });

        LoginButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                AllowingUserToLogin();
            }
        }));
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
            //helper = new myDbAdapter(this);
            SendUserToMainActivity(EncKey,EncID);
        }
    }

    private void AllowingUserToLogin()
    {
        String email=UserEmail.getText().toString();
        String password=UserPassword.getText().toString();
        if(TextUtils.isEmpty(email)){
            Toast.makeText(this,"Please Enter your email",Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(password)){
            Toast.makeText(this,"Please Enter your password",Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Login");
            loadingBar.setMessage("Please wait, while authenticating");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    if(task.isSuccessful())
                    {
                        VerifyEmailAddress();
                        loadingBar.dismiss();
                    }
                    else
                    {
                        String message=task.getException().getMessage();
                        Toast.makeText(LoginActivity.this,"Error occured: "+message,Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void VerifyEmailAddress()
    {
        FirebaseUser user = mAuth.getCurrentUser();
        emailAddressChecker=user.isEmailVerified();

        if(emailAddressChecker)
        {
            String userUID=mAuth.getCurrentUser().getUid();
            try {
                EncKey=EncModel.generateEncryptionKey(userUID);
                EncID=EncModel.genreateID(EncKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            helper = new myDbAdapter(LoginActivity.this);
            helper.deleteAll();
            helper.insertData(EncKey,EncID);
            SendUserToMainActivity(EncKey,EncID);
        }
        else
        {
            Toast.makeText(this,"Please verify you email", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
        }
    }

    private void SendUserToMainActivity(String encKey,String encID)
    {
        Intent mainIntent= new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.putExtra("key_encKey",encKey);
        mainIntent.putExtra("key_encID",encID);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToRegisterActivity()
    {
        Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);
    }
}