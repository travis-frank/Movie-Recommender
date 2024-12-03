package com.example.movie_recommender;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;

public class FlickFinder extends AppCompatActivity {

    private Spinner genreSpinner;
    private EditText actorInput;
    private EditText yearInput;
    private Button searchButton;
    private TextView resultTextView;
    private ImageButton back;                                                                       // To go to previous page
    private ImageButton home;

    private static final String API_KEY = "84c9ef7e66fdc40d8347137e2afcf2eb"; // Replace with your TMDb API key
    private static final String TMDB_API_URL = "https://api.themoviedb.org/3/discover/movie";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flick_finder);

        // Initialize UI components
        genreSpinner = findViewById(R.id.genreSpinner);
        actorInput = findViewById(R.id.actorInput);
        yearInput = findViewById(R.id.yearInput);
        searchButton = findViewById(R.id.finderButton);
        resultTextView = findViewById(R.id.resultTextView);
        back = findViewById(R.id.backButton);
        home = findViewById(R.id.homeButton);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FlickFinder.this, Homepage.class);
                startActivity(intent);
            }
        });

        // Set onClick listener for the search button
        searchButton.setOnClickListener(view -> {
            String selectedGenre = genreSpinner.getSelectedItem().toString();
            String actorName = actorInput.getText().toString().trim();
            String year = yearInput.getText().toString().trim();

            // Execute the search task
            new FetchMoviesTask().execute(selectedGenre, actorName, year);
        });
    }

    // Background task to fetch movies
    private class FetchMoviesTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultTextView.setText("Searching...");
        }

        @Override
        protected String doInBackground(String... params) {
            String genre = params[0];
            String actor = params[1];
            String year = params[2];

            String genreId = mapGenreToId(genre); // Map genre name to TMDb genre ID
            String actorId = actor.isEmpty() ? "" : getActorId(actor); // Retrieve actor ID only if an actor is specified

            if (!actor.isEmpty() && actorId.isEmpty()) {
                return "Error: Actor not found";
            }

            // Dynamically build the query URL
            StringBuilder urlBuilder = new StringBuilder(TMDB_API_URL);
            urlBuilder.append("?api_key=").append(API_KEY);

            // Add filters only if they are provided
            if (!genreId.isEmpty()) {
                urlBuilder.append("&with_genres=").append(genreId);
            }
            if (!year.isEmpty()) {
                urlBuilder.append("&primary_release_year=").append(year);
            }
            if (!actorId.isEmpty()) {
                urlBuilder.append("&with_people=").append(actorId);
            }

            try {
                URL url = new URL(urlBuilder.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return "Server returned: " + responseCode + " " + connection.getResponseMessage();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();
                return jsonBuilder.toString();

            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                // Parse JSON response
                JSONObject jsonObject = new JSONObject(result);

                // Check if "results" array exists
                if (!jsonObject.has("results")) {
                    resultTextView.setText("Unexpected response format: Missing 'results' array.");
                    return;
                }

                JSONArray resultsArray = jsonObject.getJSONArray("results");

                if (resultsArray.length() > 0) {
                    SpannableStringBuilder displayText = new SpannableStringBuilder();

                    // Loop through results
                    for (int i = 0; i < resultsArray.length(); i++) {
                        JSONObject movie = resultsArray.getJSONObject(i);
                        String title = movie.optString("title", "N/A");
                        String releaseDate = movie.optString("release_date", "Unknown");
                        String movieId = movie.optString("id", ""); // Get movie ID

                        // Create a SpannableString for the movie title
                        SpannableString spannableTitle = new SpannableString(title + "\n");
                        spannableTitle.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                // Navigate to MovieDetailsActivity
                                Intent intent = new Intent(FlickFinder.this, MovieDetailsActivity.class);
                                intent.putExtra("MOVIE_ID", movieId);
                                startActivity(intent);
                            }
                        }, 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // Append the movie title and release date
                        displayText.append(spannableTitle);
                        displayText.append("Release Date: ").append(releaseDate).append("\n\n");
                    }

                    // Set the displayText to the resultTextView
                    resultTextView.setText(displayText);
                    resultTextView.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clickable links
                } else {
                    resultTextView.setText("No movies found.");
                }

            } catch (JSONException e) {
                // Handle JSON parsing errors
                e.printStackTrace();
                resultTextView.setText("Failed to parse JSON: " + e.getMessage());
                Toast.makeText(FlickFinder.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String mapGenreToId(String genre) {
        Map<String, String> genreMap = new HashMap<>();
        genreMap.put("Action", "28");
        genreMap.put("Adventure", "12");
        genreMap.put("Animation", "16");
        genreMap.put("Comedy", "35");
        genreMap.put("Crime", "80");
        genreMap.put("Documentary", "99");
        genreMap.put("Drama", "18");
        genreMap.put("Family", "10751");
        genreMap.put("Fantasy", "14");
        genreMap.put("History", "36");
        genreMap.put("Horror", "27");
        genreMap.put("Music", "10402");
        genreMap.put("Mystery", "9648");
        genreMap.put("Romance", "10749");
        genreMap.put("Science Fiction", "878");
        genreMap.put("TV Movie", "10770");
        genreMap.put("Thriller", "53");
        genreMap.put("War", "10752");
        genreMap.put("Western", "37");

        return genreMap.getOrDefault(genre, ""); // Return genre ID, or empty string if not found
    }

    private String getActorId(String actorName) {
        String apiUrl = "https://api.themoviedb.org/3/search/person?api_key=" + API_KEY + "&query=" + actorName;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject actor = results.getJSONObject(0);
                return actor.getString("id");
            }

            return "";

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}