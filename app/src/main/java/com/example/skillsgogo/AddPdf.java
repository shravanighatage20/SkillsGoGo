package com.example.skillsgogo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AddPdf extends AppCompatActivity {
    private Uri fileUri;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pdf);
        Intent i = getIntent();
        String id = i.getStringExtra("id");

        Button b = findViewById(R.id.removepdf);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent n = new Intent(AddPdf.this,RemovePdf.class);
                n.putExtra("id",id);
                startActivity(n);
                finish();
            }
        });

        FirebaseStorage.getInstance();

        Button selectButton = findViewById(R.id.select);
        Button uploadButton = findViewById(R.id.upload);
        //Button seeFilesButton = findViewById(R.id.see);

        TextView textView = findViewById(R.id.text);

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(AddPdf.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(AddPdf.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 9);
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fileUri != null) {
                    uploadFileToFirestore(fileUri);
                } else {
                    Toast.makeText(AddPdf.this, "Select a file", Toast.LENGTH_SHORT).show();
                }
                Intent i = new Intent(AddPdf.this,UpdateCourse.class);
                startActivity(i);
                finish();
            }
        });
    }

    private void uploadFileToFirestore(Uri fileUri) {
        String filename = System.currentTimeMillis() + "";

        // Create a reference to the file in Firestore Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("files/" + filename);

        // Upload the file to Firestore Storage
        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL of the uploaded file
                storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        String fileUrl = downloadUri.toString();
                        Map<String, Object> fileData = new HashMap<>();
                        fileData.put("name", getFileName(fileUri));
                        fileData.put("fileUrl", fileUrl);
                        Intent i = getIntent();
                        String id = i.getStringExtra("id");

                        // Upload the document to Firestore
                        db.collection("skillsgogo/skillsgogo_doc/course").document(id).collection("file")
                                .add(fileData)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(AddPdf.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(AddPdf.this, "File upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AddPdf.this, "File upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, 10);
    }

    private String getFileName(Uri uri) {
        String displayName = "";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } catch (Exception e) {
            Log.e("AddPdf", "Error getting file name: " + e.getMessage());
        }
        return displayName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                fileUri = data.getData();
                TextView textView = findViewById(R.id.text);
                textView.setText("Selected File: " + getFileName(fileUri));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 9) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(this, "Permission denied. Unable to select file.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
