package com.example.movie_recommender;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MovieDetailsActivity extends AppCompatActivity {

    private ImageView movieImage;                                                                   // To show movie poster
    private TextView movieTitle;                                                                    // To show movie title
    private TextView description;                                                                   // To show movie description
    private TextView runtimeView;                                                                   // To show runtime
    private TextView languageView;                                                                  // To show language
    private TextView popularityView;                                                                // To show popularity
    private TextView voteView;                                                                      // To show vote average and count
    private TextView movieUrl;                                                                      // To show homepage URL

    private static final String API_KEY = "84c9ef7e66fdc40d8347137e2afcf2eb";                       // API for movies TMDB
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // initialize UI components
        movieImage = findViewById(R.id.movieImageView);
        movieTitle = findViewById(R.id.movieNameView);
        description = findViewById(R.id.descriptionView);
        runtimeView = findViewById(R.id.runtimeView);
        languageView = findViewById(R.id.languageView);
        popularityView = findViewById(R.id.popularityView);
        voteView = findViewById(R.id.vote);
        movieUrl = findViewById(R.id.movieURL);

        String movieId = getIntent().getStringExtra("MOVIE_ID");                              // gets movie ID from intent
        if (movieId != null){
            new FetchMovieDetails().execute(movieId);                                               // start fetching movie
        } else {
            Toast.makeText(this, "Movie Is Missing!!!", Toast.LENGTH_SHORT).show();
        }
    }

    // Preforming network operations to fetch movie items for display
    private class FetchMovieDetails extends AsyncTask<String, Void, String> {
        private static final String TMDB_MOVIE_DETAILS_URL = "https://api.themoviedb.org/3/movie/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show loading placeholder
            movieTitle.setText("Loading...");
            description.setText("");
        }

        @Override
        protected String doInBackground(String... params) {
            String movieId = params[0];
            String urlString = TMDB_MOVIE_DETAILS_URL + movieId + "?api_key=" + API_KEY;

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Check for successful response
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return "Server returned: " + responseCode + " " + connection.getResponseMessage();
                }

                // Read response
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
                String title = jsonObject.optString("title", "N/A");
                String descriptionMovie = jsonObject.optString("overview", "Description not available");
                String imagePath = jsonObject.optString("poster_path", null);
                String runtime = jsonObject.optInt("runtime", 0) + " minutes";
                String language = jsonObject.optString("original_language", "Unknown").toUpperCase();
                String popularity = String.format("%.2f", jsonObject.optDouble("popularity", 0.0));
                String voteAverage = String.format("%.1f", jsonObject.optDouble("vote_average", 0.0));
                int voteCount = jsonObject.optInt("vote_count", 0);
                String homepage = jsonObject.optString("homepage", "N/A");

                // Set title and description
                movieTitle.setText(title);
                description.setText(descriptionMovie);

                // Set runtime and language
                runtimeView.setText("Runtime: " + runtime);
                languageView.setText("Language: " + language);

                // Set popularity and vote info
                popularityView.setText("Popularity: " + popularity);
                voteView.setText("Rating: " + voteAverage + " / 10 (" + voteCount + " votes)");

                // Set homepage URL
                if (!homepage.equals("N/A")) {
                    movieUrl.setText("Visit Homepage");
                    movieUrl.setOnClickListener(view -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(homepage));
                        startActivity(intent);
                    });
                } else {
                    movieUrl.setText("Homepage: Not Available");
                }

                // Load image using Picasso library
                if (imagePath != null) {
                    String fullImageUrl = "https://image.tmdb.org/t/p/w500" + imagePath;
                    Picasso.get().load(fullImageUrl).into(movieImage);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MovieDetailsActivity.this, "Failed to parse movie details.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}


























