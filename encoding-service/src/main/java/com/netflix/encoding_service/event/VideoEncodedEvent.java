package com.netflix.encoding_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEvent {
   private String movieId;
   private String hlsUrl; // master playlist url for streaming
   private String masterPlaylistKey; // s3 key of master.m3u8
   private boolean success;
   private String errorMessage; // If encoding failed

}
