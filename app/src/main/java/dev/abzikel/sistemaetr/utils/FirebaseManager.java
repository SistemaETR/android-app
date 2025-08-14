package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.pojos.Training;
import dev.abzikel.sistemaetr.pojos.User;

public class FirebaseManager {
    // Singleton instance of FirebaseManager
    private static FirebaseManager instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private final FirebaseStorage mStorage;

    // Firebase listeners and data
    private ListenerRegistration userListenerRegistration;
    private User currentUserData;

    private FirebaseManager() {
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        // Create instance if it doesn't exist
        if (instance == null) instance = new FirebaseManager();

        return instance;
    }

    public interface OnUserDataChangedListener {
        void onDataChanged(User user);

        void onError(Exception e);
    }

    public void startListeningForUserChanges(Context context, OnUserDataChangedListener listener) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            listener.onError(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        String uid = currentUser.getUid();
        userListenerRegistration = mDb.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    // Handle changes in user data
                    if (e != null) {
                        // Handle errors
                        Log.w("FirebaseManager", context.getString(R.string.error_listening_changes), e);
                        listener.onError(e);
                        return;
                    }

                    // Handle changes in user data
                    if (snapshot != null && snapshot.exists()) {
                        // Update currentUserData with new data
                        currentUserData = snapshot.toObject(User.class);

                        // Notify listener of changes
                        if (currentUserData != null) listener.onDataChanged(currentUserData);
                    } else {
                        // Handle missing user data
                        Log.d("FirebaseManager", context.getString(R.string.user_document_not_found));
                    }
                });
    }

    public void stopListening() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
            currentUserData = null;
        }
    }

    public User getCurrentUserData() {
        return currentUserData;
    }

    public interface OnUserUpdateListener {
        void onSuccess();

        void onFailure(Exception e);
    }

    public interface OnSaveTrainingListener {
        // Callback methods for saving training
        void onSuccess();

        void onFailure(Exception e);
    }

    public void saveTraining(Context context, Training training, OnSaveTrainingListener listener) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            listener.onFailure(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        // Save training to Firestore
        String uid = currentUser.getUid();
        mDb.collection("users").document(uid)
                .collection("trainings").document(training.getTrainingId())
                .set(training)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnTrainingsFetchedListener {
        void onSuccess(List<Training> trainings, DocumentSnapshot lastVisible);

        void onFailure(Exception e);
    }

    public void fetchTrainings(Context context, int modality, @Nullable Date startDate, @Nullable Date endDate,
                               @Nullable DocumentSnapshot lastVisible, OnTrainingsFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        // Initiate the Firestore query order by date, from newest to oldest
        Query query = mDb.collection("users").document(currentUser.getUid())
                .collection("trainings")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        // Apply filters if provided
        if (modality > 0) query = query.whereEqualTo("modality", modality);
        if (startDate != null) query = query.whereGreaterThanOrEqualTo("createdAt", startDate);
        if (endDate != null) query = query.whereLessThanOrEqualTo("createdAt", endDate);

        // Apply pagination if cursor is provided
        if (lastVisible != null) query = query.startAfter(lastVisible);

        // Limit number of results to 15
        query = query.limit(15);

        // Execute the query
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                // No more data to fetch or the list is empty
                listener.onSuccess(new ArrayList<>(), null);
                return;
            }

            // Convert the documents to a list of Training objects
            List<Training> trainings = queryDocumentSnapshots.toObjects(Training.class);

            // Get the last visible document in the query result for the next cursor
            DocumentSnapshot newLastVisible = queryDocumentSnapshots.getDocuments()
                    .get(queryDocumentSnapshots.size() - 1);

            listener.onSuccess(trainings, newLastVisible);
        }).addOnFailureListener(e -> {
            Log.e("FirebaseManager", context.getString(R.string.error_getting_trainings), e);
            listener.onFailure(e);
        });
    }

    public boolean isPasswordProviderEnabled() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                // Verify that the provider is password
                if (profile.getProviderId().equals("password")) return true;
            }
        }
        return false;
    }

    public interface OnImageUploadListener {
        void onSuccess(Uri downloadUri);

        void onFailure(Exception e);
    }

    public void uploadProfileImage(Context context, Uri imageUri, OnImageUploadListener listener) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            listener.onFailure(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        // Path will be profile_images/<user_id>.jpg
        StorageReference profileImageRef = mStorage.getReference()
                .child("profile_images/" + currentUser.getUid() + ".jpg");

        // Upload the image
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl()
                        .addOnSuccessListener(listener::onSuccess)
                        .addOnFailureListener(listener::onFailure))
                .addOnFailureListener(listener::onFailure);
    }

    public void deleteProfileImage(Context context, OnSimpleListener listener) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            listener.onFailure(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        // Path will be profile_images/<user_id>.jpg
        StorageReference profileImageRef = mStorage.getReference()
                .child("profile_images/" + currentUser.getUid() + ".jpg");

        // Delete the image from Firebase Storage
        profileImageRef.delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void updateUserProfile(Context context, Map<String, Object> updates, OnSimpleListener listener) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            listener.onFailure(new Exception(context.getString(R.string.no_authenticated_user)));
            return;
        }

        // Update user data in Firestore
        mDb.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public interface OnSimpleListener {
        void onSuccess();

        void onFailure(Exception e);
    }

}
