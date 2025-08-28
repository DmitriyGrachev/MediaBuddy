package org.hrachov.com.filmproject.repository.mongo;

import org.hrachov.com.filmproject.model.UsersVideos;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVideosRepository extends MongoRepository<UsersVideos, String> {
}
