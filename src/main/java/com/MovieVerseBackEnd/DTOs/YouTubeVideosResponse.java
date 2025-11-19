package com.MovieVerseBackEnd.DTOs;

import java.util.List;

public class YouTubeVideosResponse {
    public List<VideoItem> items;

    public static class VideoItem {
        public String id;
        public Status status;
        public ContentDetails contentDetails;
    }

    public static class Status {
        public boolean embeddable;
        public String privacyStatus; // "public" expected
    }

    public static class ContentDetails {
        public RegionRestriction regionRestriction; // optional
    }

    public static class RegionRestriction {
        public List<String> blocked; // if present, region blocked list
        public List<String> allowed; // allowed list
    }
}
