package com.MovieVerseBackEnd.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.MovieVerseBackEnd.Clients.OmdbClient;
import com.MovieVerseBackEnd.Clients.YouTubeClient;
import com.MovieVerseBackEnd.DTOs.MovieDetailDto;
import com.MovieVerseBackEnd.DTOs.MovieSummaryDto;
import com.MovieVerseBackEnd.DTOs.YouTubeVideoDto;
import com.MovieVerseBackEnd.Entites.YouTubeCache;
import com.MovieVerseBackEnd.Repositories.YouTubeCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final OmdbClient omdbClient;
    private final YouTubeClient youTubeClient;
    private final YouTubeCacheRepository cacheRepository;

    @Value("${youtube.api.keys}")
    private String[] youtubeApiKeys;

    private int currentKeyIndex = 0;

    public MovieService(OmdbClient omdbClient, YouTubeClient youTubeClient, YouTubeCacheRepository cacheRepository) {
        this.omdbClient = omdbClient;
        this.youTubeClient = youTubeClient;
        this.cacheRepository = cacheRepository;
    }

    private String getCurrentApiKey() {
        return youtubeApiKeys[currentKeyIndex];
    }

    private void rotateApiKey() {
        int oldIndex = currentKeyIndex;
        currentKeyIndex = (currentKeyIndex + 1) % youtubeApiKeys.length;

        System.out.println("🔁 Rotating API key...");
        for (int i = 0; i < youtubeApiKeys.length; i++) {
            String masked = youtubeApiKeys[i].substring(0, 10) + "...";
            if (i == currentKeyIndex) {
                System.out.println("✅ Active key #" + (i + 1) + ": " + masked);
            } else {
                System.out.println("🗝️  Key #" + (i + 1) + ": " + masked);
            }
        }
    }

    @PostConstruct
    private void logLoadedKeys() {
        System.out.println("🔑 YouTube API keys loaded: " + youtubeApiKeys.length);
        for (int i = 0; i < youtubeApiKeys.length; i++) {
            String masked = youtubeApiKeys[i].substring(0, 10) + "...";
            System.out.println("   #" + (i + 1) + ": " + masked);
        }
        System.out.println("✅ Starting with key #1\n");
    }

    // ========================= 🎬 MOVIES ==============================
    public Mono<List<MovieSummaryDto>> searchMovies(String q) {
        return omdbClient.search(q);
    }

    public Mono<MovieDetailDto> getMovieById(String imdbId) {
        return omdbClient.getById(imdbId);
    }

    // ========================= 🎥 TRAILERS ==============================
    public Mono<String> findTrailerEmbedUrl(String title, String year, String lang) {
        if (title == null || title.isBlank()) return Mono.empty();

        String languagePart = (lang == null || lang.isBlank()) ? "" : lang;
        String query = String.format("%s %s official trailer %s", title, year, languagePart).trim();

        // 🔍 Check DB cache first
        Optional<YouTubeCache> cached = cacheRepository.findByQuery(query);
        if (cached.isPresent()) {
            System.out.println("🗃️ Cache hit (Trailer): " + query);
            return Mono.just(cached.get().getResponseJson());
        }

        String apiKey = getCurrentApiKey();

        return youTubeClient.searchVideoIds(query, 8, apiKey)
                .flatMap(ids -> youTubeClient.filterEmbeddable(ids, apiKey)
                        .flatMap(filtered -> {
                            if (filtered.isEmpty()) {
                                // fallback scraping if API quota exceeded
                                return Mono.justOrEmpty(scrapeYouTubeForVideoId(query)
                                        .map(id -> "https://www.youtube.com/embed/" + id)
                                        .orElse(""));
                            }
                            String embedUrl = "https://www.youtube.com/embed/" + filtered.get(0);
                            cacheRepository.save(new YouTubeCache(query, embedUrl));
                            System.out.println("✅ Cached Trailer: " + query);
                            return Mono.just(embedUrl);
                        }))
                .onErrorResume(err -> {
                    String msg = err.getMessage();
                    if (msg != null && msg.contains("quotaExceeded")) {
                        System.out.println("⚠️ Quota exceeded for trailers, rotating API key...");
                        rotateApiKey();
                        return findTrailerEmbedUrl(title, year, lang);
                    }
                    err.printStackTrace();
                    // scraping fallback if API fails
                    return Mono.justOrEmpty(scrapeYouTubeForVideoId(query)
                            .map(id -> "https://www.youtube.com/embed/" + id)
                            .orElse(""));
                });
    }

    // ========================= 🎵 SONGS ==============================
    public Mono<List<YouTubeVideoDto>> findSongsWithDetails(String title, String year, String lang, int limit) {
        if (title == null || title.isBlank()) return Mono.just(List.of());

        List<String> queries = List.of(
                String.format("%s %s song %s", title, year, lang),
                String.format("%s %s official song %s", title, year, lang),
                String.format("%s %s full album %s", title, year, lang),
                String.format("%s %s soundtrack %s", title, year, lang),
                String.format("%s %s official audio %s", title, year, lang)
        );

        return tryFindSongs(queries, limit, 0);
    }

    private Mono<List<YouTubeVideoDto>> tryFindSongs(List<String> queries, int limit, int attempt) {
        if (attempt >= youtubeApiKeys.length) {
            System.out.println("❌ All YouTube API keys exhausted for songs — switching to scraping fallback.");
            return scrapeYouTubeForVideoIds(queries.get(0))
                    .map(ids -> ids.stream()
                            .limit(limit)
                            .map(id -> new YouTubeVideoDto(id, "Song", "YouTube"))
                            .toList());
        }

        String apiKey = getCurrentApiKey();

        return Flux.fromIterable(queries)
                .flatMap(q -> {
                    Optional<YouTubeCache> cached = cacheRepository.findByQuery(q);
                    if (cached.isPresent()) {
                        System.out.println("🗃️ Cache hit (Songs): " + q);
                        // Convert single cached response into a Flux<String>
                        return Flux.just(cached.get().getResponseJson());
                    }

                    // Fetch YouTube video IDs for this query (Mono<List<String>> → Flux<String>)
                    return youTubeClient.searchVideoIds(q, 6, apiKey)
                            .flatMapMany(Flux::fromIterable);
                })
                .distinct()
                .buffer(20)
                .flatMap(batch ->
                        youTubeClient.filterEmbeddable(batch, apiKey)
                                .flatMapIterable(ids -> ids)
                )
                .distinct()
                .take(limit)
                .collectList()
                .flatMap(filteredIds -> {
                    if (filteredIds.isEmpty()) {
                        // fallback scraping if API returned no IDs
                        return scrapeYouTubeForVideoIds(queries.get(0))
                                .map(ids -> ids.stream()
                                        .limit(limit)
                                        .map(id -> new YouTubeVideoDto(id, "Song", "YouTube"))
                                        .toList());
                    }

                    // fetch full details for valid embeddable IDs
                    return youTubeClient.getVideoSnippetsWithDetails(filteredIds, apiKey)
                            .doOnNext(videos -> {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    String cacheJson = mapper.writeValueAsString(videos);
                                    cacheRepository.save(new YouTubeCache(queries.get(0), cacheJson));
                                    System.out.println("✅ Cached Songs for: " + queries.get(0));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });

                })
                .onErrorResume(err -> {
                    String msg = err.getMessage();
                    if (msg != null && msg.contains("quotaExceeded")) {
                        System.out.println("⚠️ Quota exceeded while fetching songs. Rotating key...");
                        rotateApiKey();
                        return tryFindSongs(queries, limit, attempt + 1);
                    }

                    err.printStackTrace();
                    // fallback to scraping if everything fails
                    return scrapeYouTubeForVideoIds(queries.get(0))
                            .map(ids -> ids.stream()
                                    .limit(limit)
                                    .map(id -> new YouTubeVideoDto(id, "Song", "YouTube"))
                                    .toList());
                });
    }


    // ========================= 🧠 UTILS ==============================
    public String extractOriginalLanguage(MovieDetailDto movie) {
        if (movie == null || movie.Language == null || movie.Language.isBlank()) return "English";
        return movie.Language.split(",")[0].trim();
    }

    // ========================= 🕸️ SCRAPING FALLBACK ==============================
 // ======================= 🕸️ SCRAPING FALLBACK ==========================
    private Optional<String> scrapeYouTubeForVideoId(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://www.youtube.com/results?search_query=" + encoded;

            System.out.println("🕷️ Scraping YouTube search page: " + searchUrl);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                               "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .timeout(8000)
                    .get();

            // Extract video IDs from the HTML
            Elements links = doc.select("a[href^=/watch?v=]");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.contains("watch?v=")) {
                    String videoId = href.substring(href.indexOf("=") + 1);
                    if (videoId.length() == 11) { // YouTube video IDs are 11 chars
                        System.out.println("✅ Scraped Video ID: " + videoId);
                        return Optional.of(videoId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Scrape failed for query: " + query);
            e.printStackTrace();
        }
        return Optional.empty();
    }


    private Mono<List<String>> scrapeYouTubeForVideoIds(String query) {
        return Mono.fromCallable(() -> {
            List<String> ids = new ArrayList<>();
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String searchUrl = "https://www.youtube.com/results?search_query=" + encoded;

                System.out.println("🕷️ Scraping YouTube results for: " + query);

                Document doc = Jsoup.connect(searchUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                   "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .timeout(8000)
                        .get();

                // Collect all unique video IDs
                Elements links = doc.select("a[href^=/watch?v=]");
                for (Element link : links) {
                    String href = link.attr("href");
                    if (href.contains("watch?v=")) {
                        String id = href.substring(href.indexOf("=") + 1);
                        if (id.length() == 11 && !ids.contains(id)) {
                            ids.add(id);
                        }
                    }
                }

                System.out.println("✅ Scraped " + ids.size() + " YouTube video IDs for " + query);
                return ids;
            } catch (Exception e) {
                System.err.println("❌ Scraping error for: " + query);
                e.printStackTrace();
                return List.<String>of();
            }
        });
    }

}
