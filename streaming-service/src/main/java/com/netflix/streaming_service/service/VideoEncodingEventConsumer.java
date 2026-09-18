package com.netflix.streaming_service.service;

import com.netflix.streaming_service.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodingEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    /*
     Listens to video.encoded kafka topic
     Stores master playlist key in the redis when encoding is complete
     This allows streaming service to quickly find the playlist key by moviedId
     */
    @KafkaListener(
            topics = "video.encoded",
            groupId = "streaming.service-group"
    )
    public void consumeVideoEncodedEvent(VideoEncodedEvent videoEncodedEvent) {
         log.info("Consumed video encoded event for movie: {}", videoEncodedEvent.getMovieId());

         if(videoEncodedEvent.isSuccess()) {
             //store master playlist key in redis
             String cacheKey =
                     MASTER_PLAYLIST_KEY_PREFIX + videoEncodedEvent.getMovieId();

             redisTemplate.opsForValue()
                     .set(cacheKey,videoEncodedEvent.getMasterPlaylistKey());

             log.info("master playlist key stored in redis for movie : {}", videoEncodedEvent.getMovieId());

         }else {
             log.error("Encoding failed for movie: {}" ,
                     videoEncodedEvent.getMovieId());
         }

    }


}
