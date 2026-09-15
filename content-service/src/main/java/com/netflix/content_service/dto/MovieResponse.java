package com.netflix.content_service.dto;

import com.netflix.content_service.model.Genre;
import com.netflix.content_service.model.VideoStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private String id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes;
    private String videoKey;

    //hls master playlist url for streaming

    private String hlsUrl;
    private VideoStatus videoStatus;


    private LocalDateTime createdAt;


}
