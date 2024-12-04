package com.example.movie_recommender;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Add this import
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // Import for ImageButton
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReviewMovie extends AppCompatActivity {

    private static final String API_URL_TEMPLATE = "https://api.themoviedb.org/3/movie/%d/reviews?language=en-US&page=1";
    private static final String API_KEY = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NGM5ZWY3ZTY2ZmRjNDBkODM0NzEzN2UyYWZjZjJlYiIsIm5iZiI6MTczMjU2MTMzNS42MzU1NDYyLCJzdWIiOiI2NzQ0YjY5MGQ4ZGI3ZGQxYmE0NTlhYjYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.zCFrwWuhrMDpYZz0f8ohM0PTQNTvRRP8nKlVlOGkkEQ";

    private RecyclerView reviewRecyclerView;
    private ReviewAdapter reviewAdapter;
    private List<JSONObject> reviewList;

    private TextView reviewHeaderTextView; // TextView to display "Reviews of <movie>"
    private TextView submitReviewText;
    private EditText reviewDescriptionText;
    private Button submitReviewButton;
    private Spinner spinner;
    private ImageButton homeButton; // Added homeButton
    private ImageButton backButton; // Added backButton

    private int movieId;
    private String movieName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_movie);

        // Initialize views
        reviewRecyclerView = findViewById(R.id.reviewRecyclerView);
        reviewHeaderTextView = findViewById(R.id.textView4);
        submitReviewText = findViewById(R.id.submitReviewText);
        reviewDescriptionText = findViewById(R.id.reviewDescriptionText);
        submitReviewButton = findViewById(R.id.submitReviewButton);
        spinner = findViewById(R.id.spinner);
        homeButton = findViewById(R.id.homeButton);
        backButton = findViewById(R.id.backButton);

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);

        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewRecyclerView.setAdapter(reviewAdapter);

        // Spinner with ratings
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.rating_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Data from the previous Intent
        Intent intent = getIntent();
        movieId = Integer.parseInt(intent.getStringExtra("MOVIE_ID"));
        movieName = intent.getStringExtra("MOVIE_NAME");

        // "Reviews of: <movieName>"
        if (movieName != null) {
            reviewHeaderTextView.setText("Reviews of: " + movieName);
            submitReviewText.setText("Submit review for: " + movieName);
        } else {
            reviewHeaderTextView.setText("Reviews of Unknown Movie");
            submitReviewText.setText("Submit review");
        }

        // OnClickListener for submitReviewButton
        submitReviewButton.setOnClickListener(v -> submitReview());

        // OnClickListener for homeButton
        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(ReviewMovie.this, Homepage.class);
            startActivity(homeIntent);
        });

        // OnClickListener for backButton
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(ReviewMovie.this, MovieDetailsActivity.class);
            backIntent.putExtra("MOVIE_ID", String.valueOf(movieId));
            backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);  // clears any activity on top of the target activity
            startActivity(backIntent);
            finish();
        });

        // Fetch reviews using the movieId
        if (movieId != -1) {
            fetchReviewsFromApi(movieId);
        } else {
            Log.e("ReviewMovie", "Invalid movieId received.");
        }
    }

    private void submitReview() {
        String reviewText = reviewDescriptionText.getText().toString().trim();
        String ratingString = spinner.getSelectedItem().toString();
        float rating = Float.parseFloat(ratingString);

        if (!reviewText.isEmpty()) {
            JSONObject review = new JSONObject();
            try {
                review.put("author", "CurrentUser");
                review.put("content", reviewText);

                JSONObject authorDetails = new JSONObject();
                authorDetails.put("rating", rating * 2); // Multiply by 2 to match API's 10-point scale
                review.put("author_details", authorDetails);

                // Add the "isLocal" flag
                review.put("isLocal", true);

                // Save the review to file
                saveReviewToFile(review);

                // Add the review to the list and update the adapter
                runOnUiThread(() -> {
                    reviewList.add(0, review);
                    reviewAdapter.notifyDataSetChanged();
                });

                // Clear the input fields
                reviewDescriptionText.setText("");
                spinner.setSelection(0);

                Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to submit review.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show a message to enter text
            Toast.makeText(this, "Please enter a review.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReviewToFile(JSONObject review) {
        String filename = "movie_reviews_" + movieId + ".txt";
        try {
            FileOutputStream fos = openFileOutput(filename, MODE_APPEND);
            fos.write((review.toString() + "\n").getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readLocalReviews() {
        String filename = "movie_reviews_" + movieId + ".txt";
        try {
            FileInputStream fis = openFileInput(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject review = new JSONObject(line);
                reviewList.add(0, review);
            }
            reader.close();
        } catch (IOException e) {
            // File not found or error reading, no local reviews yet
            Log.e("ReviewMovie", "No local reviews found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchReviewsFromApi(int movieId) {
        String apiUrl = String.format(API_URL_TEMPLATE, movieId);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_ERROR", "Failed to fetch reviews: " + e.getMessage());
                // Even if API call fails, try to load local reviews
                readLocalReviews();
                runOnUiThread(() -> reviewAdapter.notifyDataSetChanged());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray results = jsonResponse.getJSONArray("results");

                        reviewList.clear();

                        // Add API reviews
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject review = results.getJSONObject(i);
                            reviewList.add(review); // Add each review as a JSONObject
                        }

                        // Read local reviews and add them to the list
                        readLocalReviews();

                        // Notify the adapter about the new data
                        runOnUiThread(() -> reviewAdapter.notifyDataSetChanged());

                    } catch (Exception e) {
                        Log.e("API_ERROR", "Error parsing JSON: " + e.getMessage());
                    }
                } else {
                    Log.e("API_ERROR", "Unsuccessful response: " + response.code());
                }
            }
        });
    }
}