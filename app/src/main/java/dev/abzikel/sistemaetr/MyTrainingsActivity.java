package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import org.tensorflow.lite.Interpreter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import dev.abzikel.sistemaetr.adapters.TrainingAdapter;
import dev.abzikel.sistemaetr.pojos.Training;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;
import dev.abzikel.sistemaetr.utils.ModelManager;
import dev.abzikel.sistemaetr.utils.ModelPreprocessor;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class MyTrainingsActivity extends BaseActivity {
    private RecyclerView rvTrainings;
    private TrainingAdapter adapter;
    private FloatingActionButton fabAiAnalysis;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private TextInputEditText etDateFrom, etDateTo;
    private AutoCompleteTextView actvModality;

    private final List<Training> trainingList = new ArrayList<>();
    private DocumentSnapshot lastVisibleDocument;
    private Interpreter tfliteInterpreter;
    private ModelPreprocessor modelPreprocessor;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private Date dateFrom, dateTo;
    private final Random random = new Random();
    private String selectedModalityName = "";
    private int selectedModalityId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trainings);

        // Initialize views
        rvTrainings = findViewById(R.id.rvTrainings);
        fabAiAnalysis = findViewById(R.id.fabAiAnalysis);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        actvModality = findViewById(R.id.actvModality);

        // Initialize TensorFlow Lite interpreter
        modelPreprocessor = new ModelPreprocessor(this);

        // Initialize other components
        setupRecyclerView();
        setupPagination();
        setupFilters();

        // Set up click listener for FAB
        fabAiAnalysis.setOnClickListener(v -> onAnalyzePerformanceClicked());

        // Load first page of trainings
        loadTrainings(true);
    }

    private void setupRecyclerView() {
        adapter = new TrainingAdapter();
        rvTrainings.setLayoutManager(new LinearLayoutManager(this));
        rvTrainings.setAdapter(adapter);
    }

    private void setupFilters() {
        // Configure selector of start date
        etDateFrom.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                showDatePicker(true);
            }
        });

        // Configure selector of end date
        etDateTo.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                showDatePicker(false);
            }
        });

        // Configure selector of modality
        ArrayAdapter<CharSequence> modalityAdapter = ArrayAdapter.createFromResource(this,
                R.array.modality_array, android.R.layout.simple_spinner_dropdown_item);
        actvModality.setAdapter(modalityAdapter);
        actvModality.setOnItemClickListener((parent, view, position, id) -> {
            // Update the selected modality ID
            selectedModalityId = position;

            // Save the name of the selected modality
            selectedModalityName = (position == 0) ? "" : modalityAdapter.getItem(position).toString();

            // Show the FAB only if a modality is selected
            if (selectedModalityId > 0) fabAiAnalysis.setVisibility(View.VISIBLE);
            else fabAiAnalysis.setVisibility(View.GONE);

            // Load trainings with the new filter
            loadTrainings(true);
        });
    }

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartDate ? "Seleccionar fecha de inicio" : "Seleccionar fecha de fin")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert the selected date to a Date object
            TimeZone timeZoneUTC = TimeZone.getDefault();
            long offset = timeZoneUTC.getOffset(selection);
            Date date = new Date(selection + offset);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            if (isStartDate) {
                dateFrom = date;
                etDateFrom.setText(sdf.format(date));
            } else {
                dateTo = date;
                etDateTo.setText(sdf.format(date));
            }

            // Reload the list with the new filters
            loadTrainings(true);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void setupPagination() {
        rvTrainings.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == trainingList.size() - 1) {
                    if (!isLoading && !isLastPage) {
                        loadTrainings(false);
                    }
                }
            }
        });
    }

    private void loadTrainings(boolean isInitialLoad) {
        if (isInitialLoad) {
            trainingList.clear();
            adapter.submitList(null);
            lastVisibleDocument = null;
            isLastPage = false;
        }

        isLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        rvTrainings.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().fetchTrainings(this, selectedModalityId, dateFrom, dateTo, lastVisibleDocument, new FirebaseManager.OnTrainingsFetchedListener() {
            @Override
            public void onSuccess(List<Training> trainings, DocumentSnapshot lastVisible) {
                loadingProgressBar.setVisibility(View.GONE);
                isLoading = false;

                if (trainings.isEmpty() && isInitialLoad) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    rvTrainings.setVisibility(View.GONE);
                } else {
                    trainingList.addAll(trainings);
                    adapter.submitList(new ArrayList<>(trainingList));
                }

                lastVisibleDocument = lastVisible;
                if (lastVisible == null) {
                    isLastPage = true;
                }
            }

            @Override
            public void onFailure(Exception e) {
                loadingProgressBar.setVisibility(View.GONE);
                isLoading = false;
                Toast.makeText(MyTrainingsActivity.this, "Error al cargar entrenamientos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load the TensorFlow Lite model
    private void loadModel() {
        ModelManager.getInstance().loadModel(new ModelManager.ModelReadyCallback() {
            @Override
            public void onSuccess(Interpreter interpreter) {
                tfliteInterpreter = interpreter;
                Log.d("MyTrainings", "Modelo de IA cargado exitosamente.");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MyTrainings", "Error al cargar el modelo de IA", e);
                Toast.makeText(MyTrainingsActivity.this, "Error al cargar modelo de IA.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onAnalyzePerformanceClicked() {
        // Verify that the model is loaded
        if (tfliteInterpreter == null) {
            Toast.makeText(this, "El modelo de IA no está listo. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            loadModel();
            return;
        }

        // Verify that there are recent trainings
        if (trainingList.isEmpty()) {
            Toast.makeText(this, "No hay datos para analizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the most recent day of trainings
        List<Training> recentTrainings = getMostRecentDayTrainings();
        if (recentTrainings.isEmpty()) {
            Toast.makeText(this, "No se encontraron entrenamientos recientes.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate averages of recent trainings
        Map<String, Float> averages = calculateAverages(recentTrainings);

        // Preprocess the averages for the model
        float[] modelInput = modelPreprocessor.preprocessInput(averages);
        if (modelInput == null) {
            Toast.makeText(this, "Error al procesar datos para el modelo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare the model output
        float[][] modelOutput = new float[1][16];
        tfliteInterpreter.run(modelInput, modelOutput);

        // Obtain the deficiency ID (0-15)
        int deficiencyId = modelPreprocessor.postprocessOutputToId(modelOutput[0]);
        if (deficiencyId == -1) {
            Toast.makeText(this, "Error al procesar el resultado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the strings from the resources
        String localizedDeficiencyName = getLocalizedDeficiencyName(deficiencyId);
        String randomRecommendation = getRandomRecommendation(deficiencyId);

        // Get the date for the message
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(recentTrainings.get(0).getCreatedAt());

        // Building the message
        String message = String.format(
                Locale.getDefault(),
                "Tu deficiencia en %s (sesión del %s) fue:\n\n%s\n\nRecomendación:\n%s",
                selectedModalityName, // "Modo Reacción", etc.
                formattedDate,        // "17/11/2025"
                localizedDeficiencyName, // "Precisión Crítica | Tiempo Crítico"
                randomRecommendation  // "Enfócate en el control del gatillo..."
        );

        // Show the dialog with the message
        new MaterialAlertDialogBuilder(this)
                .setTitle("Análisis de IA")
                .setMessage(message)
                .setPositiveButton("Entendido", null)
                .show();
    }

    private String getLocalizedDeficiencyName(int deficiencyId) {
        // Build the resource name
        String resourceName = "def_name_" + deficiencyId;
        int resourceId = getResources().getIdentifier(resourceName, "string", getPackageName());

        if (resourceId == 0) {
            return "Deficiencia Desconocida"; // Fallback
        }
        return getString(resourceId);
    }

    private String getRandomRecommendation(int deficiencyId) {
        // Build the resource name
        String resourceName = "def_rec_" + deficiencyId;
        int resourceId = getResources().getIdentifier(resourceName, "array", getPackageName());

        if (resourceId == 0) {
            return "No hay recomendaciones disponibles."; // Fallback
        }

        String[] recommendations = getResources().getStringArray(resourceId);
        if (recommendations.length == 0) {
            return "No hay recomendaciones disponibles."; // Fallback
        }

        // Choose one of the 4 random recommendations
        return recommendations[random.nextInt(recommendations.length)];
    }

    private List<Training> getMostRecentDayTrainings() {
        List<Training> recentTrainings = new ArrayList<>();
        if (trainingList.isEmpty()) return recentTrainings;

        // Sort the list by createdAt in descending order
        Training mostRecent = trainingList.get(0);
        Date mostRecentDate = mostRecent.getCreatedAt();

        // Use calendar to compare dates
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(mostRecentDate);

        for (Training training : trainingList) {
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(training.getCreatedAt());

            // Compare dates using Calendar
            boolean sameDay = cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);

            if (sameDay) {
                recentTrainings.add(training);
            } else {
                break;
            }
        }
        return recentTrainings;
    }

    private Map<String, Float> calculateAverages(List<Training> trainings) {
        long totalHits = 0;
        long totalMisses = 0;
        double totalAvgShotTime = 0.0;
        double totalTrainingTime = 0.0;

        for (Training t : trainings) {
            totalHits += t.getHits();
            totalMisses += t.getMisses();
            totalAvgShotTime += t.getAverageShotTime();
            totalTrainingTime += t.getTrainingTime();
        }

        int count = trainings.size();

        // Calculate averages
        float avgHits = Math.round((float) totalHits / count);
        float avgMisses = Math.round((float) totalMisses / count);
        float avgTotalShots = avgHits + avgMisses;
        float avgAccuracy = (avgTotalShots > 0) ? (avgHits / avgTotalShots) : 0.0f;
        float avgShotTime = (float) (totalAvgShotTime / count);
        float avgTrainingTime = (float) (totalTrainingTime / count);

        // Prepare the raw input
        Map<String, Float> rawInput = new HashMap<>();
        rawInput.put("modality", (float) selectedModalityId);
        rawInput.put("hits", avgHits);
        rawInput.put("misses", avgMisses);
        rawInput.put("total_shots", avgTotalShots);
        rawInput.put("accuracy", avgAccuracy);
        rawInput.put("average_shot_time", avgShotTime);
        rawInput.put("training_time", avgTrainingTime);

        return rawInput;
    }

}