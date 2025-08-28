package com.hrachovcompressionservice.microserviceforvideocompression.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrachovcompressionservice.microserviceforvideocompression.config.RabbitMQNotification;
import com.hrachovcompressionservice.microserviceforvideocompression.config.VideoProcessingQueueConfig;
import com.hrachovcompressionservice.microserviceforvideocompression.model.CustomVideo;
import com.hrachovcompressionservice.microserviceforvideocompression.model.dto.*;
import com.hrachovcompressionservice.microserviceforvideocompression.repository.CustomVideoRepository;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoPostprocessorListener {

    private final RabbitTemplate rabbitTemplate;
    private final CustomVideoRepository customVideoRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_POST_PROC_RETRIES = 2;
    private final BackblazeB2UploadService backblazeB2UploadService;

    @Value("${app.video.base-path}")
    private String videoBasePath;
    @Value("${ffmpeg.docker.image}")
    private String ffmpegDockerImage;
    private Path thumbnailsDir;

    @PostConstruct
    public void init() {
        try {
            this.thumbnailsDir = Paths.get(videoBasePath, "thumbnails");
            Files.createDirectories(thumbnailsDir);
            log.info("🖼️ Postprocessor listener initialized. Thumbnails path: {}", thumbnailsDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not create thumbnail directory", e);
        }
    }

    /**
     * Этап 3: Создание превью.
     */
    @RabbitListener(queues = VideoProcessingQueueConfig.THUMBNAIL_QUEUE)
    public void handleThumbnailRequest(
            CompressionResponse request,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeathHeaders
    ) throws IOException {
        log.info("▶️ [ETAP 3: THUMBNAIL] Received request for videoId: {}", request.getVideoId());
        CustomVideo video = customVideoRepository.findById(request.getVideoId()).orElse(null);

        if (video == null) {
            log.error("❌ Video not found. Discarding message for videoId: {}", request.getVideoId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            CustomVideo customVideo = customVideoRepository.findById(request.getVideoId()).orElse(null);
            String userId = customVideo.getUserId();

            Path originalFilePath = Paths.get(request.getAbsoluteFilePath());
            Path hostThumbnailPath = generateThumbnail(originalFilePath);
            String thumbnailFinal = backblazeB2UploadService.uploadFile(hostThumbnailPath,userId+"/thumbnailvideos/" + request.getVideoId());
            video.setThumbnailPath(thumbnailFinal);
            video.setStatus(VideoStatus.THUMBNAIL);
            customVideoRepository.save(video);
            log.info("✅ [ETAP 3: THUMBNAIL] Thumbnail generated for videoId: {}", video.getId());

            rabbitTemplate.convertAndSend(VideoProcessingQueueConfig.METADATA_EXCHANGE, VideoProcessingQueueConfig.METADATA_ROUTING_KEY, request);
            log.info("➡️ [ETAP 3: THUMBNAIL] Forwarding to metadata queue for videoId: {}", video.getId());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            handlePostProcessingError("Thumbnail", e, video, channel, deliveryTag, xDeathHeaders);
        }
    }

    /**
     * Этап 4: Извлечение метаданных. Финальный шаг.
     */
    @RabbitListener(queues = VideoProcessingQueueConfig.METADATA_QUEUE)
    public void handleMetadataRequest(
            CompressionResponse request,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeathHeaders
    ) throws IOException {
        log.info("▶️ [ETAP 4: METADATA] Received request for videoId: {}", request.getVideoId());
        CustomVideo video = customVideoRepository.findById(request.getVideoId()).orElse(null);

        if (video == null) {
            log.error("❌ Video not found. Discarding message for videoId: {}", request.getVideoId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            //Берем данные именно из локальной компресии а не из облака
            Map<String, Object> compressedFiles = request.getCompressedFiles();
            Map<String, Object> metadataResult = new HashMap<>();
            for (Map.Entry<String, Object> entry : compressedFiles.entrySet()) {
                Map<String, Object> metadata = (Map<String, Object>) entry.getValue();
                String s = (String) metadata.get("compressedFilPath");
                metadataResult.put(entry.getKey(), extractMetadata(Paths.get(s)));
            }

            video.setMetadataProcessed(metadataResult);
            video.setStatus(VideoStatus.COMPLETED);
            //TODO Leav or not
            //video.setCreatedAt(Date.from(Instant.now()));
            customVideoRepository.save(video);
            log.info("✅ [ETAP 4: METADATA] Metadata extracted for videoId: {}", video.getId());

            sendSuccessNotification(request.getUsername(), video.getId());
            log.info("🏁 [SUCCESS] Video processing pipeline finished for videoId: {}", video.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            handlePostProcessingError("Metadata", e, video, channel, deliveryTag, xDeathHeaders);
        }
    }

    private void handlePostProcessingError(String step, Exception e, CustomVideo video, Channel channel, long deliveryTag, List<Map<String, Object>> xDeathHeaders) throws IOException {
        log.error("❌ [ETAP FAILED: {}] for videoId: {}. Reason: {}", step, video.getId(), e.getMessage());

        long retryCount = xDeathHeaders != null ? xDeathHeaders.stream().filter(h -> h.containsKey("count")).count() : 0;

        if (retryCount < MAX_POST_PROC_RETRIES) {
            log.warn("Retrying... Attempt {} of {} for videoId: {}", retryCount + 1, MAX_POST_PROC_RETRIES + 1, video.getId());
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.error("Max retries reached for step: {}. Sending to DLQ for videoId: {}", step, video.getId());
            video.setStatus(VideoStatus.FAILED);
            video.setErrorMessage("Failed at step: " + step + ". Last error: " + e.getMessage());
            customVideoRepository.save(video);
            channel.basicNack(deliveryTag, false, false);
        }
    }
    private Map<String, Object> extractMetadata(Path filePath) throws IOException, InterruptedException {
        Path parentDir = filePath.getParent();
        String fileName = filePath.getFileName().toString();

        List<String> command = List.of(
                "docker",           // "docker"
                "run", "--rm",
                "-v", parentDir + ":/data",    // монтируем папку с файлом
                "--entrypoint", "ffprobe",     // << переназначаем entrypoint
                "jrottenberg/ffmpeg:latest",             // "jrottenberg/ffmpeg:latest"
                "-v", "error",
                "-show_format", "-show_streams",
                "-of", "json",
                "/data/" + fileName
        );

        Process process = new ProcessBuilder(command).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("🚨 ffprobe (docker) failed with exit code {}. Output: {}", exitCode, err);
            throw new IOException("ffprobe failed. File may be corrupted or invalid.");
        }

        JsonNode root = objectMapper.readTree(output);
        Map<String, Object> metadata = new HashMap<>();

        JsonNode format = root.path("format");
        metadata.put("format_name", format.path("format_name").asText());
        metadata.put("duration_sec", format.path("duration").asDouble());
        metadata.put("size_bytes",   format.path("size").asLong());
        metadata.put("bit_rate_bps", format.path("bit_rate").asLong());

        for (JsonNode stream : root.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) {
                metadata.put("codec_name",     stream.path("codec_name").asText());
                metadata.put("width",          stream.path("width").asInt());
                metadata.put("height",         stream.path("height").asInt());
                metadata.put("resolution",     String.format("%dx%d",
                        stream.path("width").asInt(),
                        stream.path("height").asInt()));
                metadata.put("avg_frame_rate", stream.path("avg_frame_rate").asText());
                break;
            }
        }
        return metadata;
    }
    private Path generateThumbnail(Path originalFilePath) throws IOException, InterruptedException {
        String originalFileName = originalFilePath.getFileName().toString();
        Path hostOriginalFileDir = originalFilePath.getParent();

        String baseName = originalFileName.contains(".") ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
        String thumbnailName = baseName + ".png";
        Path hostThumbnailFilePath = thumbnailsDir.resolve(thumbnailName);

        // Пути внутри контейнера
        String containerOriginalDir = "/input";
        String containerThumbnailsDir = "/output";
        String containerOriginalFile = containerOriginalDir + "/" + originalFileName;
        String containerThumbnailFile = containerThumbnailsDir + "/" + thumbnailName;

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-v");
        command.add(hostOriginalFileDir.toAbsolutePath() + ":" + containerOriginalDir + ":ro");
        command.add("-v");
        command.add(thumbnailsDir.toAbsolutePath() + ":" + containerThumbnailsDir);
        command.add(ffmpegDockerImage);
        command.add("-i");
        command.add(containerOriginalFile);
        command.add("-ss");       // Взять кадр на 1-й секунде
        command.add("00:00:01.000");
        command.add("-vframes");  // Взять только 1 кадр
        command.add("1");
        command.add(containerThumbnailFile);

        log.info("🚀 [FFMPEG THUMBNAIL] Executing: {}", String.join(" ", command));

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> log.trace("🎞️ [FFMPEG THUMBNAIL LOGS] {}", line));
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg thumbnail process failed with exit code " + exitCode);
        }

        return hostThumbnailFilePath;
    }

    private void sendSuccessNotification(String username, String videoId) {
        NotificationDTO notification = NotificationDTO.builder()
                .createdAt(LocalDateTime.now())
                .username(username)
                .type(NotificationType.PROCESSED_SUCCESSFULLY)
                .message("Ваше видео успешно обработано и готово к просмотру.")
                .link("/videos/" + videoId) // Пример ссылки на страницу просмотра
                .build();

        rabbitTemplate.convertAndSend(RabbitMQNotification.EXCHANGE_NAME, RabbitMQNotification.ROUTING_KEY, notification);
        log.info("✉️ [NOTIFICATION] Sent final success notification for user '{}' and videoId '{}'", username, videoId);
    }
    private void handleProcessingError(CustomVideo video, String message, Exception e) {
        log.error("❌ [ERROR] {} for videoId: {}. Reason: {}", message, video.getId(), e.getMessage(), e);
        video.setStatus(VideoStatus.FAILED);
        video.setErrorMessage(message + ": " + e.getMessage());
        customVideoRepository.save(video);
        // Здесь можно отправить уведомление об ошибке пользователю, если это необходимо
    }
}
