package com.MovieVerseBackEnd.Entites;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_cache")
public class YouTubeCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String query;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String responseJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    public YouTubeCache() {}

    public YouTubeCache(String query, String responseJson) {
        this.query = query;
        this.responseJson = responseJson;
    }

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

