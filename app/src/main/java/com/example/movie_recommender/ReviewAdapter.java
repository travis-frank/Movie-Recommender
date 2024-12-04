package com.example.movie_recommender;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<JSONObject> reviewList;
    private final Context context;

    public ReviewAdapter(Context context, List<JSONObject> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.review_item, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        try {
            // Get the review data
            JSONObject review = reviewList.get(position);

            // Check if the review is local
            boolean isLocal = review.optBoolean("isLocal", false);
            String username;
            if (isLocal) {
                username = "Your Review";
            } else {
                username = review.optString("author", "Anonymous");
            }

            String content = review.optString("content", "No content available.");
            double rating = review.optJSONObject("author_details").optDouble("rating", 0.0);
            if (rating >= 0) {
                rating = rating / 2.0;
                if (rating > 5)
                    rating = 5;
                holder.ratingBar.setRating((float) rating);
            } else {
                holder.ratingBar.setRating(0);
            }

            // Bind the data to the views
            holder.authorTextView.setText(username);
            holder.reviewOutput.setText(content);
            holder.ratingBar.setRating((float) rating);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    // ViewHolder class to hold and recycle views
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView;
        TextView reviewOutput;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            reviewOutput = itemView.findViewById(R.id.reviewOutput);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
