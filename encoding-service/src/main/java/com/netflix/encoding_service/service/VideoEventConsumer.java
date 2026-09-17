package com.netflix.encoding_service.service;

import com.netflix.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService encodingService;

    /*
    Listens to video.uploaded kafka topic.
    triggered when video service uploads a raw video to s3

       * Video Service -> S3 upload -> Kafka (video.uploaded)
 *                         -> This Consumer
 *                         -> EncodingService -> FFmpeg -> S3
 *                         -> Kafka (video.encoded)

     */

    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group"
    )
    public void consumeVideoUploadedEvent(VideoUploadedEvent event) {

       log.info("Consumed VideoUploadedEvent for movie: {} file : {}",
               event.getMovieId(),
               event.getOriginalFileName());

       try {
            encodingService.encodeVideo(event);

       }catch (Exception e){
           log.error("failed to process encoding for movie : {} - {} " ,
                   event.getMovieId(),
                   e.getMessage());
       }

    }

}
