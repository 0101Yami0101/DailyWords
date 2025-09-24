package com.example.dailywords;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;


public class MainActivity extends AppCompatActivity {

    private TextView tvWord, tvMeaning, tvExample, tvPartOfSpeech;
    private ProgressBar progress;
    private Button btnRefreshNow;

    // 🔍 Search-related views
    private TextInputEditText etSearch;
    private LinearLayout searchResultContainer;
    private TextView tvSearchWord, tvSearchPOS, tvSearchMeaning, tvSearchExample;

    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private List<WordItem> historyList = new ArrayList<>();

    public static final String PREFS = "DailyWordPrefs";
    public static final String HISTORY_KEY = "word_history";

    private BroadcastReceiver wordUpdateReceiver;

    private Gson gson = new Gson();
    private LinearLayout searchContainer;
    private TextInputLayout searchLayout;


    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Daily word views
        tvWord = findViewById(R.id.tvWord);
        tvPartOfSpeech = findViewById(R.id.tvPartOfSpeech);
        tvMeaning = findViewById(R.id.tvMeaning);
        tvExample = findViewById(R.id.tvExample);
        progress = findViewById(R.id.progress);
        btnRefreshNow = findViewById(R.id.btnGetNew);

        // Search views
        etSearch = findViewById(R.id.etSearch);
        searchContainer = findViewById(R.id.searchContainer);
        searchLayout = findViewById(R.id.searchLayout);
        searchResultContainer = findViewById(R.id.searchResultContainer);
        tvSearchWord = findViewById(R.id.tvSearchWord);
        tvSearchPOS = findViewById(R.id.tvSearchPOS);
        tvSearchMeaning = findViewById(R.id.tvSearchMeaning);
        tvSearchExample = findViewById(R.id.tvSearchExample);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setTitle("DailyWords");

        // History RecyclerView
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(historyAdapter);

        requestNotificationPermissionIfNeeded();

        // Load history into RecyclerView
        loadHistory();

        // Load current word from SharedPreferences
        loadSavedWord();

        searchLayout.setStartIconOnClickListener(v -> performSearch());

        searchLayout.setEndIconOnClickListener(v -> toggleSearch(false));

        // Manual refresh
        btnRefreshNow.setOnClickListener(v -> {
            progress.setVisibility(View.VISIBLE);
            WordFetcher.fetchWord(MainActivity.this, false, () -> {
                progress.setVisibility(View.GONE);
                loadSavedWord();
                loadHistory(); // update history UI
            });
        });

        // 🔍 Handle search action
        etSearch.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            keyEvent.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });

        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_open_search) {
                toggleSearch(true);
                return true;
            }
            return false;
        });

        // Schedule repeating fetch
        scheduleRepeatingAlarm();

        // Listen for broadcast when a new word is saved
        wordUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadSavedWord(); // update UI with current word
                loadHistory();   // refresh history
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(wordUpdateReceiver,
                    new IntentFilter("com.example.dailywords.NEW_WORD"),
                    Context.RECEIVER_EXPORTED);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            progress.setVisibility(View.VISIBLE);
            WordFetcher.fetchSpecificWord(MainActivity.this, query, (word, pos, meaning, example) -> {
                progress.setVisibility(View.GONE);
                searchResultContainer.setVisibility(View.VISIBLE);
                tvSearchWord.setText(word);
                tvSearchPOS.setText(pos);
                tvSearchMeaning.setText(meaning);
                tvSearchExample.setText(example);
            });
        }
    }


    private void loadSavedWord() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        tvWord.setText(prefs.getString("word", "No word yet"));
        tvPartOfSpeech.setText(prefs.getString("pos", ""));
        tvMeaning.setText(prefs.getString("meaning", ""));
        tvExample.setText(prefs.getString("example", ""));
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = prefs.getString(HISTORY_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<WordItem>>() {}.getType();
            List<WordItem> savedList = gson.fromJson(json, type);
            if (savedList != null) {
                historyList.clear();
                historyList.addAll(savedList);
                historyAdapter.notifyDataSetChanged();
            }
        }
    }

    private void scheduleRepeatingAlarm() {
        Intent intent = new Intent(this, WordReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long interval = 24 * 60 * 60 * 1000;
        long start = System.currentTimeMillis();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, start, interval, pendingIntent);
    }

    private void toggleSearch(boolean open) {
        if (open) {
            if (searchContainer.getVisibility() == View.GONE) {
                searchContainer.setVisibility(View.VISIBLE);

                // Slide down + fade in animation
                TranslateAnimation slide = new TranslateAnimation(0, 0, -50, 0);
                slide.setDuration(200);
                AlphaAnimation fade = new AlphaAnimation(0, 1);
                fade.setDuration(200);

                searchContainer.startAnimation(slide);
                searchContainer.startAnimation(fade);

                etSearch.requestFocus();
            }
        } else {
            if (searchContainer.getVisibility() == View.VISIBLE) {
                // Slide up + fade out animation
                TranslateAnimation slide = new TranslateAnimation(0, 0, 0, -50);
                slide.setDuration(200);
                AlphaAnimation fade = new AlphaAnimation(1, 0);
                fade.setDuration(200);

                searchContainer.startAnimation(slide);
                searchContainer.startAnimation(fade);

                searchContainer.setVisibility(View.GONE);
                etSearch.setText(""); // clear text
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(wordUpdateReceiver);
        } catch (IllegalArgumentException ignored) {}
    }
}
