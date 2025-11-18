package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ModelPreprocessor {
    private static final String TAG = "ModelPreprocessor";
    private JSONObject scalerParams;
    private JSONArray featureColumns;
    private JSONObject labelMapping;

    public ModelPreprocessor(Context context) {
        // Load JSON files from assets
        try {
            scalerParams = new JSONObject(loadJSONFromAsset(context, "scaler_params.json"));
            featureColumns = new JSONArray(loadJSONFromAsset(context, "feature_columns.json"));
            labelMapping = new JSONObject(loadJSONFromAsset(context, "deficiency_labels.json"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to load JSON assets", e);
        }
    }

    private String loadJSONFromAsset(Context context, String fileName) throws IOException {
        String json = null;
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        json = new String(buffer, StandardCharsets.UTF_8);
        return json;
    }

    public float[] preprocessInput(Map<String, Float> rawInput) {
        if (scalerParams == null || featureColumns == null) {
            Log.e(TAG, "JSON data not loaded.");
            return null;
        }

        try {
            float[] processedInput = new float[featureColumns.length()];

            // Load scaler parameters
            Map<String, float[]> scalingMap = new HashMap<>();
            JSONArray featuresToScale = scalerParams.getJSONArray("features");
            JSONArray means = scalerParams.getJSONArray("mean");
            JSONArray scales = scalerParams.getJSONArray("scale");

            for (int i = 0; i < featuresToScale.length(); i++) {
                String featureName = featuresToScale.getString(i);
                float mean = (float) means.getDouble(i);
                float scale = (float) scales.getDouble(i);
                scalingMap.put(featureName, new float[]{mean, scale});
            }

            // Get the modality for one-hot encoding
            int modality = rawInput.get("modality").intValue();
            float[] oneHotModality = new float[5];
            if (modality >= 1 && modality <= 5) {
                oneHotModality[modality - 1] = 1.0f;
            }

            // Build the processed input
            for (int i = 0; i < featureColumns.length(); i++) {
                String colName = featureColumns.getString(i);

                if (colName.startsWith("modality_")) {
                    int modalityIndex = Integer.parseInt(colName.substring(colName.length() - 1)) - 1;
                    processedInput[i] = oneHotModality[modalityIndex];

                } else if (scalingMap.containsKey(colName)) {
                    float rawValue = rawInput.get(colName);
                    float[] params = scalingMap.get(colName);
                    float mean = params[0];
                    float scale = params[1];

                    // Apply scaling
                    processedInput[i] = (rawValue - mean) / scale;

                }
            }
            return processedInput;

        } catch (Exception e) {
            Log.e(TAG, "Error during preprocessing", e);
            return null;
        }
    }

    public int postprocessOutputToId(float[] probabilities) {
        if (labelMapping == null) return -1;

        int maxIndex = -1;
        float maxProb = -1.0f;

        // Find the index with the highest probability
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}