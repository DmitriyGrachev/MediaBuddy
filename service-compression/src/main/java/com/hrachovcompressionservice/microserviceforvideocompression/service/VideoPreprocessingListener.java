package com.hrachovcompressionservice.microserviceforvideocompression.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrachovcompressionservice.microserviceforvideocompression.config.VideoProcessingQueueConfig;
import com.hrachovcompressionservice.microserviceforvideocompression.model.CustomVideo;
import com.hrachovcompressionservice.microserviceforvideocompression.model.dto.*;
import com.hrachovcompressionservice.microserviceforvideocompression.repository.CustomVideoRepository;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoPreprocessingListener {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final CustomVideoRepository customVideoRepository;

    /**
     * Этап 1: Принимает первоначальный запрос на обработку видео.
     */
    @RabbitListener(queues = VideoProcessingQueueConfig.PREPROCESSOR_QUEUE)
    public void handlePreprocessingRequest(CompressionRequest request, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("▶️ [ETAP 1: PRE-PROCESSING] Received request for videoId: {}", request.getVideoId());
        CustomVideo video = customVideoRepository.findById(request.getVideoId())
                .orElse(null);

        if (video == null) {
            log.error("❌ Video with id: {} not found. Message will be discarded.", request.getVideoId());
            channel.basicAck(deliveryTag, false); // Подтверждаем, чтобы убрать "битое" сообщение
            return;
        }

        try {
            // 1. Валидация запроса и файла
            Path filePath = Paths.get(request.getAbsoluteFilePath());
            validateRequest(request);
            validateFile(filePath);

            // 2. Обновляем статус в БД и извлекаем метаданные
            video.setStatus(VideoStatus.PROCESSING);
            Map<String, Object> metadata = extractMetadata(filePath);
            video.setMetadataOriginal(metadata);
            customVideoRepository.save(video);
            log.info("✅ Metadata extracted, status updated to PROCESSING for videoId: {}", video.getId());

            // 3. Отправляем дальше по конвейеру на этап компрессии
            rabbitTemplate.convertAndSend(
                    VideoProcessingQueueConfig.COMPRESSION_EXCHANGE,
                    VideoProcessingQueueConfig.COMPRESSION_ROUTING_KEY,
                    request
            );
            log.info("➡️ [ETAP 1: PRE-PROCESSING] Forwarding to compression queue for videoId: {}", video.getId());

            // 4. Подтверждаем успешную обработку
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ [ETAP 1: PRE-PROCESSING FAILED] for videoId: {}. Reason: {}", request.getVideoId(), e.getMessage());
            //video.setStatus(VideoStatus.FAILED);
            video.setStatus(VideoStatus.PROCESSING);
            video.setErrorMessage("Preprocessing failed: " + e.getMessage());
            customVideoRepository.save(video);
            sendFailureNotification(request.getUsername(), e.getMessage());

            // Ошибка на этом этапе фатальна, поэтому не повторяем, а просто подтверждаем,
            // чтобы сообщение ушло в DLQ (или было удалено, если DLQ не настроен)
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // ... все вспомогательные методы (extractMetadata, validateFile и т.д.) остаются здесь без изменений
    private Map<String, Object> extractMetadata(Path filePath) throws IOException, InterruptedException {
        Path parentDir = filePath.getParent();
        String fileName = filePath.getFileName().toString();

        List<String> command = List.of(
                "docker", "run", "--rm",
                "-v", parentDir + ":/data",
                "--entrypoint", "ffprobe", "jrottenberg/ffmpeg:latest",
                "-v", "error", "-show_format", "-show_streams",
                "-of", "json", "/data/" + fileName
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
        metadata.put("size_bytes", format.path("size").asLong());
        metadata.put("bit_rate_bps", format.path("bit_rate").asLong());

        for (JsonNode stream : root.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) {
                metadata.put("codec_name", stream.path("codec_name").asText());
                metadata.put("width", stream.path("width").asInt());
                metadata.put("height", stream.path("height").asInt());
                metadata.put("resolution", String.format("%dx%d",
                        stream.path("width").asInt(),
                        stream.path("height").asInt()));
                metadata.put("avg_frame_rate", stream.path("avg_frame_rate").asText());
                break;
            }
        }
        return metadata;
    }

    private void validateRequest(CompressionRequest request) {
        if (request.getVideoId() == null || request.getQualities() == null || request.getQualities().isEmpty()) {
            throw new IllegalArgumentException("VideoId or qualities list is null or empty.");
        }
    }

    private void validateFile(Path filePath) throws IOException {
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File does not exist or is not a regular file: " + filePath);
        }

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null || !mimeType.startsWith("video/")) {
            throw new IllegalArgumentException("File is not a valid video based on MIME type: " + mimeType);
        }

        long maxSizeBytes = 20L * 1024 * 1024 * 1024; // 20 GB
        if (file.length() > maxSizeBytes) {
            throw new IllegalArgumentException("File size exceeds the limit of 20 GB: " + file.length() + " bytes");
        }
    }

    private void sendFailureNotification(String username, String message) {
        NotificationDTO notification = NotificationDTO.builder()
                .createdAt(LocalDateTime.now())
                .username(username)
                .type(NotificationType.PROCESSED_FAILED)
                .message("Ошибка на начальном этапе обработки видео: " + message)
                .link(null)
                .build();

        rabbitTemplate.convertAndSend("notification.exchange", "notification.routing.key", notification);
        log.info("✉️ [NOTIFICATION] Sent pre-processing failure for user '{}'", username);
    }
}
