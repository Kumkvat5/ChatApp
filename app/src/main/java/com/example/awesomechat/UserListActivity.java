package com.example.awesomechat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class UserListActivity extends AppCompatActivity {
    private static final int RC_IMAGE_PICKER = 1234;
    private String userName;
    private DatabaseReference usersDataBaseReference;
    private ChildEventListener usersChildEventListener;
    private ArrayList<User> userArrayList;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private RecyclerView.LayoutManager userLayoutManager;
    private FirebaseAuth auth;
    private StorageReference avatarStorageReference;
    private FirebaseStorage storage;
    ImageView avatarImageView;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        if (intent != null) {
            userName = intent.getStringExtra(userName);
        }
        userArrayList = new ArrayList<>();
        attachUserDataBaseReferenceListener();
        buildRecyclerView();
        auth = FirebaseAuth.getInstance();

        avatarImageView = findViewById(R.id.avatarImageView);
        storage = FirebaseStorage.getInstance();
        avatarStorageReference = storage.getReference().child("avatars");
    }

    private void attachUserDataBaseReferenceListener() {


        usersDataBaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        if (usersChildEventListener == null) {
            usersChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    user = snapshot.getValue(User.class);
                    if (!user.getId().equals(auth.getCurrentUser().getUid())) {
                        user.setAvatarMocUpResource(R.drawable.baseline_person_24);
                        userArrayList.add(user);
                        userAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    user = snapshot.getValue(User.class);
                    if (user != null) {
                        for (int i = 0; i < userArrayList.size(); i++) {
                            User currentUser = userArrayList.get(i);
                            if (currentUser.getId().equals(user.getId())) {

                                currentUser.setAvatarUrl(user.getAvatarUrl());

                                userAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
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
            };
            usersDataBaseReference.addChildEventListener(usersChildEventListener);
        }
    }

    private void buildRecyclerView() {
        userRecyclerView = findViewById(R.id.userListRecyclerView);
        userRecyclerView.setHasFixedSize(true);

        userLayoutManager = new LinearLayoutManager(this);
        userAdapter = new UserAdapter(userArrayList);

        userRecyclerView.setLayoutManager(userLayoutManager);
        userRecyclerView.setAdapter(userAdapter);

        userAdapter.setOnUserClickListener(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(int position) {
                goToChat(position);
            }
        });
    }

    private void goToChat(int position) {
        Intent intent = new Intent(UserListActivity.this, ChatActivity.class);
        intent.putExtra("recipientUserId", userArrayList.get(position).getId());
        intent.putExtra("recipientUserName", userArrayList.get(position).getName());
        intent.putExtra("userName", userName);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sighOut) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(UserListActivity.this, SignInActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.addProfilePhoto) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(intent, "Choose an image"), RC_IMAGE_PICKER);

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_IMAGE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri selectedAvatarUri = data.getData();
            if (selectedAvatarUri != null) {
                final StorageReference avatarReference = avatarStorageReference.child(selectedAvatarUri.getLastPathSegment());

                avatarReference.putFile(selectedAvatarUri)
                        .continueWithTask(task -> {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return avatarReference.getDownloadUrl();
                        })
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Uri downloadUri = task.getResult();
                                String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                usersDataBaseReference.orderByChild("id").equalTo(currentUserUid)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                                    userSnapshot.getRef().child("avatarUrl").setValue(downloadUri.toString())
                                                            .addOnSuccessListener(aVoid -> Log.d("AvatarUpdate", "Avatar URL updated successfully."))
                                                            .addOnFailureListener(e -> Log.e("AvatarUpdate", "Failed to update avatar URL.", e));
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("FirebaseDatabase", "Query cancelled or failed.", error.toException());
                                            }
                                        });
                            } else {
                                Log.e("FirebaseStorage", "Failed to get download URI", task.getException());
                            }
                        });
            }
        }
    }


}