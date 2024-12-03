package com.example.movie_recommender;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private int movieId;
    private String movieName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_movie);

        // Initialize views
        reviewRecyclerView = findViewById(R.id.reviewRecyclerView);
        reviewHeaderTextView = findViewById(R.id.textView4); // "Review of:" TextView
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);

        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewRecyclerView.setAdapter(reviewAdapter);

        // Get data from the previous Intent
        Intent intent = getIntent();
        movieId = 98;
        movieName = intent.getStringExtra("movieName");

        // Set "Review of: <movieName>"
        if (movieName != null) {
            reviewHeaderTextView.setText("Reviews of " + movieName);
        } else {
            reviewHeaderTextView.setText("Reviews of Unknown Movie");
        }

        // Fetch reviews using the movieId
        if (movieId != -1) {
            fetchReviewsFromApi(movieId);
        } else {
            Log.e("ReviewMovie", "Invalid movieId received.");
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
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray results = jsonResponse.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject review = results.getJSONObject(i);
                            reviewList.add(review); // Add each review as a JSONObject
                        }

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