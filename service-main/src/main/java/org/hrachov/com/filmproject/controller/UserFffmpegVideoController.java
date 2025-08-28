package org.hrachov.com.filmproject.controller;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.hrachov.com.filmproject.config.B2Config;
import org.hrachov.com.filmproject.config.RabbitMQConfig;
import org.hrachov.com.filmproject.model.CustomVideo;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.VideoStatus;
import org.hrachov.com.filmproject.model.dto.CompressionRequest;
import org.hrachov.com.filmproject.model.dto.CustomVideoDTO;
import org.hrachov.com.filmproject.model.dto.CustomVideoFilterDTO;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.repository.mongo.CustomVideoRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component; // If RabbitMQConfig is in another package
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct; // For Spring Boot 3+
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mpeg/")
public class UserFffmpegVideoController {
    private final RedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CustomVideoRepository customVideoRepository;
    private final MongoTemplate mongoTemplate;
    @Value("${app.video.base-path}")
    private String videoBasePath;
    private Path originalVideosDir;
    // Timeout for waiting for RabbitMQ response (e.g., 5 minutes for compression)
    private static final long RABBITMQ_REPLY_TIMEOUT_MS = 300_000; // 5 minutes
    @Value("${backblaze.b2.bucket-name}")
    private String bucketName;
    @Value("${backblaze.b2.access-key-id}")
    private String accessKeyId;

    @Value("${backblaze.b2.secret-access-key}")
    private String secretAccessKey;

    @Value("${backblaze.b2.endpoint-url}")
    private String endpointUrl;

    @Value("${backblaze.b2.region}")
    private String region;

