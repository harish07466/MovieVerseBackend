package com.MovieVerseBackEnd.DTOs;

import java.util.List;

public class OmdbSearchResponse {
    public List<SearchItem> Search;
    public String totalResults;
    public String Response;
    public String Error;

    public static class SearchItem {
        public String Title;
        public String Year;
        public String imdbID;
        public String Type;
        public String Poster;
    }
}

