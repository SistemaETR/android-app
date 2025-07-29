package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.pojos.Training;
import dev.abzikel.sistemaetr.pojos.User;

public class FirebaseManager {
    // Singleton instance of FirebaseManager
    private static FirebaseManager instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;

    // Firebase listeners and data
    private ListenerRegistration userListenerRegistration;
    private User currentUserData;

    private FirebaseManager() {
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
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

}
