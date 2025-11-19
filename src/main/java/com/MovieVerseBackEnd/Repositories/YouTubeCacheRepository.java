package com.MovieVerseBackEnd.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MovieVerseBackEnd.Entites.YouTubeCache;

import java.util.Optional;

@Repository
public interface YouTubeCacheRepository extends JpaRepository<YouTubeCache, Long> {
    Optional<YouTubeCache> findByQuery(String query);
}

