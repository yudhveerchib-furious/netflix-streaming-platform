# 🎬 Netflix-Style Video Streaming Platform

A Netflix-inspired video streaming platform built using **Spring Boot Microservices**.

The project is being developed to understand how a real-world video streaming system can be designed using microservices, databases, caching, event-driven communication, cloud storage, and video processing.

## 🚧 Project Status

The project is currently under development.

So far, I have completed the initial project setup and started building the **Content Service**, including the movie model, DTOs, and controller structure.

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

MySQL – Stores structured movie/catalog information.
Redis – Used for caching frequently accessed data such as streaming information.
Apache Kafka – Used for asynchronous communication and event streaming between microservices.
ZooKeeper – Used for Kafka coordination in the current Kafka setup.
AWS S3 – Used for storing video files and HLS streaming content.

The infrastructure is configured using docker-compose.yml.

🎥 Content Service

The Content Service is the first microservice currently being developed.

Its responsibility is to manage the movie catalog and store movie-related metadata in MySQL.

Current package structure:

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
                └── model/
                    ├── Movie
                    ├── Genre
                    └── VideoStatus
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

Used for receiving movie information from the client when creating/updating movie data.

MovieResponse

Used to return movie information from the API to the client.

DTOs help keep the API layer separate from the database entity.

🗃️ Database

The Content Service uses MySQL to store movie metadata.

The movie entity is mapped to the:

movies

table.

The database stores information about the movie, while large video files will eventually be stored separately in AWS S3.

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
MySQL
Redis
Apache Kafka
ZooKeeper