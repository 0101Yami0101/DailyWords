package com.example.dailywords;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.content.Intent;

public class WordFetcher {
    private static final String TAG = "WordFetcher";
    private static final OkHttpClient client = new OkHttpClient();

    // Added parameter: showNotification
    public static void fetchWord(Context context, boolean showNotification, Runnable onComplete) {
        String randomWordApi = "https://random-word-api.vercel.app/api?words=1";
        Request randomReq = new Request.Builder().url(randomWordApi).build();

        client.newCall(randomReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Random word fetch failed: " + e.getMessage());
                if (onComplete != null) runOnMain(onComplete);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                response.close();

                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr.size() == 0) throw new Exception("Empty array");
                    String word = arr.get(0).getAsString().toLowerCase(Locale.ROOT);
                    fetchMeaning(context, word, showNotification, onComplete);
                } catch (Exception ex) {
                    Log.e(TAG, "Parse error random word: " + ex.getMessage());
                    if (onComplete != null) runOnMain(onComplete);
                }
            }
        });
    }

    // Pass showNotification along
    private static void fetchMeaning(Context context, String word, boolean showNotification, Runnable onComplete) {
        String dictUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
        Request dictReq = new Request.Builder().url(dictUrl).build();

        client.newCall(dictReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Meaning fetch failed: " + e.getMessage());
                if (onComplete != null) runOnMain(onComplete);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                response.close();

                String pos = "", def = "Not found", ex = "No example";

                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    JsonArray meanings = arr.get(0).getAsJsonObject().getAsJsonArray("meanings");

                    for (int i = 0; i < meanings.size(); i++) {
                        JsonObject meaningObj = meanings.get(i).getAsJsonObject();
                        pos = meaningObj.get("partOfSpeech").getAsString();
                        JsonArray defs = meaningObj.getAsJsonArray("definitions");
                        if (defs.size() > 0) {
                            JsonObject defObj = defs.get(0).getAsJsonObject();
                            if (defObj.has("definition")) def = defObj.get("definition").getAsString();
                            if (defObj.has("example")) ex = defObj.get("example").getAsString();
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Dictionary parse error: " + e.getMessage());
                }

                saveWord(context, word, pos, def, ex, showNotification);
                if (onComplete != null) runOnMain(onComplete);
            }
        });
    }

    // Add flag for notification + history saving
    private static void saveWord(Context context, String word, String pos, String meaning, String example, boolean showNotification) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);

        // ✅ Save old word into history before overwriting
        String oldWord = prefs.getString("word", null);
        String oldMeaning = prefs.getString("meaning", null);

        if (oldWord != null && oldMeaning != null && !oldWord.equals("No word yet")) {
            Gson gson = new Gson();
            String json = prefs.getString(MainActivity.HISTORY_KEY, null);
            Type type = new TypeToken<ArrayList<WordItem>>(){}.getType();
            List<WordItem> history = json != null ? gson.fromJson(json, type) : new ArrayList<>();

            // Insert at top
            history.add(0, new WordItem(oldWord, oldMeaning));

            // Save back
            prefs.edit().putString(MainActivity.HISTORY_KEY, gson.toJson(history)).apply();
            Log.d(TAG, "History updated with: " + oldWord);
        }

        // ✅ Now overwrite prefs with new word
        prefs.edit()
                .putString("word", word)
                .putString("pos", pos)
                .putString("meaning", meaning)
                .putString("example", example)
                .apply();

        Log.d(TAG, "Word saved: " + word);

        // Broadcast so UI updates
        Intent intent = new Intent("com.example.dailywords.NEW_WORD");
        context.sendBroadcast(intent);

        // 🔔 Show notification only if auto-refresh
        if (showNotification) {
            NotificationHelper.showWordRefreshedNotification(context);
        }
    }

    private static void runOnMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    public static void fetchSpecificWord(Context context, String word, WordCallback callback) {
        String dictUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
        Request dictReq = new Request.Builder().url(dictUrl).build();

        client.newCall(dictReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnMain(() -> callback.onResult(word, "", "Not found", "No example"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                response.close();

                String pos = "", def = "Not found", ex = "No example";
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    JsonArray meanings = arr.get(0).getAsJsonObject().getAsJsonArray("meanings");
                    for (int i = 0; i < meanings.size(); i++) {
                        JsonObject meaningObj = meanings.get(i).getAsJsonObject();
                        pos = meaningObj.get("partOfSpeech").getAsString();
                        JsonArray defs = meaningObj.getAsJsonArray("definitions");
                        if (defs.size() > 0) {
                            JsonObject defObj = defs.get(0).getAsJsonObject();
                            if (defObj.has("definition")) def = defObj.get("definition").getAsString();
                            if (defObj.has("example")) ex = defObj.get("example").getAsString();
                            break;
                        }
                    }
                } catch (Exception e) {}

                String finalPos = pos, finalDef = def, finalEx = ex;
                runOnMain(() -> callback.onResult(word, finalPos, finalDef, finalEx));
            }
        });
    }

    public interface WordCallback {
        void onResult(String word, String pos, String meaning, String example);
    }

}
