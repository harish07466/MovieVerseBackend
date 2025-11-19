package com.MovieVerseBackEnd.DTOs;


public class YouTubeVideoDto {
    public String videoId;
    public String title;
    public String channelTitle;
    public String thumbnailUrl;
    public String embedUrl;

    public YouTubeVideoDto() {}

    public YouTubeVideoDto(String videoId, String title, String channelTitle) {
        this.videoId = videoId;
        this.title = title;
        this.channelTitle = channelTitle;
        this.thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        this.embedUrl = "https://www.youtube.com/embed/" + videoId;
    }
}



