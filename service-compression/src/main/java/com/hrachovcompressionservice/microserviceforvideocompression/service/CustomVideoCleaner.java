package com.hrachovcompressionservice.microserviceforvideocompression.service;

import com.hrachovcompressionservice.microserviceforvideocompression.repository.CustomVideoRepository;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.internal.MongoClientImpl;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.mongodb.client.result.DeleteResult;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Component
@AllArgsConstructor
public class CustomVideoCleaner {
    private final CustomVideoRepository customVideoRepository;

    @Scheduled(fixedRate = 30000)
    @Async
    public void cleanVideosFailed() {
        try(MongoClient mongoClient = MongoClients.create("mongodb://appuser:appPass123@localhost:27017/filmproject?authSource=filmproject&authMechanism=SCRAM-SHA-256")) {
            MongoDatabase db = mongoClient.getDatabase("filmproject");
            MongoCollection<Document> collection = db.getCollection("videos");
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(5));
            Date cutoffDate = Date.from(cutoff);

            Document filter = new Document("status", "FAILED");
            Document filter2 = new Document("$and",
                    Arrays.asList(new Document("status","PROCESSING"),
                            new Document("createdAt",new Document("$lt",cutoffDate))));

            Document filter3 = new Document("$or", Arrays.asList(filter, filter2));
            DeleteResult result = collection.deleteMany(filter3);
            log.info("Scheduled cleaning of CustomVideos Collection " + result.toString());
        }catch (Exception e) {
            log.error("Error occurred during cleaning collection" + e.getMessage());
        }
        //придумать что сделать перед удалением
        //customVideoRepository delete query class
        //db.collection.removeMany
        //Query query = new Query();
        //query.addCriteria(Criteria.where("status").is("FAILED"));
    }
}
