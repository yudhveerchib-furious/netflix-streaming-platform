package com.netflix.streaming_service.controller;


import com.netflix.streaming_service.dto.StreamingResponse;
import com.netflix.streaming_service.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stream")
@Slf4j
@RequiredArgsConstructor
public class StreamingController {

  private final StreamingService streamingService;
  private final RedisTemplate<String, String> redisTemplate;

  private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

   /*
     Get streaming url for a movie
     Returns presigned HLS master playlist url
   */

    @GetMapping("/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(@PathVariable String movieId) {
        log.info("Getting streaming url for movie id {}", movieId);

        //get master playlist key from redis
        String playListKey = redisTemplate.opsForValue()
                .get(MASTER_PLAYLIST_KEY_PREFIX + movieId);

        if(playListKey == null) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponse response =  streamingService.getStreamingUrl(movieId,playListKey);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{movieId}/playlist")
    public ResponseEntity<String> getSignedPlayList(
            @PathVariable String movieId,
            @RequestParam String path
    ) {
     String singedPlayList =
             streamingService.getSignedPlaylist(movieId, path);

     return ResponseEntity.ok().header("Content-Type", "application/x-mpegURL").body(singedPlayList);
    }
}