    public UserFffmpegVideoController(RedisTemplate redisTemplate, RabbitTemplate rabbitTemplate, CurrentUserService currentUserService, UserRepository userRepository, CustomVideoRepository customVideoRepository, MongoTemplate mongoTemplate) {
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.customVideoRepository = customVideoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void init() {
        this.originalVideosDir = Paths.get(videoBasePath, "original");
        try {
            Files.createDirectories(originalVideosDir);
            System.out.println("🎥 UserFffmpegVideoController initialized.");
            System.out.println("📂 Original videos directory set to: " + originalVideosDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("🚨 Could not create original videos directory: " + originalVideosDir + " - " + e.getMessage());
            throw new RuntimeException("Could not create original videos directory: " + originalVideosDir, e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("video") MultipartFile video, @RequestParam String title, @RequestParam String description, @RequestParam List<String> qualities, @RequestParam Set<String> tags) {

        CustomVideo usersInputVideo = new CustomVideo();
        usersInputVideo.setTitle(title);
        usersInputVideo.setDescription(description);
        Set<String> qualitiesSet = tags.stream().map(tag ->{
             return tag.replaceAll("^\"|\"$", "");
        }).collect(Collectors.toSet());
        usersInputVideo.setTags(tags);
        User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElse(null);

        usersInputVideo.setUserId(String.valueOf(user.getId()));
        usersInputVideo.setStatus(VideoStatus.UPLOADING);
        usersInputVideo.setCreatedAt(Date.from(Instant.now()));
        Map<String, Object> originalFileInfo = new HashMap<>();

        System.out.println("===> [UPLOAD START] Received upload request for file: " + video.getOriginalFilename() + ", Size: " + video.getSize());

        if (video.isEmpty()) {
            System.out.println("[UPLOAD] Video file is empty.");
            Map<String, String> responseBody = Map.of(
                    "message", "Video file is empty."
            );
            return ResponseEntity.badRequest().body(responseBody);
        }

        if (originalVideosDir == null) {
            System.err.println("[UPLOAD ERROR] originalVideosDir is not initialized!");
            Map<String, String> responseBody = Map.of(
                    "message", "Server error: storage path not initialized."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }

        String originalFileNameClean;
        try {
            originalFileNameClean = StringUtils.cleanPath(Objects.requireNonNull(video.getOriginalFilename()));
        } catch (NullPointerException e) {
            System.err.println("[UPLOAD ERROR] Original filename is null!");
            Map<String, String> responseBody = Map.of(
                    "message", "Original filename is null."
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }

        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileNameClean;
        Path filePath = originalVideosDir.resolve(uniqueFileName);
        System.out.println("===> [UPLOAD INFO] Attempting to save to: " + filePath.toAbsolutePath());

        String correlationId = UUID.randomUUID().toString(); // Generate correlationId early

        try (InputStream inputStream = video.getInputStream()) {
            System.out.println("===> [UPLOAD INFO] InputStream obtained. Starting file copy...");
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("===> [UPLOAD SUCCESS] Video saved successfully to: " + filePath.toAbsolutePath());

            originalFileInfo.put("originalFilename", originalFileNameClean);
            originalFileInfo.put("fileSize", video.getSize());
            originalFileInfo.put("storagePath", filePath.toAbsolutePath().toString());
            usersInputVideo.setOriginalFile(originalFileInfo);
            customVideoRepository.save(usersInputVideo);

            // 2. Готовим и отправляем сообщение
            //CompressionRequest compressionRequest = new CompressionRequest(usersInputVideo.getId(),filePath.toAbsolutePath().toString(),currentUserService.getCurrentUser().getUsername(),null,correlationId,0,qualities);
            //TODO
            //List<String> list = new ArrayList<>();
            //list.add("720p");
            CompressionRequest compressionRequest = new CompressionRequest(usersInputVideo.getId(),
                    filePath.toAbsolutePath().toString()
                    , currentUserService.getCurrentUser().getUsername(),
                    null, correlationId,
                    0, qualities);

            //TODO FIRST WE WILL SEND TO PREPROC AND NOT TO COMPRESSION THUS WE WILL CREATE PIPELINE
            int priority = isUserVip();
            //rabbitTemplate.convertAndSend(RabbitMQConfig.COMPRESSION_EXCHANGE_NAME, RabbitMQConfig.COMPRESSION_ROUTING_KEY,compressionRequest);
            rabbitTemplate.convertAndSend(RabbitMQConfig.PREPROCESSOR_EXCHANGE,
                    RabbitMQConfig.PREPROCESSOR_ROUTING_KEY,
                    compressionRequest,
                    message -> {
                message.getMessageProperties().setPriority(priority);
                return message;
                    });


            System.out.println("--- [RABBITMQ WAIT] Waiting for compression response for CorrelationID: " + correlationId + " (Timeout: " + RABBITMQ_REPLY_TIMEOUT_MS + "ms)");
            // 3. НЕ ЖДЕМ! НЕМЕДЛЕННО ВОЗВРАЩАЕМ ОТВЕТ КЛИЕНТУ!
            String responseMessage = "Файл успешно загружен и отправлен в обработку. Вы получите уведомление по завершении.";
            Map<String, String> responseBody = Map.of(
                    "message", responseMessage,
                    "trackingId", correlationId // Отдаем ID, чтобы клиент мог отслеживать статус (на будущее)
            );

            return ResponseEntity.accepted().body(responseBody); // HTTP 202 Accepted
        } catch (IOException e) {
            System.err.println("!!! [UPLOAD IOException] IOException during file copy: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> responseBody = Map.of(
                    "message", "Error saving video: " + e.getMessage(),
                    "trackingId", correlationId

            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }

    @GetMapping("/videos")
    public ResponseEntity<List<CustomVideoDTO>> getAllVideos() {
        User user = userRepository
                .findByUsername(currentUserService.getCurrentUser().getUsername())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        List<CustomVideoDTO> result = customVideoRepository
                .findByUserId(String.valueOf(user.getId()))
                .stream()
                .filter(v -> v.getStatus() == VideoStatus.COMPLETED)
                .map(video -> {
                    // 1) собираем карту качество→URL
                    Map<String, String> urls = video.getProcessedFiles().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> info = (Map<String, Object>) e.getValue();
                                        return getPresignedUrl(info.get("path").toString());
                                    }
                            ));

                    // 2) метаданные как есть (Map<String, Map<String,Object>>)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = video.getMetadataProcessed();

                    return CustomVideoDTO.builder()
                            .title(video.getTitle())
                            .description(video.getDescription())
                            .status(video.getStatus())
                            .thumbnailUrl(getPresignedUrl(video.getThumbnailPath()))
                            .videoUrls(urls)
                            .metadata(metadata)
                            .tags(video.getTags())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tag")
    public ResponseEntity<?> addNewTag(@RequestParam(name = "tag") String tag) {
        try {
            User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

            String key = user.getId() + ":videos";
            Set<String> tagsCached = redisTemplate.opsForSet().members(key);
            tag.replaceAll("^\"|\"$", "");
            tagsCached.add(tag.toLowerCase());

            redisTemplate.opsForSet().add(key, tag);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    @DeleteMapping("/tag")
    public ResponseEntity<?> removeTag(@RequestParam(name = "tag") String tag) {
        try {
            User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

            String key = user.getId() + ":videos";
            Set<String> tagsCached = redisTemplate.opsForSet().members(key);
            tag.replaceAll("^\"|\"$", "");
            tagsCached.remove(tag.toLowerCase());
            redisTemplate.opsForSet().add(key, tagsCached);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/tags")
    public ResponseEntity<Set<String>> getAllTags() {
        User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
        String key = user.getId() + ":videos";
        Set<String> tagsCached = redisTemplate.opsForSet().members(key);

        if(tagsCached.size() < 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(tagsCached);
    }
    @PostMapping("/filter")
    public ResponseEntity<List<CustomVideoDTO>> getAllFilters(@RequestBody CustomVideoFilterDTO filter) {
        User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
        Criteria criteria = Criteria.where("userId").is(user.getId().toString());

        if(filter.getTags() != null && !filter.getTags().isEmpty()) {
            criteria = criteria.and("tags").all(filter.getTags());
        }
        if(filter.getTitle() != null && !filter.getTitle().isEmpty()) {
            criteria = criteria.and("title").regex(filter.getTitle());
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.fromString(filter.getDirection()), filter.getSortBy()));
        List<CustomVideo> customVideos = mongoTemplate.find(query,CustomVideo.class);
        if(customVideos.size() > 0) {
            return ResponseEntity.ok(customVideos.stream().map(video -> {
                // 1) собираем карту качество→URL
                Map<String, String> urls = video.getProcessedFiles().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> info = (Map<String, Object>) e.getValue();
                                    return getPresignedUrl(info.get("path").toString());
                                }
                        ));

                // 2) метаданные как есть (Map<String, Map<String,Object>>)
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = video.getMetadataProcessed();

                return CustomVideoDTO.builder()
                        .title(video.getTitle())
                        .description(video.getDescription())
                        .status(video.getStatus())
                        .thumbnailUrl(getPresignedUrl(video.getThumbnailPath()))
                        .videoUrls(urls)
                        .metadata(metadata)
                        .tags(video.getTags())
                        .build();
            }).collect(Collectors.toList()));
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    private String getPresignedUrl(String url) {
        Object cache = redisTemplate.opsForValue().get(url);

        if (cache != null) {
            return (String) cache;
        } else {
            try {
                S3Presigner presigner = S3Presigner.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                        .region(Region.of(region))
                        .endpointOverride(URI.create(endpointUrl))
                        .build();

                GetObjectRequest objectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(url)
                        .build();

                GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
                        .getObjectRequest(objectRequest)
                        .signatureDuration(Duration.ofHours(24))
                        .build();

                PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(objectPresignRequest);

                String urlPresigned = presignedGetObjectRequest.url().toString();
                Instant instant = presignedGetObjectRequest.expiration();
                redisTemplate.opsForValue().set(url, urlPresigned, instant.toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

                return urlPresigned;
            } catch (Exception e) {
                log.error("❌ Failed to presign reason  {}", e);
                throw new RuntimeException("Failed to upload file to Backblaze B2", e);
            }

        }
    }
    private int isUserVip() {
        Collection<? extends GrantedAuthority> authorities = currentUserService.getCurrentUser().getAuthorities();
        log.info("Current user has authorities: {}", authorities);
        return authorities.contains("ROLE_VIP") ? 10 : 0;
    }
}