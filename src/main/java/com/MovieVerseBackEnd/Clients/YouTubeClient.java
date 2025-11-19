package com.MovieVerseBackEnd.Clients;

import com.MovieVerseBackEnd.DTOs.YouTubeSearchResponse;
import com.MovieVerseBackEnd.DTOs.YouTubeVideoDto;
import com.MovieVerseBackEnd.DTOs.YouTubeVideosResponse;
import com.MovieVerseBackEnd.Entites.YouTubeCache;
import com.MovieVerseBackEnd.Repositories.YouTubeCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.UriSpec;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YouTubeClient {

    private final WebClient webClient;
    private final YouTubeCacheRepository cacheRepo;

    public YouTubeClient(WebClient webClient, YouTubeCacheRepository cacheRepo) {
        this.webClient = webClient;
        this.cacheRepo = cacheRepo;
    }

    /** 🔍 Search YouTube and return list of video IDs (with DB cache) */
    public Mono<List<String>> searchVideoIds(String query, int maxResults, String apiKey) {
        if (query == null || query.isBlank()) {
            System.err.println("⚠️ YouTubeClient: query is blank!");
            return Mono.just(List.of());
        }

        // 🔹 Normalize query
        String trimmedQuery = query.trim();
        String cacheKey = "search:" + trimmedQuery.toLowerCase();

        // 🔹 Try cache first (valid for 24 hours)
        Optional<YouTubeCache> cached = cacheRepo.findByQuery(cacheKey);
        if (cached.isPresent()) {
            YouTubeCache entry = cached.get();
            if (entry.getCreatedAt().isAfter(LocalDateTime.now().minus(1, ChronoUnit.DAYS))) {
                System.out.println("🗃️ Cache hit (Search): " + trimmedQuery);
                return Mono.just(Arrays.asList(entry.getResponseJson().split(",")));
            } else {
                cacheRepo.delete(entry); // old cache
            }
        }

        String encodedQuery = trimmedQuery.replace(" ", "+");
        String uri = String.format(
                "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=%d&q=%s&key=%s&regionCode=IN&relevanceLanguage=en",
                maxResults, encodedQuery, apiKey);

        System.out.println("🔍 YouTube API Request: " + uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(YouTubeSearchResponse.class)
                .map(resp -> {
                    List<String> ids = (resp != null && resp.items != null)
                            ? resp.items.stream()
                            .map(i -> i.id.videoId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
                            : List.of();

                    if (!ids.isEmpty()) {
                        String joined = String.join(",", ids);
                        cacheRepo.save(new YouTubeCache(cacheKey, joined));
                        System.out.println("✅ Cached Search: " + trimmedQuery);
                    }
                    return ids;
                })
                .onErrorResume(err -> {
                    System.err.println("YouTube search error: " + err.getMessage());
                    return Mono.just(List.of());
                });
    }

    /** 🎥 Get /videos details (safe URI + error handling) */
    public Mono<YouTubeVideosResponse> getVideoDetails(List<String> videoIds, String apiKey) {
        if (videoIds == null || videoIds.isEmpty()) {
            YouTubeVideosResponse empty = new YouTubeVideosResponse();
            empty.items = List.of();
            return Mono.just(empty);
        }

        String ids = String.join(",", videoIds);
        String uri = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=status,contentDetails&id=%s&key=%s",
                ids, apiKey);

        return ((UriSpec<?>) webClient.get()
                .uri(uriBuilder -> uriBuilder.path("").build()))
                .uri(uri)
                .retrieve()
                .bodyToMono(YouTubeVideosResponse.class)
                .onErrorResume(err -> {
                    System.err.println("YouTube videos error: " + err.getMessage());
                    YouTubeVideosResponse fallback = new YouTubeVideosResponse();
                    fallback.items = List.of();
                    return Mono.just(fallback);
                });
    }

    /** ✅ Filter embeddable/public videos */
    public Mono<List<String>> filterEmbeddable(List<String> videoIds, String apiKey) {
        if (videoIds == null || videoIds.isEmpty()) return Mono.just(List.of());
        return getVideoDetails(videoIds, apiKey)
                .map(resp -> {
                    if (resp == null || resp.items == null) return List.of();
                    return resp.items.stream()
                            .filter(item -> {
                                if (item.status == null) return false;
                                boolean embeddable = item.status.embeddable;
                                boolean isPublic = "public".equalsIgnoreCase(item.status.privacyStatus);
                                boolean regionBlocked = item.contentDetails != null
                                        && item.contentDetails.regionRestriction != null
                                        && item.contentDetails.regionRestriction.blocked != null
                                        && !item.contentDetails.regionRestriction.blocked.isEmpty();
                                return embeddable && isPublic && !regionBlocked;
                            })
                            .map(i -> i.id)
                            .collect(Collectors.toList());
                });
    }

    /** 🎶 Get video snippet info (title, channel) — cached by ID list */
    public Mono<List<YouTubeVideoDto>> getVideoSnippetsWithDetails(List<String> videoIds, String apiKey) {
        if (videoIds == null || videoIds.isEmpty()) return Mono.just(List.of());

        String joinedIds = String.join(",", videoIds);
        String cacheKey = "details:" + joinedIds;

        // Check cache
        Optional<YouTubeCache> cached = cacheRepo.findByQuery(cacheKey);
        if (cached.isPresent()) {
            YouTubeCache entry = cached.get();
            if (entry.getCreatedAt().isAfter(LocalDateTime.now().minus(1, ChronoUnit.DAYS))) {
                System.out.println("🗃️ Cache hit (Video details): " + videoIds.size() + " videos");
                return Mono.just(parseVideoDetails(entry.getResponseJson()));
            } else {
                cacheRepo.delete(entry);
            }
        }

        String uri = String.format(
                "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=%s&key=%s",
                joinedIds, apiKey);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(YouTubeSearchResponse.class)
                .map(resp -> {
                    List<YouTubeVideoDto> list = (resp != null && resp.items != null)
                            ? resp.items.stream()
                            .map(item -> new YouTubeVideoDto(
                                    item.id.videoId,
                                    item.snippet != null ? item.snippet.title : "Unknown Title",
                                    item.snippet != null ? item.snippet.channelTitle : "Unknown Channel"))
                            .collect(Collectors.toList())
                            : List.of();

                    if (!list.isEmpty()) {
                        String serialized = list.stream()
                                .map(v -> v.videoId + "|" + v.title + "|" + v.channelTitle)
                                .collect(Collectors.joining(";"));
                        cacheRepo.save(new YouTubeCache(cacheKey, serialized));
                        System.out.println("✅ Cached Video Details for " + list.size() + " videos");
                    }

                    return list;
                })
                .onErrorResume(err -> {
                    System.err.println("YouTube details error: " + err.getMessage());
                    return Mono.just(List.of());
                });
    }

    /** 🧩 Helper — parse cached details back into DTOs */
    private List<YouTubeVideoDto> parseVideoDetails(String responseJson) {
        return Arrays.stream(responseJson.split(";"))
                .map(entry -> {
                    String[] parts = entry.split("\\|");
                    if (parts.length < 3) return null;
                    return new YouTubeVideoDto(parts[0], parts[1], parts[2]);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
