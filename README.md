# 🎬 Netflix-Style Video Streaming Platform

A Netflix-inspired video streaming platform built using **Spring Boot Microservices**.

The project is being developed to understand how a real-world video streaming system can be designed using microservices, databases, caching, event-driven communication, cloud storage, and video processing.

---

## 🚧 Project Status

The project is currently under development.

### Completed

- ✅ Content Service
- ✅ Video Service
- ✅ Encoding Service
- ✅ PostgreSQL integration
- ✅ AWS S3 video storage
- ✅ Apache Kafka event communication
- ✅ FFmpeg video encoding
- ✅ HLS generation
- ✅ Multiple video quality generation
- ✅ Master HLS playlist generation
- ✅ Temporary file cleanup

### In Progress / Planned

- ✅ Streaming Service
- ✅ Redis caching
- ✅ Complete end-to-end streaming workflow
- 🚧 Frontend video player

---

# 🏗️ Project Structure

The project is organized into multiple microservices:

```text
netflix/
│
├── content-service/
├── encoding-service/
├── streaming-service/
├── video-service/
│
├── docker-compose.yml
└── README.md
Services

Content Service
Manages movie metadata and catalog information.

Video Service
Handles video uploads, stores original videos in AWS S3, and publishes video upload events to Kafka.

Encoding Service
Consumes video upload events, downloads videos from S3, processes them using FFmpeg, generates HLS files in multiple qualities, uploads the encoded files back to S3, and publishes encoding completion events.

Streaming Service
Handles video streaming, generates presigned S3 URLs, serves signed HLS playlists, and uses Redis for caching.

⚙️ Infrastructure Setup

The project uses Docker to run the required infrastructure services locally.

The current setup includes:

PostgreSQL – Stores structured movie/catalog information.
Redis – Used for caching frequently accessed data such as streaming information.
Apache Kafka – Used for asynchronous communication and event streaming between microservices.
ZooKeeper – Used for Kafka coordination in the current Kafka setup.
AWS S3 – Used for storing original videos and HLS streaming content.
FFmpeg – Used for video processing and HLS generation.

The infrastructure is configured using:

docker-compose.yml
🎥 Content Service

The Content Service manages the movie catalog and stores movie-related metadata in PostgreSQL.

Current Responsibilities
Create movies
Retrieve movies
Retrieve movies by ID
Search movies by title
Retrieve movies by genre
Track video processing status
Store S3 video references
Store HLS streaming URLs
📦 Content Service Package Structure
content-service/
└── src/
    └── main/
        └── java/
            └── com.netflix.content_service/
                │
                ├── controller/
                │   └── ContentController
                │
                ├── dto/
                │   ├── MovieRequest
                │   └── MovieResponse
                │
                ├── model/
                │   ├── Movie
                │   ├── Genre
                │   └── VideoStatus
                │
                ├── repository/
                │   └── ContentRepository
                │
                └── service/
                    └── ContentService
🎬 Movie Model

The Movie entity represents movie information stored in PostgreSQL.

It currently contains fields for:

Movie ID
Title
Description
Genre
Director
Cast
Release Year
Rating
Thumbnail URL
Duration
S3 Video Key
HLS URL
Video Status
Created At
Updated At

The entity uses JPA/Hibernate for database persistence.

Example:

@Entity
@Table(name = "movies")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes;

    // S3 key for the original video
    private String videoKey;

    // HLS master playlist URL
    private String hlsUrl;

    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
📦 DTOs

The Content Service contains:

MovieRequest

Used for receiving movie information from the client when creating movie data.

MovieResponse

Used to return movie information from the API to the client.

DTOs help keep the API layer separate from the database entity.

🗃️ Database

The Content Service uses PostgreSQL to store movie metadata.

The movie entity is mapped to the:

movies

table.

Large video files are not stored inside PostgreSQL.

Instead:

Movie Metadata → PostgreSQL

Video Files → AWS S3

The database stores references to the video files using the S3 object key and HLS URL.

🌐 Content Service API Routes

Base URL:

/api/v1/movies
Add a Movie
POST /api/v1/movies

Creates a new movie in the catalog.

The movie is initially assigned:

VideoStatus.PENDING
Get All Movies
GET /api/v1/movies

Returns all movies available in the catalog.

Get Movie by ID
GET /api/v1/movies/{movieId}

Example:

GET /api/v1/movies/abc123
Get Movies by Genre
GET /api/v1/movies/genre/{genre}

Example:

GET /api/v1/movies/genre/SCI_FI
Search Movies by Title
GET /api/v1/movies/search?title={title}

Example:

GET /api/v1/movies/search?title=inception

The search uses case-insensitive partial title matching.

For example, searching for:

dark

can return:

The Dark Knight
🔄 Video Status Flow

The Content Service tracks the current state of a movie's video.

PENDING
   │
   │ Video uploaded to S3
   ▼
UPLOADED
   │
   │ Video encoded and HLS generated
   ▼
READY
PENDING

The movie has been added to the catalog, but the video has not been uploaded yet.

UPLOADED

The original video has been uploaded to AWS S3.

The S3 object key is stored in:

videoKey
READY

The video has been processed into HLS and is ready for streaming.

The HLS master playlist URL is stored in:

hlsUrl
🎥 Video Service

The Video Service is responsible for handling original video uploads.

Its main responsibilities are:

Receive video files from clients
Generate unique S3 object keys
Upload original videos to AWS S3
Publish VideoUploadedEvent to Kafka
Trigger the Encoding Service asynchronously through Kafka
📦 Video Service Structure
video-service/
└── src/
    └── main/
        └── java/
            └── com.netflix.video_service/
                │
                ├── config/
                │   ├── KafkaConfig
                │   └── S3Config
                │
                ├── controller/
                │   └── VideoController
                │
                ├── event/
                │   └── VideoUploadedEvent
                │
                └── service/
                    └── VideoService
📤 Video Upload API
POST /api/v1/videos/upload/{movieId}

The video is sent as a multipart file.

Example:

/api/v1/videos/upload/abc123

The request contains:

file = movie.mp4

The VideoController receives the file and passes it to the VideoService.

☁️ Uploading Videos to AWS S3

The Video Service generates a unique S3 key for every uploaded video.

Format:

raw/{movieId}/{uuid}_{filename}

Example:

raw/abc123/550e8400-e29b-41d4-a716-446655440000_inception.mp4

This prevents filename conflicts when multiple videos are uploaded.

The original video is then uploaded to:

AWS S3
📨 VideoUploadedEvent

After successfully uploading the video to S3, the Video Service publishes a Kafka event.

Topic:

video.uploaded

Event structure:

public class VideoUploadedEvent {

    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private long fileSizeBytes;
}

The event contains the information required by the Encoding Service to process the uploaded video.

🔄 Video Upload Flow
Client
   │
   │ Upload video
   ▼
Video Controller
   │
   ▼
Video Service
   │
   │ Upload original video
   ▼
AWS S3
   │
   │ Publish event
   ▼
Kafka
   │
   │ video.uploaded
   ▼
Encoding Service
🎞️ Encoding Service

The Encoding Service is responsible for processing uploaded videos.

It consumes:

video.uploaded

from Kafka.

It then downloads the original video from S3 and uses FFmpeg to encode the video into multiple qualities.

📦 Encoding Service Structure
encoding-service/
└── src/
    └── main/
        └── java/
            └── com.netflix.encoding_service/
                │
                ├── config/
                │   └── S3Config
                │
                ├── event/
                │   ├── VideoUploadedEvent
                │   └── VideoEncodedEvent
                │
                ├── service/
                │   ├── EncodingService
                │   └── VideoEventConsumer
                │
                └── EncodingServiceApplication
📨 Video Event Consumer

The Encoding Service contains a Kafka consumer that listens to:

video.uploaded

When the event is received, the Encoding Service starts the encoding process.

Kafka
   │
   │ VideoUploadedEvent
   ▼
VideoEventConsumer
   │
   ▼
EncodingService
🎬 Video Encoding Pipeline

The Encoding Service follows this pipeline:

1. Receive VideoUploadedEvent
              │
              ▼
2. Download raw video from S3
              │
              ▼
3. Encode video using FFmpeg
              │
              ▼
4. Generate HLS playlists
              │
              ▼
5. Generate master playlist
              │
              ▼
6. Upload encoded files to S3
              │
              ▼
7. Publish VideoEncodedEvent
🎚️ Video Qualities

The Encoding Service generates multiple video qualities.

Current configurations:

Resolution  Bitrate
1920x1080   5000 kbps
1280x720    2800 kbps
854x480 1200 kbps
640x360 800 kbps

This allows the streaming system to provide different quality levels depending on the client's network and device.

⚙️ FFmpeg

FFmpeg is used to process and encode the uploaded video.

For each quality, FFmpeg:

Resizes the video
Encodes the video using H.264
Encodes audio using AAC
Splits the video into HLS segments
Generates an .m3u8 playlist

Example output:

1080p/
├── playlist.m3u8
├── segment_000.ts
├── segment_001.ts
└── segment_002.ts
📺 HLS

The Encoding Service converts the original video into HTTP Live Streaming (HLS) format.

Each quality contains:

playlist.m3u8

and multiple:

segment_XXX.ts

files.

Example:

encoded/
│
├── 1080p/
│   ├── playlist.m3u8
│   ├── segment_000.ts
│   ├── segment_001.ts
│   └── ...
│
├── 720p/
│   ├── playlist.m3u8
│   └── ...
│
├── 480p/
│   ├── playlist.m3u8
│   └── ...
│
├── 360p/
│   ├── playlist.m3u8
│   └── ...
│
└── master.m3u8
🎯 Master Playlist

The Encoding Service generates a master:

master.m3u8

The master playlist contains information about all available video qualities.

Conceptually:

master.m3u8
      │
      ├── 1080p/playlist.m3u8
      ├── 720p/playlist.m3u8
      ├── 480p/playlist.m3u8
      └── 360p/playlist.m3u8

The video player can use the master playlist to select the appropriate quality.

☁️ Encoded Files in S3

After encoding, the generated HLS files are uploaded back to AWS S3.

The encoded files use the following prefix:

encoded/{movieId}/

Example:

encoded/abc123/
│
├── master.m3u8
│
├── 1080p/
│   ├── playlist.m3u8
│   ├── segment_000.ts
│   └── segment_001.ts
│
├── 720p/
├── 480p/
└── 360p/
📨 VideoEncodedEvent

After successfully completing the encoding process, the Encoding Service publishes:

video.encoded

to Kafka.

The event contains information such as:

Movie ID
HLS URL
Master playlist key
Encoding status
Error message, if encoding failed

Example:

VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
        event.getMovieId(),
        hlsUrl,
        masterPlayListKey,
        true,
        null
);
🔄 Complete Video Processing Flow

The currently implemented pipeline is:

                    VIDEO UPLOAD
                         │
                         ▼
                    ┌─────────┐
                    │ Client  │
                    └────┬────┘
                         │
                         ▼
                ┌─────────────────┐
                │  Video Service  │
                └────────┬────────┘
                         │
                         │ Upload
                         ▼
                   ┌───────────┐
                   │  AWS S3   │
                   │ Raw Video │
                   └─────┬─────┘
                         │
                         │ video.uploaded
                         ▼
                    ┌─────────┐
                    │  Kafka  │
                    └────┬────┘
                         │
                         ▼
              ┌────────────────────┐
              │  Encoding Service  │
              └─────────┬──────────┘
                        │
                        ▼
                    ┌────────┐
                    │ FFmpeg │
                    └────┬───┘
                         │
                         ▼
                  HLS Generation
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
        1080p           720p           480p
          │              │              │
          └──────────────┼──────────────┘
                         │
                         ▼
                    master.m3u8
                         │
                         ▼
                   ┌───────────┐
                   │  AWS S3   │
                   │ HLS Files │
                   └─────┬─────┘
                         │
                         │ video.encoded
                         ▼
                    ┌─────────┐
                    │  Kafka  │
                    └────┬────┘
                         │
                         ▼
                 Streaming Service
                    (Planned)
🧹 Temporary File Cleanup

The Encoding Service temporarily stores videos and encoded HLS files on the local filesystem while FFmpeg processes them.

Example:

/tmp/encoding/{movieId}/
│
├── raw_video.mp4
│
└── encoded/
    ├── master.m3u8
    ├── 1080p/
    ├── 720p/
    ├── 480p/
    └── 360p/

After encoding completes or fails, the temporary files are cleaned up.

This prevents encoded videos from continuously consuming local disk space.

🔁 Event-Driven Architecture

Kafka is used for asynchronous communication between the Video Service and Encoding Service.

Video Upload Event
Video Service
      │
      │ VideoUploadedEvent
      ▼
Kafka
      │
      │ video.uploaded
      ▼
Encoding Service
Encoding Completion Event
Encoding Service
      │
      │ VideoEncodedEvent
      ▼
Kafka
      │
      │ video.encoded
      ▼
Other Services

This keeps the video upload and encoding processes loosely coupled.

The Video Service does not need to directly call the Encoding Service.

☁️ Video Metadata

The Content Service stores references to video files instead of storing the actual video in PostgreSQL.

Original Video
videoKey

Example:

raw/inception/abc123_inception.mp4
HLS Master Playlist
hlsUrl

Example:

https://bucket-name.s3.amazonaws.com/encoded/inception/master.m3u8
🔧 Service Architecture

The project follows a microservices architecture:

                    ┌──────────────────┐
                    │      Client      │
                    └────────┬─────────┘
                             │
                 ┌───────────┴───────────┐
                 │                       │
                 ▼                       ▼
        ┌─────────────────┐     ┌─────────────────┐
        │ Content Service │     │  Video Service  │
        └────────┬────────┘     └────────┬────────┘
                 │                       │
                 ▼                       ▼
           PostgreSQL                 AWS S3
                                         │
                                         ▼
                                       Kafka
                                         │
                                         ▼
                              ┌────────────────────┐
                              │  Encoding Service  │
                              └─────────┬──────────┘
                                        │
                                        ▼
                                     FFmpeg
                                        │
                                        ▼
                                  HLS Generation
                                        │
                                        ▼
                                     AWS S3
                                        │
                                        ▼
                                      Kafka
                                        │
                                        ▼
                              ┌────────────────────┐
                              │  Streaming Service │
                              │      (Planned)     │
                              └────────────────────┘
🔮 Planned Streaming Service

The Streaming Service will be responsible for handling video playback.

Planned responsibilities include:

Providing HLS streaming URLs
Serving video playback requests
Integrating with Redis for caching
Providing movie/video playback information to clients

Planned flow:

Client
   │
   ▼
Streaming Service
   │
   ▼
Redis
   │
   ▼
HLS Master Playlist
   │
   ▼
AWS S3
🛠️ Technologies
Backend
Java
Spring Boot
Spring Data JPA
Hibernate
Database
PostgreSQL
Redis
Messaging
Apache Kafka
ZooKeeper
Cloud Storage
AWS S3
Video Processing
FFmpeg
HLS
.m3u8
MPEG-TS .ts segments
Infrastructure
Docker
Docker Compose
📌 Current Development Flow

The current implementation can be summarized as:

Movie Creation
      │
      ▼
Content Service
      │
      │
      ▼
Video Upload
      │
      ▼
Video Service
      │
      ▼
AWS S3
      │
      ▼
Kafka
(video.uploaded)
      │
      ▼
Encoding Service
      │
      ▼
FFmpeg
      │
      ▼
HLS
      │
      ▼
AWS S3
      │
      ▼
Kafka
(video.encoded)
      │
      ▼
Streaming Service
   (Next Phase)
🚀 Future Improvements

Planned improvements include:

 Streaming Service
 Redis caching
 Continue watching
 Adaptive bitrate streaming
 Monitoring and logging
 Cloud deployment
🎯 Project Goal

The goal of this project is to understand how a production-style video streaming platform can be designed using:

Microservices
REST APIs
PostgreSQL
Redis
Apache Kafka
Event-driven architecture
AWS S3
FFmpeg
HLS
Docker
Asynchronous processing