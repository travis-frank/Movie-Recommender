package com.example.movie_recommender;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
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
    private ImageButton back;                                                                       // To go to previous page
    private ImageButton home;                                                                       // To go to home page
    private ImageButton add;
    private Button reviewButton;
    private String movieId;

    private static final String API_KEY = "84c9ef7e66fdc40d8347137e2afcf2eb";                       // API for movies TMDB
    private String title;
    private String imagePath;

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

        // Initialize UI components
        movieImage = findViewById(R.id.movieImageView);
        movieTitle = findViewById(R.id.movieNameView);
        description = findViewById(R.id.descriptionView);
        runtimeView = findViewById(R.id.runtimeView);
        languageView = findViewById(R.id.languageView);
        popularityView = findViewById(R.id.popularityView);
        voteView = findViewById(R.id.vote);
        movieUrl = findViewById(R.id.movieURL);
        back = findViewById(R.id.backButton);
        home = findViewById(R.id.homeButton);
        add = findViewById(R.id.addButton);
        reviewButton = findViewById(R.id.reviewpage);

        // Navigate back
        back.setOnClickListener(view -> finish());

        // Navigate home
        home.setOnClickListener(view -> {
            Intent intent = new Intent(MovieDetailsActivity.this, Homepage.class);
            startActivity(intent);
        });

        // when user clicks review page it will take them to the review activity
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (movieId != null & movieTitle != null)  { // Ensure movieId is not null
                    //Intent intent = new Intent(MovieDetailsActivity.this, reviewPage.class);
                    //intent.putExtra("MOVIE_ID", movieId); // Pass the movie ID to the next activity
                    //intent.putExtra("MOVIE_NAME", movieTitle);
                    //startActivity(intent);
                }
            }
        });

        movieId = getIntent().getStringExtra("MOVIE_ID");                              // gets movie ID from intent
        if (movieId != null){
            new FetchMovieDetails().execute(movieId);                                               // start fetching movie

        } else {
            Toast.makeText(this, "Movie is Missing!!!", Toast.LENGTH_SHORT).show();
        }

        // Add movie to watchlist
        add.setOnClickListener(view -> {
            if (movieId != null && title != null) {
                try {
                    JSONObject movie = new JSONObject();
                    movie.put("id", movieId);
                    movie.put("title", title);
                    movie.put("poster_path", imagePath);

                    // Add movie to watchlist
                    if (addToWatchlist(movie)) {
                        Toast.makeText(MovieDetailsActivity.this, "Added to Watchlist", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MovieDetailsActivity.this, "Already in Watchlist", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Add movie to watchlist in SharedPreferences
    private boolean addToWatchlist(JSONObject movie) {
        SharedPreferences sharedPreferences = getSharedPreferences("WatchlistPrefs", MODE_PRIVATE);
        String watchlistString = sharedPreferences.getString("WATCHLIST", "[]");

        try {
            JSONArray watchlist = new JSONArray(watchlistString);

            // Check if the movie is already in the watchlist
            for (int i = 0; i < watchlist.length(); i++) {
                JSONObject existingMovie = watchlist.getJSONObject(i);
                if (existingMovie.getString("id").equals(movie.getString("id"))) {
                    return false; // Movie already in the watchlist
                }
            }

            // Add movie to the watchlist
            watchlist.put(movie);
            sharedPreferences.edit().putString("WATCHLIST", watchlist.toString()).apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Fetch movie details
    private class FetchMovieDetails extends AsyncTask<String, Void, String> {
        private static final String TMDB_MOVIE_DETAILS_URL = "https://api.themoviedb.org/3/movie/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            movieTitle.setText("Loading...");
            description.setText("");
        }

        @Override
        protected String doInBackground(String... params) {
            movieId = params[0];
            String urlString = TMDB_MOVIE_DETAILS_URL + movieId + "?api_key=" + API_KEY;

            try {
                URL url = new URL(urlString);
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
                JSONObject jsonObject = new JSONObject(result);
                title = jsonObject.optString("title", "N/A");
                String descriptionMovie = jsonObject.optString("overview", "Description not available");
                imagePath = jsonObject.optString("poster_path", null);
                String runtime = jsonObject.optInt("runtime", 0) + " minutes";
                String language = jsonObject.optString("original_language", "Unknown").toUpperCase();
                String popularity = String.format("%.2f", jsonObject.optDouble("popularity", 0.0));
                String voteAverage = String.format("%.1f", jsonObject.optDouble("vote_average", 0.0));
                int voteCount = jsonObject.optInt("vote_count", 0);
                String homepage = jsonObject.optString("homepage", "N/A");

                movieTitle.setText(title);
                description.setText(descriptionMovie);
                runtimeView.setText("Runtime: " + runtime);
                languageView.setText("Language: " + language);
                popularityView.setText("Popularity: " + popularity);
                voteView.setText("Rating: " + voteAverage + " / 10 (" + voteCount + " votes)");

                if (homepage != null && !homepage.isEmpty()) {
                    movieUrl.setText("Visit Homepage");
                    movieUrl.setOnClickListener(view -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(homepage));
                        startActivity(intent);
                    });
                } else {
                    movieUrl.setVisibility(View.GONE);
                }

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
