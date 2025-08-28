package org.hrachov.com.filmproject.repository.mongo;

import org.hrachov.com.filmproject.model.CustomVideo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomVideoRepository extends MongoRepository<CustomVideo, String> {
    List<CustomVideo> findByUserId(String userId);
}
