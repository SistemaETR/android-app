package dev.abzikel.sistemaetr.utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.pojos.Training;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;

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
