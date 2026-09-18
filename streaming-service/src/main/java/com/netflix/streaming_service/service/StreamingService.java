package com.netflix.streaming_service.service;

import com.netflix.streaming_service.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final RedisTemplate<String,String> redisTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry; // 60 minutes

    //redis key for caching streaming URLS
    private final static String STREAMING_URL_CACHE_PREFIX =
            "streaming:url:";

    /*
    Get streaming url for a movie

    1. check redis cache for existing presigned url
    2. if cached - return immediately
    3. if not cached = generate new presigned url from s3
    4. cache the url in redis
    5. return streaming url
     */

    public StreamingResponse getStreamingUrl(String movieId, String playListKey) {
          log.info("Getting Streaming url for movie: {}", movieId);

          String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;

          //CHECK redis

        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if(cachedUrl != null){
            log.info("Returning Streaming url for movie: {}", movieId);
            return new StreamingResponse(
                movieId,
                cachedUrl,
                "1080p, 720p, 480p, 360p",
                presignedUrlExpiry
            );
        }

        //Generate presigned url from s3
        log.info("generate new preSigned url for movie: {}", movieId);
        String presignedUrl =
                generatePresignedUrl(playListKey);

        //cache in redis for 55 minutes
        //5 mins less than actual expiry to avoid edge cases
        redisTemplate.opsForValue()
                .set(
                        cacheKey,
                        presignedUrl,
                        55,
                        TimeUnit.MINUTES
                );
         log.info("Streaming url generated and cached for movie: {}", movieId);
         return new StreamingResponse(
                 movieId,
                 presignedUrl,
                 "1080p, 720p, 480p, 360p",
                 presignedUrlExpiry
         );

    }

    /*
    Generate a presigned url for s3 object,
    url expired after configure time
     */

    private String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().
                        bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    /*
    invalidate the cache streaming url
    called when video is re-encoded or updated
     */

    public void invalidateCache(String movieId) {
        String cacheKey =
                STREAMING_URL_CACHE_PREFIX + movieId;

        redisTemplate.delete(cacheKey);
        log.info("Streaming url cached invalidated for movie: {}", movieId);



    }


    private String rewriteM3U8ContentUrls(String m3u8Content,
                                          String basePath) {
        StringBuilder sb = new StringBuilder();

        for(String line: m3u8Content.split("\n")){
            String trimmed =
                           line.trim();

            if(trimmed.isEmpty() || trimmed.startsWith("#")) {
                sb.append(line).append("\n");
                continue;
            }

            // this is the segment or playlist reference build full s3 key and sign it

            String fullKey = basePath + trimmed;
            String signedUrl = generatePresignedUrl(fullKey);

            sb.append(signedUrl).append("\n");
        }
        return sb.toString();
    }

    /*
     this is the key method that makes everything secure
     */

    public String getSignedPlaylist(String movieId, String playListPath) {
        //get base path for this playlist

        String basePath = playListPath.substring(0,
                playListPath.lastIndexOf('/') + 1);

        //read m3u8 content from s3

        String m3u8Content = readFromS3(playListPath);

        //rewrite each line that is a segment or a playlist reference
        String signedContent = rewriteM3U8ContentUrls(
                m3u8Content,
                basePath
        );

        return signedContent;
    }

    private String readFromS3(String playListPath) {
        GetObjectRequest request =
                GetObjectRequest.builder()
                .bucket(bucketName)
                        .key(playListPath)
                        .build();

        ResponseInputStream<GetObjectResponse> response =
                s3Client.getObject(request);

        return new BufferedReader(new InputStreamReader(response))
                .lines().collect(Collectors.joining("\n"));
    }

}