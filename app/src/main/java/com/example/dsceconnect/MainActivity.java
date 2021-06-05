package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.ToolbarWidgetWrapper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RecyclerView postList;

    private Toolbar mToolbar;
    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef,PostsRef,LikesRef;
    private String EncKey,EncID;
    private ImageButton AddNewPostButton;
    String currentUserID;

    private CircleImageView NavProfileImage;
    private TextView NavProfileUserName;
    static myDbAdapter helper;

    Boolean LikeChecker=false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SQLLITE
        helper = new myDbAdapter(this);
        if(helper.getRowCount()==1)
        {
            String finalString=helper.getEncData();
            String[] encparam=finalString.split(" ");
            EncKey=encparam[0];
            EncID=encparam[1];
            mAuth=FirebaseAuth.getInstance();
            FirebaseUser user=mAuth.getCurrentUser();
            currentUserID=user.getUid();

            if(user!=null)
            {
                Log.i("user",currentUserID);
                try {
                    String enckey2=EncModel.generateEncryptionKey(currentUserID);
                    if(!EncKey.equals(enckey2))
                    {
                        helper.deleteAll();
                        SendUserToLoginActivity();
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace(); }
            }
            else
            {
                SendUserToLoginActivity();
            }
        }
        else
        {
            helper.deleteAll();
            SendUserToLoginActivity();
            finish();
        }

        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef= FirebaseDatabase.getInstance().getReference().child("Posts");
        LikesRef= FirebaseDatabase.getInstance().getReference().child("Likes");

        Log.i("enc",EncKey+"  "+EncID);
        mToolbar=(Toolbar)findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");

        AddNewPostButton=(ImageButton)findViewById(R.id.add_new_post_button);

        drawerLayout= (DrawerLayout) findViewById(R.id.drawable_layout);
        actionBarDrawerToggle=new ActionBarDrawerToggle(MainActivity.this, drawerLayout, R.string.drawer_open,R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView= (NavigationView)findViewById(R.id.navigation_view);


        postList=(RecyclerView)findViewById(R.id.all_users_post_list);
        postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);


        View navView = navigationView.inflateHeaderView(R.layout.navigation_header);
        NavProfileImage=(CircleImageView)navView.findViewById(R.id.nav_profile_image);
        NavProfileUserName=(TextView)navView.findViewById(R.id.nav_user_full_name);




        UsersRef.child(EncID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if(snapshot.exists())
                {
                    if(snapshot.hasChild("fullname"))
                    {
                        String fullname=snapshot.child("fullname").getValue().toString();
                        NavProfileUserName.setText(fullname);
                    }

                    if(snapshot.hasChild("profileimage"))
                    {
                        String image=snapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(NavProfileImage);
                    }

                    else
                    {
                        Toast.makeText(MainActivity.this,"Profile name/image does not exist",Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                UserMenuSelector(item);
                return false;
            }
        });

        AddNewPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserTOPostActivity(EncKey,EncID);
            }
        });

        DisplayAllUsersPosts();

    }


    private void DisplayAllUsersPosts()
    {


        Query query= PostsRef.orderByChild("timestamp");
        FirebaseRecyclerOptions<Posts> options =
                new FirebaseRecyclerOptions.Builder<Posts>()
                        .setQuery(query, Posts.class)
                        .build();

        FirebaseRecyclerAdapter<Posts,PostsViewHolder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<Posts, PostsViewHolder>
                (options)
        {
            @Override
            protected void onBindViewHolder(@NonNull PostsViewHolder holder, int position, @NonNull Posts model)
            {
                final String PostKey=getRef(position).getKey();

                holder.setFullname(model.getFullname());
                holder.setTime(model.getTime());
                holder.setDate(model.getDate());
                holder.setDescription(model.getDescription());
                holder.setProfileimage(getApplicationContext(),model.getProfileimage());
                holder.setPostimage(getApplicationContext(),model.getPostimage());

                holder.setLikeButtonStatus(PostKey);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent clickPostIntent=new Intent(MainActivity.this,ClickPostActivity.class);
                        clickPostIntent.putExtra("PostKey",PostKey);
                        clickPostIntent.putExtra("key_encKey",EncKey);
                        clickPostIntent.putExtra("key_encID",EncID);
                        startActivity(clickPostIntent);
                    }
                });

                holder.CommentPostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent commentsIntent=new Intent(MainActivity.this,CommentsActivity.class);
                        commentsIntent.putExtra("PostKey",PostKey);
                        commentsIntent.putExtra("key_encKey",EncKey);
                        commentsIntent.putExtra("key_encID",EncID);
                        startActivity(commentsIntent);
                    }
                });

                holder.LikePostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        LikeChecker=true;
                        LikesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot)
                            {
                                if(LikeChecker.equals(true))
                                {
                                    if(snapshot.child(PostKey).hasChild(EncID))
                                    {
                                        LikesRef.child(PostKey).child(EncID).removeValue();
                                        LikeChecker=false;
                                    }
                                    else
                                    {
                                        LikesRef.child(PostKey).child(EncID).setValue(true);
                                        LikeChecker=false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });
            }

            @NonNull
            @Override
            public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.all_posts_layout,parent,false);
                return new PostsViewHolder(view);
            }
        };
        firebaseRecyclerAdapter.startListening();
        postList.setAdapter(firebaseRecyclerAdapter);

    }

    public class PostsViewHolder extends RecyclerView.ViewHolder
    {

        View mView;

        ImageButton LikePostButton,CommentPostButton;
        TextView DisplayNoOfLike;
        int countLikes;
        String EncID;
        DatabaseReference LikesRef;
        myDbAdapter helper2;

        public PostsViewHolder(@NonNull View itemView)
        {
            super(itemView);
            mView=itemView;

            LikePostButton = (ImageButton) mView.findViewById(R.id.like_button);
            CommentPostButton = (ImageButton) mView.findViewById(R.id.comment_button);
            DisplayNoOfLike = (TextView) mView.findViewById(R.id.display_no_of_likes);

            LikesRef=FirebaseDatabase.getInstance().getReference().child("Likes");
            String CurID=FirebaseAuth.getInstance().getCurrentUser().getUid();
            helper2= new myDbAdapter(getApplicationContext());

            try {
                String finalString=helper2.getEncData();
                String[] encparam=finalString.split(" ");
                //EncKey=encparam[0];
                EncID=encparam[1];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setLikeButtonStatus(final String PostKey)
        {
            LikesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot)
                {
                    if(snapshot.child(PostKey).hasChild(EncID))
                    {
                        countLikes=(int)snapshot.child(PostKey).getChildrenCount();
                        LikePostButton.setImageResource(R.drawable.like);
                        DisplayNoOfLike.setText(Integer.toString(countLikes));
                    }
                    else
                    {
                        countLikes=(int)snapshot.child(PostKey).getChildrenCount();
                        LikePostButton.setImageResource(R.drawable.dislike);
                        DisplayNoOfLike.setText(Integer.toString(countLikes));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        public void setFullname(String fullname)
        {
            TextView username=(TextView)mView.findViewById(R.id.post_user_name);
            username.setText(fullname);
        }

        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView image=(CircleImageView)mView.findViewById(R.id.post_profile_image);
            Picasso.get().load(profileimage).into(image);
        }

        public void setTime(String time)
        {
            TextView PostTime  =(TextView)mView.findViewById(R.id.post_time);
            PostTime.setText("   "+time);
        }

        public void setDate(String date)
        {
            TextView PostDate  =(TextView)mView.findViewById(R.id.post_date);
            PostDate.setText("   "+date);
        }

        public void setDescription(String description)
        {
            TextView PostDescription  =(TextView)mView.findViewById(R.id.post_description);
            PostDescription.setText(description);
        }

        public void setPostimage(Context ctx, String postimage)
        {
            ImageView PostImage=(ImageView)mView.findViewById(R.id.post_image);
            Log.i("post image",postimage);
            Picasso.get().load(postimage).into(PostImage);
        }
    }


    private void SendUserTOPostActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(MainActivity.this,PostActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if(helper.getRowCount()==1)
        {
            try {
                CheckUserExistence();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
            SendUserToLoginActivity();
            finish();
        }
    }

    private void CheckUserExistence()throws Exception
    {
        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.hasChild(EncID))
                {
                    File dir = getApplicationContext().getCacheDir();
                    boolean alpha=deleteDir(dir);
                    String alphas=""+alpha;
                    if(alpha)
                    {
                        //mAuth=FirebaseAuth.getInstance();
                        Log.i("userStat",alphas);
                        if(mAuth.getCurrentUser()==null)
                        {
                            SendUserToLoginActivity();
                            finish();
                        }
                        currentUserID=mAuth.getCurrentUser().getUid();

                        Log.i("userStatID",currentUserID);
                        if(currentUserID==null)
                        {

                            SendUserToLoginActivity();
                        }
                    }
                    SendUserToSetupActivity(EncKey,EncID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private boolean deleteDir(File dir)
    {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }


    private void SendUserToSetupActivity(String encKey, String encID)
    {

        Intent setupIntent = new Intent(MainActivity.this,SetupActivity.class);
        setupIntent.putExtra("key_encKey",encKey);
        setupIntent.putExtra("key_encID",encID);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }

    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void UserMenuSelector(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.nav_post:
                SendUserTOPostActivity(EncKey,EncID);
                break;

            case R.id.nav_profile:
                SendUserToProfileActivity(EncKey,EncID);
                break;

            case R.id.nav_friends:
                SendUserToFriendsActivity(EncKey,EncID);
                break;

            case R.id.nav_request:
                SendUserToRequestsActivity(EncKey,EncID);
                break;

            case R.id.nav_find_friends:
                SendUserToFindFriendsActivity(EncKey,EncID);
                break;

            case R.id.nav_messages:
                SendUserToMessagesActivity(EncKey,EncID);
                break;

            case R.id.nav_settings:
                SendUserToSettingsActivity(EncKey,EncID);
                break;

            case R.id.nav_logout:

                //updateUserStatus("offline");
                helper.deleteAll();
                mAuth= FirebaseAuth.getInstance();
                mAuth.signOut();
                SendUserToLoginActivity();
                break;
        }
    }

    private void SendUserToSettingsActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(MainActivity.this,SettingsActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    private void SendUserToProfileActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(MainActivity.this,ProfileActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    private void SendUserToFindFriendsActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(MainActivity.this,FindFriendsActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    private void SendUserToFriendsActivity(String encKey, String encID)
    {
        Intent addNewPostIntent= new Intent(MainActivity.this,FriendsActivity.class);
        addNewPostIntent.putExtra("key_encKey",encKey);
        addNewPostIntent.putExtra("key_encID",encID);
        startActivity(addNewPostIntent);
    }

    private void SendUserToRequestsActivity(String encKey, String encID)
    {
        Intent requestsIntent= new Intent(MainActivity.this,RequestsActivity.class);
        requestsIntent.putExtra("key_encKey",encKey);
        requestsIntent.putExtra("key_encID",encID);
        startActivity(requestsIntent);
    }

    private void SendUserToMessagesActivity(String encKey, String encID)
    {
        Intent messagesIntent= new Intent(MainActivity.this,MessagesActivity.class);
        messagesIntent.putExtra("key_encKey",encKey);
        messagesIntent.putExtra("key_encID",encID);
        startActivity(messagesIntent);
    }
}