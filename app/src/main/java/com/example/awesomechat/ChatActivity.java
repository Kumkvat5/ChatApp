package com.example.awesomechat;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.LocaleListCompat;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    int maxValue;
    int progressValue;


    private FirebaseAuth auth;
    private static final int RC_IMAGE_PICKER = 123;
    private ListView messageListView;
    private AwesomeMessageAdapter adapter;
    private ProgressBar progressBar;
    private ImageButton sendImageButton;
    private Button sendMessageButton;
    private EditText messageEditText;
    private String userName;
    private String recipientUserId;
    private FirebaseDatabase database;
    private DatabaseReference messagesDatabaseReference;
    private ChildEventListener messagesChildEventListener;
    private DatabaseReference usersDatabaseReference;
    private ChildEventListener usersChildEventListener;

    private SharedPreferences sharedPreferences;
    private FirebaseStorage storage;
    private StorageReference chatImagesStorageReference;
    private String recipientUserName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        auth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("AwesomeChatPrefs", MODE_PRIVATE);
        userName = sharedPreferences.getString("userName", "Default User");

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("userName") && intent.hasExtra("recipientUserId")) {
            userName = intent.getStringExtra("userName");
            recipientUserId = intent.getStringExtra("recipientUserId");
            recipientUserName = intent.getStringExtra("recipientUserName");
            sharedPreferences.edit().putString("userName", userName).apply();
        }
        setTitle("Chat with " + recipientUserName);
        storage = FirebaseStorage.getInstance();
        chatImagesStorageReference = storage.getReference().child("chat_images");

        // Firebase setup
        database = FirebaseDatabase.getInstance();
        messagesDatabaseReference = database.getReference().child("messages");
        usersDatabaseReference = database.getReference().child("users");

        // UI components
        progressBar = findViewById(R.id.progressBar);
        sendImageButton = findViewById(R.id.sendPhotoButton);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        messageEditText = findViewById(R.id.messageEditText);

        messageListView = findViewById(R.id.messageListView);
        List<AwesomeMessage> awesomeMessages = new ArrayList<>();
        adapter = new AwesomeMessageAdapter(this, R.layout.message_item, awesomeMessages);
        messageListView.setAdapter(adapter);

        progressBar.setVisibility(ProgressBar.INVISIBLE);


        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendMessageButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        messageEditText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(500)
        });

        sendMessageButton.setOnClickListener(v -> {
            AwesomeMessage message = new AwesomeMessage();
            message.setText(messageEditText.getText().toString());
            message.setName(userName);
            message.setSender(auth.getCurrentUser().getUid());
            message.setRecipient(recipientUserId);
            message.setImageUrl(null);
            messagesDatabaseReference.push().setValue(message);

            messageEditText.setText("");
        });

        sendImageButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
            intent1.setType("image/*");
            intent1.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(intent1, "Choose an image"), RC_IMAGE_PICKER);
        });

        setupFirebaseListeners();
    }


    private void setupFirebaseListeners() {

        usersChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    userName = user.getName();
                    sharedPreferences.edit().putString("userName", userName).apply(); // Save to SharedPreferences
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
        };
        usersDatabaseReference.addChildEventListener(usersChildEventListener);

        showProgressBar();
        messagesChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                AwesomeMessage message = snapshot.getValue(AwesomeMessage.class);

                if (message.getSender().equals(auth.getCurrentUser().getUid())
                        && message.getRecipient().equals(recipientUserId)) {
                    message.setMine(true);
                    adapter.add(message);
                } else if (message.getRecipient().equals(auth.getCurrentUser().getUid())
                        && message.getSender().equals(recipientUserId)) {
                    message.setMine(false);
                    adapter.add(message);
                }
                messageListView.setSelection(adapter.getCount() - 1);
                hideProgressBar();
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
        };
        messagesDatabaseReference.addChildEventListener(messagesChildEventListener);
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
            sharedPreferences.edit().remove("userName").apply(); // Clear userName on sign-out
            startActivity(new Intent(ChatActivity.this, SignInActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_IMAGE_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                final StorageReference imageReference = chatImagesStorageReference.child(selectedImageUri.getLastPathSegment());

                UploadTask uploadTask = imageReference.putFile(selectedImageUri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        return imageReference.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            AwesomeMessage message = new AwesomeMessage();
                            message.setName(userName);
                            message.setImageUrl(downloadUri.toString());
                            message.setSender(auth.getCurrentUser().getUid());
                            message.setRecipient(recipientUserId);

                            messagesDatabaseReference.push().setValue(message);
                        }
                    }
                });
            }
        }
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }
}

