package com.MovieVerseBackEnd.Controllers;

import com.MovieVerseBackEnd.DTOs.MovieDetailDto;
import com.MovieVerseBackEnd.DTOs.MovieMediaResponse;
import com.MovieVerseBackEnd.DTOs.MovieSummaryDto;
import com.MovieVerseBackEnd.DTOs.YouTubeVideoDto;
import com.MovieVerseBackEnd.Services.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@CrossOrigin(
	    origins = "http://localhost:5173",
	    allowedHeaders = "*",
	    allowCredentials = "true"
	)
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private static final int DEFAULT_SONG_LIMIT = 10;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // ========================= 🎬 SEARCH MOVIES ==============================
    /**
     * Search movies by keyword (used by search bar)
     * Example: /api/movies/search?q=avengers
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<List<MovieSummaryDto>>> search(@RequestParam String q) {
        return movieService.searchMovies(q)
                .map(ResponseEntity::ok)
                .onErrorResume(err -> {
                    err.printStackTrace();
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }

    // ========================= 🎥 MOVIE DETAILS ==============================
    /**
     * Get full movie details by IMDb ID
     * Example: /api/movies/tt0848228
     */
    @GetMapping("/{imdbId}")
    public Mono<ResponseEntity<MovieDetailDto>> getMovie(@PathVariable String imdbId) {
        return movieService.getMovieById(imdbId)
                .map(ResponseEntity::ok)
                .onErrorResume(err -> {
                    err.printStackTrace();
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }

    // ========================= 🎵 MOVIE MEDIA (TRAILER + SONGS) ==============================
    /**
     * GET /api/movies/{imdbId}/media?lang={lang}&songLimit={n}
     *
     * Returns:
     *  - originalLanguage : detected from OMDb
     *  - selectedLanguage : user-selected (fallbacks to original)
     *  - trailerEmbedUrl  : YouTube embeddable trailer
     *  - songs[]          : Accurate filtered songs with title, channel, and thumbnail
     *
     * Examples:
     *   /api/movies/tt0848228/media
     *   /api/movies/tt0848228/media?lang=kannada
     *   /api/movies/tt0848228/media?lang=hindi&songLimit=5
     */
    @GetMapping("/{imdbId}/media")
    public Mono<ResponseEntity<MovieMediaResponse>> getMedia(
            @PathVariable String imdbId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false, defaultValue = "10") int songLimit
    ) {
        return movieService.getMovieById(imdbId)
                .flatMap((MovieDetailDto movie) -> {
                    // 🧠 Determine original and selected language
                    String originalLanguage = movieService.extractOriginalLanguage(movie);
                    String selectedLanguage = (lang == null || lang.isBlank())
                            ? originalLanguage
                            : lang.trim();

                    // 🎬 Parallel reactive calls for trailer + songs
                    Mono<String> trailerMono = movieService.findTrailerEmbedUrl(
                            movie.Title, movie.Year, selectedLanguage)
                        .defaultIfEmpty(""); // handle no trailer gracefully

                    Mono<List<YouTubeVideoDto>> songsMono = movieService.findSongsWithDetails(
                            movie.Title, movie.Year, selectedLanguage, songLimit)
                        .defaultIfEmpty(List.of()); // handle empty list

                    return Mono.zip(trailerMono, songsMono)
                        .map(tuple -> {
                            MovieMediaResponse response = new MovieMediaResponse();
                            response.originalLanguage = originalLanguage;
                            response.selectedLanguage = selectedLanguage;
                            response.trailerEmbedUrl = tuple.getT1().isBlank() ? null : tuple.getT1();
                            response.songs = tuple.getT2();
                            return ResponseEntity.ok(response);
                        })
                        .onErrorResume(err -> {
                            err.printStackTrace();
                            return Mono.just(ResponseEntity.status(500).build());
                        });

                })
                .onErrorResume(err -> {
                    err.printStackTrace();
                    return Mono.just(ResponseEntity.status(500).build());
                });
    }
}
