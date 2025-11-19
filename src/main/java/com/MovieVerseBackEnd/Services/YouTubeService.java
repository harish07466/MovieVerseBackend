package com.MovieVerseBackEnd.Services;

import com.MovieVerseBackEnd.Entites.YouTubeCache;
import com.MovieVerseBackEnd.Repositories.YouTubeCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class YouTubeService {

    private final YouTubeCacheRepository cacheRepository;
    private final WebClient webClient;
    private int currentKeyIndex = 0;

    private final List<String> apiKeys;

    public YouTubeService(
            YouTubeCacheRepository cacheRepository,
            WebClient.Builder webClientBuilder,
            @Value("${youtube.api.keys}") String apiKeysCsv
    ) {
        this.cacheRepository = cacheRepository;
        this.webClient = webClientBuilder.baseUrl("https://www.googleapis.com/youtube/v3").build();
        this.apiKeys = Arrays.asList(apiKeysCsv.split(","));
        System.out.println("🔑 YouTube API keys loaded: " + this.apiKeys.size());
    }

    private String getCurrentApiKey() {
        return apiKeys.get(currentKeyIndex % apiKeys.size());
    }

    private void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        System.out.println("🔄 Switched to API key #" + (currentKeyIndex + 1));
    }

    public Mono<String> searchYouTube(String query) {
        // 🔍 Step 1 — Check cache
        Optional<YouTubeCache> cached = cacheRepository.findByQuery(query);
        if (cached.isPresent()) {
            System.out.println("🗃️ Cache hit for query: " + query);
            return Mono.just(cached.get().getResponseJson());
        }

        System.out.println("🔍 Cache miss — calling YouTube API for query: " + query);

        // 🔍 Step 2 — Call YouTube API
        return callYouTubeWithRetry(query, 0)
                .doOnNext(responseJson -> {
                    // Save to DB
                    cacheRepository.save(new YouTubeCache(query, responseJson));
                    System.out.println("✅ Cached YouTube response for: " + query);
                });
    }

    private Mono<String> callYouTubeWithRetry(String query, int attempt) {
        if (attempt >= apiKeys.size()) {
            System.out.println("❌ All YouTube API keys exhausted.");
            return Mono.just("{\"error\":\"quotaExceeded\"}");
        }

        String apiKey = getCurrentApiKey();
        String url = "/search?part=snippet&type=video&maxResults=8&q=" + query + "&key=" + apiKey;

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(err -> {
                    System.err.println("⚠️ YouTube API error (attempt " + attempt + "): " + err.getMessage());
                    rotateApiKey();
                    return callYouTubeWithRetry(query, attempt + 1);
                })
                .flatMap(response -> {
                    if (response.contains("quotaExceeded")) {
                        rotateApiKey();
                        return callYouTubeWithRetry(query, attempt + 1);
                    }
                    return Mono.just(response);
                });
    }
}

