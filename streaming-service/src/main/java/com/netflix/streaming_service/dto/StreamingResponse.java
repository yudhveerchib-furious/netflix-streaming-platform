package com.netflix.streaming_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {

  private String movieId;
  private String streamingURL; // presigned hls master playlist url
  private String quality; // avail qualities
  private long expiredInMinutes; // url expiry time


}
