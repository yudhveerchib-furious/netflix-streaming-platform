package com.netflix.content_service.service;

import com.netflix.content_service.dto.MovieRequest;
import com.netflix.content_service.dto.MovieResponse;
import com.netflix.content_service.model.Genre;
import com.netflix.content_service.model.Movie;
import com.netflix.content_service.model.VideoStatus;
import com.netflix.content_service.repository.ContentRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentService {

        /*
    Add a new movie to the catalog
    yet movie is not at uploaded stage
     */


    @Autowired
    private ContentRepository contentRepository;

    public MovieResponse addMovie(@Valid MovieRequest request) {
      log.info("adding an new movie: {}", request.getTitle());

        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRating(request.getRating());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie savedMovie = contentRepository.save(movie);
        log.info("added movie: {}", savedMovie.getId());

        return mapToMovieResponse(savedMovie);

    }

    private MovieResponse mapToMovieResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setGenre(movie.getGenre());
        response.setDirector(movie.getDirector());
        response.setCast(movie.getCast());
        response.setReleaseYear(movie.getReleaseYear());
        response.setRating(movie.getRating());
        response.setThumbnailUrl(movie.getThumbnailUrl());
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setVideoKey(movie.getVideoKey());
        response.setVideoStatus(movie.getVideoStatus());
        response.setHlsUrl(movie.getHlsUrl());
        response.setCreatedAt(movie.getCreatedAt());

        return response;
    }

    public void updateVideoKey(String movieId,String videoKey) {
        log.info("updating video key for movie: {}", movieId);
        Movie movie = contentRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("movie not found"));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        contentRepository.save(movie);
    }

    public void updateHlsUrl(String movieId,String hlsUrl) {
        log.info("updating hls url for movie: {}", movieId);
        Movie movie = contentRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("movie not found"));

        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        contentRepository.save(movie);

        log.info("updated hls url for movie: {}", movieId);
    }

    public List<MovieResponse> getAllMovies() {
       return contentRepository.findAll()
               .stream()
               .map(this::mapToMovieResponse)
               .collect(Collectors.toList());
    }

    public MovieResponse getMovieById(String id) {
        Movie movie = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("movie not found"));

        return  mapToMovieResponse(movie);
    }

    public List<MovieResponse> getMovieByGenre(Genre genre) {
        return contentRepository.findByGenre(genre)
                .stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> searchMovies(String title) {
        return contentRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }
}
