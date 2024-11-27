package com.example.movie_recommender;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SearchMovie extends AppCompatActivity {

    private TextInputLayout movieInputLayout;
    private TextInputEditText movieEditText; // Retrieved from TextInputLayout
    private Button searchButton;
    private TextView resultTextView;

    // TMDB API key
    private static final String API_KEY = "84c9ef7e66fdc40d8347137e2afcf2eb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_movie);

        // Initialize UI components
        movieInputLayout = findViewById(R.id.movieInput);
        searchButton = findViewById(R.id.searchButton); // Updated Button ID
        resultTextView = findViewById(R.id.resultTextView);

        // Retrieve EditText from TextInputLayout with an explicit cast
        if (movieInputLayout.getEditText() != null) {
            movieEditText = (TextInputEditText) movieInputLayout.getEditText();
        } else {
            Toast.makeText(this, "EditText not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set onClick listener for the button
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String movieQuery = movieEditText.getText().toString().trim();
                if (!movieQuery.isEmpty()) {
                    new FetchMovieTask().execute(movieQuery);
                } else {
                    Toast.makeText(SearchMovie.this, "Please enter a movie name", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Perform network operation on a background thread
    private class FetchMovieTask extends AsyncTask<String, Void, String> {

        private static final String TMDB_API_URL = "https://api.themoviedb.org/3/search/movie";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show a loading message
            resultTextView.setText("Searching...");
        }

        @Override
        protected String doInBackground(String... params) {
            String query = params[0];
            String encodedQuery = query.replace(" ", "%20");
            String urlString = TMDB_API_URL + "?api_key=" + API_KEY + "&query=" + encodedQuery;

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Check for successful response code
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return "Server returned: " + responseCode + " " + connection.getResponseMessage();
                }

                // Read Response
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

        // Parse JSON to get data and update UI
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray resultsArray = jsonObject.getJSONArray("results");

                if (resultsArray.length() > 0) {
                    JSONObject firstMovie = resultsArray.getJSONObject(0);
                    String title = firstMovie.optString("title", "N/A");
                    String movieId = firstMovie.optString("id", "N/A"); // Extracted Movie ID

                    String displayText = "Title: " + title + "\n\n"
                            + "Id: " + movieId + "\n\n";

                    //Mmake the movie title clickable
                    SpannableString spannableString = new SpannableString(displayText);

                    // Find the start and end indices of the movie title
                    int titleStart = displayText.indexOf("Title: ") + "Title: ".length();
                    int titleEnd = displayText.indexOf("\n\n", titleStart);

                    if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) {
                        // Set a ClickableSpan on the movie title
                        spannableString.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                if (!movieId.equals("N/A")) {
                                    //Intent to start the MovieDetailsActivity
                                    //Intent intent = new Intent(SearchMovie.this, MovieDetailsActivity.class);
                                    //intent.putExtra("MOVIE_ID", movieId);
                                    //startActivity(intent);
                                } else {
                                    Toast.makeText(SearchMovie.this, "Movie ID not available.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, titleStart, titleEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    resultTextView.setText(spannableString);
                    resultTextView.setMovementMethod(LinkMovementMethod.getInstance()); // Enable link clicking

                } else {
                    resultTextView.setText("No results found.");
                }

            } catch (JSONException e) {
                e.printStackTrace();
                resultTextView.setText("Failed to parse JSON.");
                Toast.makeText(SearchMovie.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}


