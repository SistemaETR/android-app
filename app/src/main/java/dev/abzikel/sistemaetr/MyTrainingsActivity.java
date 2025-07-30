package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dev.abzikel.sistemaetr.adapters.TrainingAdapter;
import dev.abzikel.sistemaetr.pojos.Training;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class MyTrainingsActivity extends AppCompatActivity {
    // Views and adapter
    private RecyclerView rvTrainings;
    private TrainingAdapter adapter;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private TextInputEditText etDateFrom, etDateTo;
    private AutoCompleteTextView actvModality;

    // Data management, pagination and filters
    private final List<Training> trainingList = new ArrayList<>();
    private DocumentSnapshot lastVisibleDocument;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private Date dateFrom, dateTo;
    private int selectedModalityId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trainings);

        // Initialize views
        rvTrainings = findViewById(R.id.rvTrainings);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        etDateFrom = findViewById(R.id.etDateFrom);
        etDateTo = findViewById(R.id.etDateTo);
        actvModality = findViewById(R.id.actvModality);

        setupRecyclerView();
        setupPagination();
        setupFilters();

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
        etDateFrom.setOnClickListener(v -> showDatePicker(true));

        // Configure selector of end date
        etDateTo.setOnClickListener(v -> showDatePicker(false));

        // Configure selector of modality
        ArrayAdapter<CharSequence> modalityAdapter = ArrayAdapter.createFromResource(this,
                R.array.modality_array, android.R.layout.simple_spinner_dropdown_item);
        actvModality.setAdapter(modalityAdapter);
        actvModality.setOnItemClickListener((parent, view, position, id) -> {
            selectedModalityId = position;
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
}