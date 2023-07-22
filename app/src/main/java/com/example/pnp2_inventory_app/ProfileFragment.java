package com.example.pnp2_inventory_app;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.annotation.SuppressLint;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    TextInputEditText editTextFirstName, editTextLastName;
    private ProgressBar progressBar;
    public Uri imageUri;
    public SwitchCompat nightModeSwitch;
    private boolean isNightModeEnabled = false; // Track the current night mode state
    private AlertDialog dialog;
    private TextView nameTextView;
    private ImageView profileImage;
    private ImageView changeprofpic;
    private FirebaseUser currentUser;
    private StorageReference storageReference;
    // Constants for image picker
    private static final int PICK_IMAGE_REQUEST = 1;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        // Initialize Firebase Authentication
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        //calls fb storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize views
        nightModeSwitch = view.findViewById(R.id.nightmodebtn);
        ImageView arrowFamily = view.findViewById(R.id.arrowFamilybtn);
        SwitchCompat notifswitch = view.findViewById(R.id.notifswitchbtn);
        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        TextView emailTextView = view.findViewById(R.id.email);
        nameTextView  = view.findViewById(R.id.nameofuser);
        profileImage = view.findViewById(R.id.profileImage);

        String name = currentUser.getDisplayName();
        nameTextView.setText(name);
        String email = currentUser.getEmail();
        emailTextView.setText(email);

        // Check if permission is granted to access the camera roll
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
        }

