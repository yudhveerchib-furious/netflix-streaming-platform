# 🎬 Netflix-Style Video Streaming Platform

A Netflix-inspired video streaming platform built using **Spring Boot Microservices**.

The project is being developed to understand how a real-world video streaming system can be designed using microservices, databases, caching, event-driven communication, cloud storage, and video processing.

## 🚧 Project Status

The project is currently under development.

So far, I have completed the initial project setup and started building the **Content Service**, including the movie model, DTOs, controller, service, and repository layers.

---

## 🏗️ Project Structure

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
Content Service – Manages movie metadata and catalog information.
Video Service – Handles video-related operations and video uploads.
Encoding Service – Responsible for video processing and HLS generation using FFmpeg.
Streaming Service – Handles video streaming and provides streaming URLs.
⚙️ Infrastructure Setup

The project uses Docker to run the required infrastructure services locally.

The current setup includes:

PostgreSQL – Stores structured movie/catalog information.
Redis – Used for caching frequently accessed data such as streaming information.
Apache Kafka – Used for asynchronous communication and event streaming between microservices.
ZooKeeper – Used for Kafka coordination in the current Kafka setup.
AWS S3 – Used for storing video files and HLS streaming content.

The infrastructure is configured using docker-compose.yml.

🎥 Content Service

The Content Service is the first microservice currently being developed.

Its responsibility is to manage the movie catalog and store movie-related metadata in PostgreSQL.

Current Package Structure
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

The Movie entity represents the movie information stored in the database.

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

The Content Service currently contains:

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

The database stores information about the movie, while large video files are stored separately in AWS S3.

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

Get Movies by Genre
GET /api/v1/movies/genre/{genre}

Example:

GET /api/v1/movies/genre/SCI_FI

Returns all movies belonging to the specified genre.

Get Movie by ID
GET /api/v1/movies/{movieId}

Example:

GET /api/v1/movies/abc123

Returns a specific movie using its movie ID.

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
   │ Video processed into HLS
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
☁️ Video Metadata

The Content Service stores references to video files rather than storing the actual video inside PostgreSQL.

S3 Video Key

videoKey

Stores the S3 object key of the original video.

Example:

videos/inception/original.mp4
HLS URL

hlsUrl

Stores the URL of the HLS master playlist used for streaming.

Example:

https://cdn.example.com/videos/inception/master.m3u8
🔧 Content Service Layers

The Content Service follows a layered architecture:

Client
   │
   ▼
Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
PostgreSQL
Controller

Handles HTTP requests and responses.

Service

Contains the business logic and converts Movie entities into MovieResponse DTOs.

Repository

Uses Spring Data JPA to communicate with the PostgreSQL database.

🔮 Planned Architecture

The planned video flow is:

Client
   │
   ▼
Video Service
   │
   ▼
AWS S3
   │
   ▼
Kafka Event
   │
   ▼
Encoding Service
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
Streaming Service
   │
   ▼
Redis / HLS URL
   │
   ▼
Client

The remaining services and video-processing pipeline will be implemented incrementally.

🛠️ Technologies
Java
Spring Boot
Spring Data JPA
Hibernate
PostgreSQL
Redis
Apache Kafka
ZooKeeper
AWS S3
FFmpeg
HLS
Docker