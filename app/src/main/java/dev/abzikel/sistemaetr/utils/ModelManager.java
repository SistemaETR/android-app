package dev.abzikel.sistemaetr.utils;

import android.util.Log;

import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import org.tensorflow.lite.Interpreter;

import java.io.File;

public class ModelManager {
    private static ModelManager instance;
    private Interpreter tfliteInterpreter;
    private boolean isModelReady = false;
    private static final String MODEL_NAME = "etr-deficiency-model";
    private static final String TAG = "ModelManager";

    // Private constructor for Singleton
    private ModelManager() {
    }

    // Public method to get the single instance
    public static synchronized ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    // Interface for the callback
    public interface ModelReadyCallback {
        void onSuccess(Interpreter interpreter);

        void onFailure(Exception e);
    }

    // Loads the model asynchronously
    public void loadModel(ModelReadyCallback callback) {
        // If model is already loaded, return it immediately
        if (isModelReady && tfliteInterpreter != null) {
            // Added null check
            if (callback != null) callback.onSuccess(tfliteInterpreter);
            return;
        }

        // Set up model download conditions
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        // Download the model
        FirebaseModelDownloader.getInstance()
                .getModel(MODEL_NAME, DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
                .addOnSuccessListener(model -> {
                    File modelFile = model.getFile();
                    if (modelFile != null) {
                        tfliteInterpreter = new Interpreter(modelFile);
                        isModelReady = true;
                        Log.d(TAG, "TFLite interpreter initialized successfully.");
                        if (callback != null) callback.onSuccess(tfliteInterpreter);
                    } else {
                        Log.e(TAG, "Model file is null after download.");
                        if (callback != null) callback.onFailure(new Exception("Model file is null."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download model.", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

}