package com.netflix.encoding_service.service;


import com.netflix.encoding_service.event.VideoEncodedEvent;
import com.netflix.encoding_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

  private final S3Client s3Client;
  private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Value("${ffmpeg.path}")
  private String ffmpegPath;

  private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

  @Value("${encoding.base-path}")
  private String basePath;


  //Video qualities to encode
    // format : resolution, bitrate,height

    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920,5000,1080}, // 1000p-5000k bitrate
            new int[]{1280,2800,720},
            new int[]{854,1200,480},
            new int[]{640,800,360}
    );
/*
Main encoding pipeline
steps -
1:  Download raw video from s3
2:  encode to multiple qualities using ffmpeg
3:  generate hsl playlist (.m3u8) for each quality
4:  Create master playlist
5:  upload all encode files back to S3
6:  publish video encoded event to kafka
 */

    public void encodeVideo(VideoUploadedEvent event) throws IOException {
       log.info("starting encoding of video for movie: {}", event.getMovieId());


       String jobPath = basePath + "/" + event.getMovieId(); // simple one

        try{
            //create temp dirs
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            // s1: download raw video from s3
            String localVideoPath = jobPath + "/raw_video.mp4";

            downloadFromS3(event.getVideoKey(), localVideoPath);
            log.info("finished downloading of video to : {}", localVideoPath);

            //s2 and s3  encode to multiple qualities and generate hls

            for(int[] qualities: VIDEO_QUALITIES){
                int width = qualities[0];
                int bitrate = qualities[1];
                int height =  qualities[2];

                String qualityDir =
                        jobPath + "/encoded/" + height + "p";

                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, width,height,bitrate);

                log.info("finished encoding of video of height: {}", height);

                //s4: generate master playlist
                String masterPlayListPath = jobPath + "/encoded/master.m3u8";
                generateMasterPlayList(masterPlayListPath);

                log.info("Master playlist generated");

                //s5: upload all resources file to s3
                String encodedPrefix = "encoded/" + event.getMovieId() + "/";
                uploadEncodedFilesToS3(jobPath + "/encoded", encodedPrefix);
                log.info("finished encoding of video of encoded prefix: {}", encodedPrefix);

                //s6: publish video encoded event
                String masterPlayListKey =
                        encodedPrefix + "master.m3u8";

                String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlayListKey;

                VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
                        event.getMovieId(),
                        hlsUrl,
                        masterPlayListKey,
                        true,
                        null
                );

                kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), videoEncodedEvent);
                log.info("VideoEncodedEvent published " +
                        "for movie: {}", event.getMovieId());

            }

        }catch(Exception e){
             log.error("failed to encode video for movie: {}", event.getMovieId(), e);

            //publish failure event
            VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage()
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC,event.getMovieId() , videoEncodedEvent);

        }finally {
            cleanUpTempFiles(jobPath);
        }
    }


    private void cleanUpTempFiles(String jobPath) {
        try {
            Path dirPath = Paths.get(jobPath);

            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                log.info("Temp files cleaned up for job: {}", jobPath);
            }
        }
        catch (IOException e) {
            log.warn("Failed to cleanup temp files: {}", e.getMessage());
        }
    }

    private void uploadEncodedFilesToS3(String localDir, String s3Prefix) {
        File dir = new File(localDir);
        uploadDirToS3(dir, localDir, s3Prefix);

    }

    private void uploadDirToS3(File dir, String baseDir, String s3Prefix) {

         for(File file: dir.listFiles()){
             if(file.isDirectory()){
                 uploadDirToS3(file, baseDir, s3Prefix);
             }else {
                 String relativePath = file.getAbsolutePath()
                         .substring(baseDir.length() + 1)
                         .replace("\\", "/");

                 String s3Key = s3Prefix + relativePath;

                 String contentType = file.getName().endsWith(".m3u8")
                         ? "application/x-mpegURL"
                         : "video/MP2T";

                 PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                         .bucket(bucketName)
                         .key(s3Key)
                         .contentType(contentType)
                         .build();



             }
         }
    }

    // download file from s3 to local path
    private void downloadFromS3(String s3key, String localVideoPath) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().
                        bucket(bucketName)
                        .key(s3key)
                        .build();

        s3Client.getObject(getObjectRequest, Paths.get(localVideoPath));
    }


    private void encodeToHLS(String inputPath, String outputDir, int width, int height, int bitrate) throws IOException,InterruptedException {

        String playListPath =
                outputDir + "/playlist.m3u8";
        String segmentPattern =
                outputDir + "/segment_%03d.ts";


        //FFmpeg Command for hlsEncoding

        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-b:v", bitrate + "k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playListPath
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "FFmpeg encoding failed with exit code: " + exitCode
            );
        }
    }


    //Generate aster hls playlist that references all quality playlists
    private void generateMasterPlayList(String masterPlayListPath) throws IOException {
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n"); // extended m3u8 playlist
        master.append("#EXT-X-VERSION:3\n\n");


        //add each quality to playlist

        int[][] qualities =
                {{1920, 5000, 1080},
                {1280, 2800, 720},
                {854, 1200, 480},
                {640, 800, 360}};

        for (int[] q : qualities) {
            int width = q[0];
            int bitrate = q[1];
            int height = q[2];

            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate * 1000)
                    .append(", RESOLUTION=").append(width).append("x").append(height)
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"\n");

            master.append(height).append("p/playlist.m3u8\n\n");

        }

        Files.writeString(Paths.get(masterPlayListPath), master.toString());


    }


}
