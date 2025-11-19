package com.MovieVerseBackEnd.Clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.MovieVerseBackEnd.DTOs.MovieDetailDto;
import com.MovieVerseBackEnd.DTOs.MovieSummaryDto;
import com.MovieVerseBackEnd.DTOs.OmdbSearchResponse;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OmdbClient {

    private final WebClient webClient;
    private final String apiKey;

    public OmdbClient(WebClient webClient, @Value("${omdb.api.key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    public Mono<List<MovieSummaryDto>> search(String query) {
        String uri = String.format("https://www.omdbapi.com/?apikey=%s&s=%s", apiKey, query);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(OmdbSearchResponse.class)
                .map(resp -> {
                    if (resp != null && "True".equalsIgnoreCase(resp.Response) && resp.Search != null) {
                        return resp.Search.stream()
                                .map(s -> new MovieSummaryDto(s.imdbID, s.Title, s.Year, s.Poster))
                                .collect(Collectors.toList());
                    }
                    return List.of();
                });
    }

    public Mono<MovieDetailDto> getById(String imdbId) {
        String uri = String.format("https://www.omdbapi.com/?apikey=%s&i=%s&plot=full", apiKey, imdbId);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MovieDetailDto.class);
    }
}

