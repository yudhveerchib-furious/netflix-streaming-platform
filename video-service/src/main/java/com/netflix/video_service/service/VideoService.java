package com.netflix.video_service.service;

import com.netflix.video_service.event.VideoUploadedEvent;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    @Autowired
    private final S3Client s3Client;

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";

    /**
     * Upload video to AWS S3 and publish VideoUploadedEvent to Kafka

     * FLOW:
     * 1. Receive multipart video file
     * 2. Generate unique S3 key
     * 3. Upload to S3
     * 4. Publish VideoUploadedEvent to Kafka
     * 5. Encoding Service picks up and start FFmpeg
     */

    public String uploadVideo(String movieId, MultipartFile file) throws IOException {
       log.info("video uploaded topic: {}", VIDEO_UPLOADED_TOPIC);
       log.info("Starting video upload for movie: {}", movieId);

       //generate unique s3 key for raw video
        // format: raw/movieId/uuid_filename

        String videoKey = "raw/" + movieId + "/" +
                UUID.randomUUID() + "_" + file.getOriginalFilename();


       // build request to send video-file to S3

       PutObjectRequest putObjectRequest = PutObjectRequest.builder()
               .bucket(bucketName)
               .key(videoKey)
               .contentType(file.getContentType())
               .contentLength(file.getSize())
               .build();

       s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(),
               file.getSize()));

       log.info("video uploaded topic: {}", VIDEO_UPLOADED_TOPIC);


        //publish event to kafka
        //encoding Service will consume this and start FFMEG processing

        VideoUploadedEvent videoUploadedEvent = new VideoUploadedEvent(
                movieId,
                videoKey,
                bucketName,
                file.getOriginalFilename(),
                file.getSize()
        );

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC, movieId, videoUploadedEvent);
        log.info("video uploaded event for movie: {}", movieId);

        return videoKey;
    }
}
