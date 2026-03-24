package com.example.sakhi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {

    private final Context context;
    private final List<Article> articleList;
    private final int layoutId;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public ArticleAdapter(Context context, List<Article> articleList, int layoutId) {
        this.context = context;
        this.articleList = articleList;
        this.layoutId = layoutId;
        this.sharedPreferences = context.getSharedPreferences("SakhiBookmarks", Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Article article = articleList.get(position);

        // Bind Title and Description with Null Checks
        holder.tvTitle.setText(article.getTitle() != null ? article.getTitle() : "No Title");
        holder.tvDesc.setText(article.getDescription() != null ? article.getDescription() : "No description available.");

        // 🖼️ Load image using Glide with enhanced error handling
        Glide.with(context)
                .load(article.getUrlToImage())
                .placeholder(R.drawable.ic_launcher_background)
                .error(android.R.drawable.ic_menu_report_image) // Default error icon if image fails
                .centerCrop()
                .into(holder.imgArticle);

        // 🔹 Check bookmark state to set icon (ic_bookmark_filled or ic_bookmark_border)
        if (isBookmarked(article)) {
            holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border);
        }

        // 🔹 Bookmark Click logic
        holder.imgBookmark.setOnClickListener(v -> {
            if (isBookmarked(article)) {
                removeFromBookmarks(article);
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_border);
                Toast.makeText(context, "Removed from bookmarks", Toast.LENGTH_SHORT).show();
            } else {
                addToBookmarks(article);
                holder.imgBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                Toast.makeText(context, "Saved to bookmarks! 🌸", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔹 Item Click: Open full article in phone browser
        holder.itemView.setOnClickListener(v -> {
            String url = article.getUrl();
            if (url != null && !url.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                // Cleartext traffic (http) is allowed via Manifest, so this will work for all links
                context.startActivity(browserIntent);
            } else {
                Toast.makeText(context, "Link not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return articleList != null ? articleList.size() : 0;
    }

    // --- 🔹 Internal Bookmark Logic Methods ---

    private void addToBookmarks(Article article) {
        List<Article> bookmarks = getBookmarks();
        bookmarks.add(article);
        saveBookmarksList(bookmarks);
    }

    private void removeFromBookmarks(Article article) {
        List<Article> bookmarks = getBookmarks();
        for (int i = 0; i < bookmarks.size(); i++) {
            // Compare URLs to find the correct article to remove
            if (bookmarks.get(i).getUrl() != null && bookmarks.get(i).getUrl().equals(article.getUrl())) {
                bookmarks.remove(i);
                break;
            }
        }
        saveBookmarksList(bookmarks);
    }

    private boolean isBookmarked(Article article) {
        List<Article> bookmarks = getBookmarks();
        if (article.getUrl() == null) return false;
        for (Article b : bookmarks) {
            if (article.getUrl().equals(b.getUrl())) return true;
        }
        return false;
    }

    private List<Article> getBookmarks() {
        String json = sharedPreferences.getString("bookmark_list", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Article>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveBookmarksList(List<Article> list) {
        String json = gson.toJson(list);
        sharedPreferences.edit().putString("bookmark_list", json).apply();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        ImageView imgArticle, imgBookmark;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvArticleTitle);
            tvDesc = itemView.findViewById(R.id.tvArticleDesc);
            imgArticle = itemView.findViewById(R.id.imgArticle);
            imgBookmark = itemView.findViewById(R.id.imgBookmark);
        }
    }
}