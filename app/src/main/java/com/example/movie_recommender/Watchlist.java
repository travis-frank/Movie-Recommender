package com.example.movie_recommender;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Watchlist extends AppCompatActivity {
    private ListView watchlistView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchlist);

        watchlistView = findViewById(R.id.watchlistView);

        // Load watchlist
        loadWatchlist();
    }

    private void loadWatchlist() {
        try {
            // Access SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("WatchlistPrefs", MODE_PRIVATE);
            String watchlistString = sharedPreferences.getString("WATCHLIST", "[]");

            // Parse the watchlist as a JSON array
            JSONArray watchlist = new JSONArray(watchlistString);
            ArrayList<String> movieTitles = new ArrayList<>();

            for (int i = 0; i < watchlist.length(); i++) {
                JSONObject movie = watchlist.getJSONObject(i);
                String title = movie.optString("title", "Unknown");
                movieTitles.add(title);
            }

            // Display movies in the ListView
            if (!movieTitles.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movieTitles);
                watchlistView.setAdapter(adapter);
            } else {
                Toast.makeText(this, "No movies in watchlist.", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load watchlist", Toast.LENGTH_SHORT).show();
        }
    }
}
