package com.netflix.video_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {


     // published when video is uploaded to S3 consumed by encoding services
    @Bean
    public NewTopic videoUploadedTopic() {
       return TopicBuilder.name("video.uploaded")
               .partitions(3)
               .replicas(1)
               .build();
     }

     //published when encoding is complete
    @Bean
    public NewTopic videoEncodedType() {
         return TopicBuilder.name("video.encoded")
                 .partitions(3)
                 .replicas(1)
                 .build();
     }

}
