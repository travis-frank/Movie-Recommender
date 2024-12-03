package com.example.movie_recommender;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WatchlistHelper {

    private static final String PREF_NAME = "WatchlistPrefs";
    private static final String WATCHLIST_KEY = "WATCHLIST"; // Ensure consistency with keys

    private final SharedPreferences sharedPreferences;

    public WatchlistHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Add a movie to the watchlist
    public void addToWatchlist(JSONObject movie) {
        try {
            JSONArray watchlist = getWatchlist();
            String newMovieId = movie.optString("id");

            // Prevent duplicates
            if (!isMovieInWatchlist(newMovieId)) {
                watchlist.put(movie);
                sharedPreferences.edit().putString(WATCHLIST_KEY, watchlist.toString()).apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the full watchlist
    public JSONArray getWatchlist() {
        String watchlistString = sharedPreferences.getString(WATCHLIST_KEY, "[]");
        try {
            return new JSONArray(watchlistString);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // Remove a movie from the watchlist by ID
    public void removeFromWatchlist(String movieId) {
        try {
            JSONArray watchlist = getWatchlist();
            JSONArray updatedWatchlist = new JSONArray();

            for (int i = 0; i < watchlist.length(); i++) {
                JSONObject movie = watchlist.getJSONObject(i);
                if (!movie.optString("id").equals(movieId)) {
                    updatedWatchlist.put(movie);
                }
            }

            sharedPreferences.edit().putString(WATCHLIST_KEY, updatedWatchlist.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Check if a movie is already in the watchlist
    public boolean isMovieInWatchlist(String movieId) {
        try {
            JSONArray watchlist = getWatchlist();
            for (int i = 0; i < watchlist.length(); i++) {
                JSONObject movie = watchlist.getJSONObject(i);
                if (movie.optString("id").equals(movieId)) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
