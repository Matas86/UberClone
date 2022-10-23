package com.simcoder.uber;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText nameEditTextField, phoneEditTextField;

    private Button backButton, confirmButton;

    private ImageView profileImageView;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference customerDatabase;

    private String userID;
    private String userName;
    private String userPhone;
    private String userImageViewUrl;
    private Uri resultUri;
    private static final int correct_request_code = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        nameEditTextField = (EditText) findViewById(R.id.name);
        phoneEditTextField = (EditText) findViewById(R.id.phone);

        profileImageView = (ImageView) findViewById(R.id.profileImage);

        backButton = (Button) findViewById(R.id.back);
        confirmButton = (Button) findViewById(R.id.confirm);

        firebaseAuth = FirebaseAuth.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();
        customerDatabase = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userID);

        initDatabaseListeners();

        profileImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        confirmButton.setOnClickListener(view -> {
            saveUserInformation();
        });

        backButton.setOnClickListener(view -> {
            finish();
        });
    }

    private void initDatabaseListeners() {
        customerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> valueByKey = (Map<String, Object>) dataSnapshot.getValue();
                    if (valueByKey.get("name") != null) {
                        userName = valueByKey.get("name").toString();
                        nameEditTextField.setText(userName);
                    }
                    if (valueByKey.get("phone") != null) {
                        userPhone = valueByKey.get("phone").toString();
                        phoneEditTextField.setText(userPhone);
                    }
                    if (valueByKey.get("profileImageUrl") != null) {
                        userImageViewUrl = valueByKey.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(userImageViewUrl).into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void saveUserInformation() {
        userName = nameEditTextField.getText().toString();
        userPhone = phoneEditTextField.getText().toString();

        Map userInfo = Map.of(
                "name", userName,
                "phone", userPhone);
        customerDatabase.updateChildren(userInfo);

        if (resultUri == null) {
            finish();
            return;
        }
        StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images")
                .child(userID);
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = filePath.putBytes(data);

        uploadTask.addOnFailureListener(view -> {
            finish();
        });
        uploadTask.addOnSuccessListener(view -> {
            Uri downloadUrl = taskSnapshot.getDownloadUrl();

            Map newImage = Map.of(
                    "profileImageUrl", downloadUrl.toString());
            customerDatabase.updateChildren(newImage);

            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == correct_request_code && resultCode == Activity.RESULT_OK) {
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            profileImageView.setImageURI(resultUri);
        }
    }
}
