package com.hrachovcompressionservice.microserviceforvideocompression.repository;

import com.hrachovcompressionservice.microserviceforvideocompression.model.CustomVideo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomVideoRepository extends MongoRepository<CustomVideo, String> {
}
