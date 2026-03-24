package com.example.sakhi;

import java.util.List;

public class NewsResponse {
    private String status;
    private List<Article> articles;

    public List<Article> getArticles() { return articles; }
}