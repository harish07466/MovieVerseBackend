package com.MovieVerseBackEnd.DTOs;

import java.util.List;

public class YouTubeSearchResponse {

    public List<Item> items;

    public static class Item {
        public Id id;
        public Snippet snippet;
    }

    public static class Id {
        public String kind;
        public String videoId;

        public Id() {}

        @com.fasterxml.jackson.annotation.JsonCreator
        public Id(String value) {
            this.videoId = value;
        }
    }

    public static class Snippet {
        public String title;
        public String channelTitle;
        public Thumbnails thumbnails;
    }

    public static class Thumbnails {
        public ThumbnailDetails high;
        public ThumbnailDetails medium;
        public ThumbnailDetails defaultThumbnail;
    }

    public static class ThumbnailDetails {
        public String url;
        public int width;
        public int height;
    }
}

