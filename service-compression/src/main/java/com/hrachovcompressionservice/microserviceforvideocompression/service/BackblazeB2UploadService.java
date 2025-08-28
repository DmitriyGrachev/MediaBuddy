package com.hrachovcompressionservice.microserviceforvideocompression.service;

import com.hrachovcompressionservice.microserviceforvideocompression.config.B2Config;
import com.hrachovcompressionservice.microserviceforvideocompression.model.videofile.VideoFileForB2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Service
public class BackblazeB2UploadService {

    private final S3Client s3Client;

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

    public BackblazeB2UploadService(S3Client s3Client) {
        // Spring автоматически внедрит бин, созданный в B2Config
        this.s3Client = s3Client;
    }

    public String uploadFile(Path filePath, String remotePath) {
        log.info("Starting upload for file: {} as {} to bucket: {}", filePath, remotePath, bucketName);

        try {
            // 1. Загружаем файл в бакет
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remotePath)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(filePath));
            log.info("✅ Successfully uploaded {} to Backblaze B2.", remotePath);

            /*// 2. Создаем presigner для генерации временного доступа
            S3Presigner presigner = S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpointUrl))
                    .build();

            // 3. Строим запрос на доступ к объекту
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(remotePath)
                    .build();

            // 4. Генерируем временную ссылку (например, на 1 час)
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getReq)
                    .build();
            String presignedUrl = presigner.presignGetObject(presignReq)
                    .url()
                    .toString();

             */
            log.info("🎥 Generated presigned URL for streaming: {}", remotePath);
            return remotePath;
            //<video src="https://your-presigned-url" controls autoplay></video>
        } catch (Exception e) {
            log.error("❌ Failed to upload file {} to B2. Reason: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to Backblaze B2", e);
        }
    }
}
