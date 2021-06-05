package com.example.dsceconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyPostsActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView myPostsList;

    private DatabaseReference PostsRef,UsersRef,LikesRef;

    private String EncKey,EncID;

    myDbAdapter helper;
    Boolean LikeChecker=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        mToolbar = (Toolbar) findViewById(R.id.my_posts_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Posts");

        myPostsList=(RecyclerView)findViewById(R.id.my_all_posts_list);
        myPostsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myPostsList.setLayoutManager(linearLayoutManager);

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

        UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef= FirebaseDatabase.getInstance().getReference().child("Posts");
        LikesRef= FirebaseDatabase.getInstance().getReference().child("Likes");

        DisplayMyAllPosts();
    }

    private void DisplayMyAllPosts()
    {
        Query query= PostsRef.orderByChild("uid").startAt(EncID).endAt(EncID+"\uf8ff");
        FirebaseRecyclerOptions<Posts> options =
                new FirebaseRecyclerOptions.Builder<Posts>()
                        .setQuery(query, Posts.class)
                        .build();

        FirebaseRecyclerAdapter<Posts, MyPostsViewHolder> firebaseRecyclerAdapter=new FirebaseRecyclerAdapter<Posts, MyPostsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyPostsViewHolder holder, int position, @NonNull Posts model)
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
                        Intent clickPostIntent=new Intent(MyPostsActivity.this,ClickPostActivity.class);
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
                        Intent commentsIntent=new Intent(MyPostsActivity.this,CommentsActivity.class);
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
            public MyPostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.all_posts_layout,parent,false);
                return new MyPostsViewHolder(view);
            }
        };
        firebaseRecyclerAdapter.startListening();
        myPostsList.setAdapter(firebaseRecyclerAdapter);
    }

    public class MyPostsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        ImageButton LikePostButton,CommentPostButton;
        TextView DisplayNoOfLike;
        int countLikes;
        String EncID;
        DatabaseReference LikesRef;
        myDbAdapter helper2;

        public MyPostsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;

            LikePostButton = (ImageButton) mView.findViewById(R.id.like_button);
            CommentPostButton = (ImageButton) mView.findViewById(R.id.comment_button);
            DisplayNoOfLike = (TextView) mView.findViewById(R.id.display_no_of_likes);

            LikesRef=FirebaseDatabase.getInstance().getReference().child("Likes");
            String CurID= FirebaseAuth.getInstance().getCurrentUser().getUid();
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
}