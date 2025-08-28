package com.hrachovcompressionservice.microserviceforvideocompression.service;

import com.hrachovcompressionservice.microserviceforvideocompression.config.VideoProcessingQueueConfig;
import com.hrachovcompressionservice.microserviceforvideocompression.model.CustomVideo;
import com.hrachovcompressionservice.microserviceforvideocompression.model.dto.*;
import com.hrachovcompressionservice.microserviceforvideocompression.repository.CustomVideoRepository;
import com.hrachovcompressionservice.microserviceforvideocompression.strategy.CompressionStrategy;
import com.hrachovcompressionservice.microserviceforvideocompression.strategy.HD720pStrategy;
import com.hrachovcompressionservice.microserviceforvideocompression.strategy.SD480pStrategy;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoCompressionListener {

    private final RabbitTemplate rabbitTemplate;
    private final CustomVideoRepository customVideoRepository;
    private final BackblazeB2UploadService backblazeB2UploadService;
    private static final int MAX_RETRIES = 2; // Всего 3 попытки (1 + 2 повтора)

    @Value("${app.video.base-path}")
    private String videoBasePath;
    @Value("${ffmpeg.docker.image}")
    private String ffmpegDockerImage;

    private Path compressedVideosDir;
    private final Map<String, CompressionStrategy> strategies = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            this.compressedVideosDir = Paths.get(videoBasePath, "compressed");
            Files.createDirectories(compressedVideosDir);
            strategies.put("720p", new HD720pStrategy());
            strategies.put("480p", new SD480pStrategy());
            log.info("🎥 VideoCompressionListener initialized. Compressed videos path: {}", compressedVideosDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not create video directories", e);
        }
    }

    /**
     * Этап 2: Сжимает видео. Реализует паттерн Retry для обработки временных сбоев.
     */
    @RabbitListener(queues = VideoProcessingQueueConfig.COMPRESSION_QUEUE)
    public void handleCompressionRequest(
            CompressionRequest request,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeathHeaders
    ) throws IOException {

        log.info("▶️ [ETAP 2: COMPRESSION] Received request for videoId: {}", request.getVideoId());
        CustomVideo video = customVideoRepository.findById(request.getVideoId()).orElse(null);
        String useriD = video.getUserId();

        if (video == null) {
            log.error("❌ Video with id: {} not found. Discarding message.", request.getVideoId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            Path originalFilePath = Paths.get(request.getAbsoluteFilePath());
            if (!Files.exists(originalFilePath)) {
                handleTerminalError(video, "Original file not found at " + originalFilePath, channel, deliveryTag);
                return;
            }

            Map<String, Object> processedFiles = new ConcurrentHashMap<>(video.getProcessedFiles() != null ? video.getProcessedFiles() : new HashMap<>());

            for (String quality : request.getQualities()) {
                //По чоереди берем стратегию из списка 460p 720p
                CompressionStrategy strategy = strategies.get(quality);
                if (strategy == null) {
                    log.warn("⚠️ No compression strategy for quality: {}. Skipping.", quality);
                    continue;
                }
                log.info("🚀 Starting compression for quality: {}", quality);

                //Запускаем компресиию ffmpeg по качеству и получаем локальный путь в папку с компресией
                Path compressedFilePath = runFfmpegForQuality(originalFilePath, strategy);
                //Загружаем файл в облако
                //TODO NEW UPLOD TO B2
                String presignedUrl = backblazeB2UploadService.uploadFile(
                        compressedFilePath,
                        useriD + "/videos/" + compressedFilePath.getFileName().toString()
                );
                System.out.println("🎬 Watch at: " + presignedUrl);

                //мапа небольшая с данными убрал размер и добавилпуть к ссылке
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("path", presignedUrl);
                fileInfo.put("compressedFilPath", compressedFilePath.toString());
                //Дабавляем ключ качество - мапа с положением
                processedFiles.put(strategy.getQualityKey(), fileInfo);
                log.info("✅ Compression successful for quality: {}", quality);

            }

            video.setProcessedFiles(processedFiles);
            customVideoRepository.save(video);
            log.info("✅ All qualities compressed for videoId: {}", video.getId());

            CompressionResponse response = CompressionResponse.builder()
                    .videoId(video.getId())
                    .username(request.getUsername())
                    .absoluteFilePath(request.getAbsoluteFilePath())
                    .compressedFiles(processedFiles)
                    .build();

            rabbitTemplate.convertAndSend(VideoProcessingQueueConfig.THUMBNAIL_EXCHANGE, VideoProcessingQueueConfig.THUMBNAIL_ROUTING_KEY, response);
            log.info("➡️ [ETAP 2: COMPRESSION] Forwarding to thumbnail queue for videoId: {}", video.getId());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ [ETAP 2: COMPRESSION FAILED] for videoId: {}. Reason: {}", request.getVideoId(), e.getMessage());

            long retryCount = getRetryCount(xDeathHeaders);

            if (retryCount < MAX_RETRIES) {
                log.warn("Retrying... Attempt {} of {} for videoId: {}", retryCount + 1, MAX_RETRIES + 1, video.getId());
                channel.basicNack(deliveryTag, false, false); // Отправляем в retry-цепочку
            } else {
                log.error("Max retries ({}) reached. Sending to DLQ for videoId: {}", MAX_RETRIES, video.getId());
                handleTerminalError(video, "Max retries reached. Last error: " + e.getMessage(), channel, deliveryTag);
            }
        }
    }

    private long getRetryCount(List<Map<String, Object>> xDeathHeaders) {
        if (xDeathHeaders == null) {
            return 0;
        }
        return xDeathHeaders.stream()
                .filter(header -> header.containsKey("count"))
                .map(header -> (Long) header.get("count"))
                .findFirst()
                .orElse(0L);
    }

    private void handleTerminalError(CustomVideo video, String errorMessage, Channel channel, long deliveryTag) throws IOException {
        log.error("❌ Terminal error for videoId: {}. Reason: {}", video.getId(), errorMessage);
        video.setStatus(VideoStatus.FAILED);
        video.setErrorMessage(errorMessage);
        customVideoRepository.save(video);

        // Отправляем в DLQ, отправляя Nack и не переставляя в очередь
        // RabbitMQ перенаправит его в DLX, так как мы это настроили.
        channel.basicNack(deliveryTag, false, false);
    }

    private Path runFfmpegForQuality(Path originalFilePath, CompressionStrategy strategy) throws IOException, InterruptedException {
        String originalFileName = originalFilePath.getFileName().toString();
        Path hostOriginalFileDir = originalFilePath.getParent();
        String compressedFileName = generateCompressedFileName(originalFileName, strategy.getQualityKey());
        Path hostCompressedFilePath = compressedVideosDir.resolve(compressedFileName);

        List<String> command = new ArrayList<>(Arrays.asList(
                "docker", "run", "--rm",
                "-v", hostOriginalFileDir.toAbsolutePath() + ":/input:ro",
                "-v", compressedVideosDir.toAbsolutePath() + ":/output",
                ffmpegDockerImage,
                "-i", "/input/" + originalFileName
        ));
        command.addAll(Arrays.asList(strategy.getFfmpegOptions().trim().split("\\s+")));
        command.add("/output/" + compressedFileName);

        log.info("🚀 [FFMPEG] Executing for quality '{}': {}", strategy.getQualityKey(), String.join(" ", command));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.trace("🎞️ [FFMPEG LOGS] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg process failed with exit code " + exitCode + " for quality " + strategy.getQualityKey());
        }
        if (!Files.exists(hostCompressedFilePath)) {
            throw new IOException("Output file not found after compression for quality " + strategy.getQualityKey());
        }
        return hostCompressedFilePath;
    }

    private String generateCompressedFileName(String originalFileName, String qualitySuffix) {
        String coreName = originalFileName.contains(".") ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
        String extension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
        return String.format("%s_%s%s", coreName, qualitySuffix, extension);
    }

}