//        //TODO- need to implement VIEW IMAGE instead of choosing a new photo
//        //this can also just be left alone(no need to view full image)
//        profileImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Launch the image picker
//                openImagePicker();
//            }
//        });

        //loads profile image after upload
        loadUserProfileImage();

        // Set click listeners
        arrowFamily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ShowFamilyView();
            }
        });

        //handles notification switch
        notifswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handle nightModeSwitch state change
                if (isChecked) {
                    // Notification is enabled
                    Toast.makeText(getContext(), "Notifications on", Toast.LENGTH_SHORT).show();
                }
                else {
                    // Notification is disabled
                    Toast.makeText(getContext(), "Notifications off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle editProfileButton click event
                openEditProfileActivity();
            }
        });

        // Initialize night mode switch
        nightModeSwitch.setChecked(isNightModeEnabled);
        nightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handle night mode switch state change
                if (isChecked) {
                    // Night mode is enabled
                    enableNightMode();
                } else {
                    // Night mode is disabled
                    disableNightMode();
                }
            }
        });

        return view;
    }

    // Method to open the image picker
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK && data != null && data.getData() != null){
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadPicture();
        }
    }

    //Method to upload photo to Firebase Storage
    private void uploadPicture() {

        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle("Uploading image");
        pd.show();


        final String randomKey = currentUser.getDisplayName() + UUID.randomUUID();
        // Create a reference to images
        StorageReference reference = storageReference.child("images/" + randomKey);

        reference.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        pd.dismiss();
                        Toast.makeText(getContext(), "Image uploaded", Toast.LENGTH_SHORT).show();

                        // Get the download URL of the uploaded image
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                // Store the download URL in Firebase Firestore
                                String imageUrl = downloadUri.toString();
                                saveImageUrlToDatabase(imageUrl);

                                Glide.with(requireContext())
                                        .load(downloadUri)
                                        .apply(new RequestOptions().transforms(new CircleCrop()).skipMemoryCache(true))
                                        .into(profileImage);

                                Glide.with(requireContext())
                                        .load(downloadUri)
                                        .apply(new RequestOptions().transforms(new CircleCrop()).skipMemoryCache(true))
                                        .into(changeprofpic);
                            }
                        });
                    }
                })
                //pretty straightforward
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(getContext(), "Image upload fail", Toast.LENGTH_SHORT).show();
                    }
                })
                //Adds a pop-up loading screen when uploading the image
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        pd.setMessage("Uploading" + (int) progressPercent + "%");
                    }
                });
    }

    private void saveImageUrlToDatabase(String imageUrl) {
        // Get the current user's UID
        String uid = currentUser.getUid();

        // Access your Firebase Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a reference to the user's document in Firestore
        DocumentReference userRef = db.collection("users").document(uid);

        // Update the 'profileImageUrl' field with the image URL
        userRef.update("profileImageUrl", imageUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // The image URL is successfully saved in the database
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the failure to save the image URL
                    }
                });
    }

    private void loadUserProfileImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Access your Firebase Firestore instance
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Creates a reference to the user's document in Firestore
            DocumentReference userRef = db.collection("users").document(uid);

            // Get the user's document from Firestore
            userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        // User document exists, retrieve the profile image URL from the document
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Load the profile image into the ImageView using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Use Glide's CircleCrop transformation directly and skip cache
                            RequestOptions requestOptions = new RequestOptions()
                                    .transforms(new CircleCrop())
                                    .skipMemoryCache(true);

                            Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(profileImage);
                        }
                    } else {
                        // User document doesn't exist, create it with the UID and other user-specific data
                        String name = currentUser.getDisplayName();
                        String email = currentUser.getEmail();

                        //Created Map object to input data(specific user)
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);

                        userRef.set(userData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // User documentID is created
                                        // Load the profile image again after creating the user documentID
                                        loadUserProfileImage();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle the failure to create the user document
                                    }
                                });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle the failure to retrieve the user's document
                }
            });
        }
    }

    //method to enable night mode
    private void enableNightMode() {
        if (!isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            isNightModeEnabled = true;
        }
    }

    //method to disable night mode
    private void disableNightMode() {
        if (isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            isNightModeEnabled = false;
        }
    }

    private void ShowFamilyView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_family_view, null);
        builder.setView(dialogView);

        RecyclerView familyView = dialogView.findViewById(R.id.familyView);
        Button addFamilyMem = dialogView.findViewById(R.id.addfamilymem);

        addFamilyMem.setOnClickListener(v -> {

            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });

        dialog = builder.create();
        dialog.show();
    }

    //method to open edit profile fragment to edit current users info
    private void openEditProfileActivity(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_edit_profile_view, null);
        builder.setView(dialogView);

        changeprofpic = dialogView.findViewById(R.id.profileImage);
        editTextFirstName = dialogView.findViewById(R.id.firstname);
        editTextLastName = dialogView.findViewById(R.id.lastname);
        progressBar = dialogView.findViewById(R.id.progressBarz);
        Button continueBtn = dialogView.findViewById(R.id.continuebtneditprof);

        // Load the current profile image in the edit profile dialog using Glide
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(uid);

            userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        // User document exists, retrieve the profile image URL from the document
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Load the profile image into the ImageView using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Use Glide's CircleCrop transformation directly and skip cache
                            RequestOptions requestOptions = new RequestOptions()
                                    .transforms(new CircleCrop())
                                    .skipMemoryCache(true);

                            Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(changeprofpic);
                        }
                    }
                }
            });
        }
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //makes progress bar visible after clicking register button
                progressBar.setVisibility(View.VISIBLE);
                String firstname, lastname;
                firstname = String.valueOf(editTextFirstName.getText());
                lastname = String.valueOf(editTextLastName.getText());

                //edits the profile and inputs the data to firebase

                UserProfileChangeRequest profileName = new UserProfileChangeRequest.Builder()
                        .setDisplayName(firstname + " " + lastname)
                        .build();

                //updates the profile
                currentUser.updateProfile(profileName)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Log.e("HomeStock", "Profile edited successfully");
                                    String updatedName = firstname + " " + lastname;
                                    nameTextView.setText(updatedName);
                                    nameTextView.setGravity(Gravity.CENTER);
                                }
                            }
                        });

                dialog.dismiss();
            }
        });

        //on click listener for changeprofpic button to open the image picker
        changeprofpic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.cancel();
        });

        dialog = builder.create();
        dialog.show();
    }
}