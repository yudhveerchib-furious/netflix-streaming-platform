package com.netflix.video_service.controller;

import com.netflix.video_service.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
public class VideoController {

    @Autowired
    private VideoService videoService;

    /*
    upload video file for a movie
     */

    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("video upload req for movie: {}", movieId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is empty");
        }

        String videoKey = videoService.uploadVideo(movieId, file);
        return ResponseEntity.ok("uploaded in success" + videoKey + "encoding started by kafka");
    }
}