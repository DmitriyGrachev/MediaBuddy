package com.hrachovblackblaze.blackblazestreamingmicroservice.controller;
/*
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2NotFoundException;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.util.B2ByteRange;
import com.hrachovblackblaze.blackblazestreamingmicroservice.serivce.B2VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/stream")
public class VideoStreamController {

    private static final Logger logger = LoggerFactory.getLogger(VideoStreamController.class);

    // Set a longer timeout for video streaming (5 minutes)
    private static final long ASYNC_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    // Increase buffer size to 64KB for better streaming performance
    private static final int BUFFER_SIZE = 256 * 1024;

    private final B2VideoService videoService;
    private final String bucketName;
    private final B2StorageClient b2Client;

    public VideoStreamController(B2VideoService videoService,
                                 @Value("${b2.bucket}") String bucketName,
                                 B2StorageClient b2Client) {
        this.videoService = videoService;
        this.bucketName = bucketName;
        this.b2Client = b2Client;
    }

    @GetMapping("/{directory}/{fileName:.+}")
    public DeferredResult<ResponseEntity<StreamingResponseBody>> streamVideo(
            @PathVariable String directory,
            @PathVariable String fileName,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) {

        // Create a DeferredResult with a longer timeout
        DeferredResult<ResponseEntity<StreamingResponseBody>> deferredResult = new DeferredResult<>(ASYNC_TIMEOUT_MS);

        // Set timeout callback
        deferredResult.onTimeout(() -> {
            logger.warn("Request timeout for streaming: {}/{}", directory, fileName);
            deferredResult.setErrorResult(
                    ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                            .body("Video streaming request timed out")
            );
        });

        // Process the streaming request asynchronously
        Callable<ResponseEntity<StreamingResponseBody>> callable = () -> {
            String fullPath = directory + "/" + fileName;
            logger.info("Streaming video from path: {} in bucket: {}", fullPath, bucketName);

            // Prevent directory traversal
            if (fullPath.contains("..")) {
                logger.warn("Invalid file path requested: {}", fullPath);
                return ResponseEntity.badRequest().build();
            }

            long fileLength;
            try {
                fileLength = videoService.getFileSize(fullPath);
            } catch (B2NotFoundException e) {
                logger.error("File not found in Backblaze B2: {}", fullPath, e);
                return ResponseEntity.notFound().build();
            } catch (B2Exception e) {
                logger.error("Error retrieving file size for: {}", fullPath, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing Backblaze B2", e);
            }

            long start;
            long end;
            long rangeLength;
            HttpStatus responseStatus;

            // Handle range requests
            var httpRanges = headers.getRange();
            String requestedRangeStr = "full file"; // По умолчанию, если нет Range хедера
            if (httpRanges == null || httpRanges.isEmpty()) {
                start = 0;
                end = fileLength - 1;
                rangeLength = fileLength;
                responseStatus = HttpStatus.OK;
            } else {
                var httpRange = httpRanges.get(0);
                start = httpRange.getRangeStart(fileLength);
                end = httpRange.getRangeEnd(fileLength);
                rangeLength = end - start + 1;
                responseStatus = HttpStatus.PARTIAL_CONTENT;
                requestedRangeStr = String.format("bytes %d-%d/%d", start, end, fileLength); // Формируем строку диапазона
            }

            // Обновленное логирование
            logger.info("Streaming video from path: {} in bucket: {}. Requested range: {}. File length: {}",
                    fullPath, bucketName, requestedRangeStr, fileLength);

            final long finalStart = start;
            final long finalEnd = end;

            StreamingResponseBody responseBody = outputStream -> {
                B2ByteRange range = new B2ByteRange(finalStart, finalEnd);
                B2DownloadByNameRequest requestB2 = B2DownloadByNameRequest.builder(bucketName, fullPath)
                        .setRange(range)
                        .build();
                // Логируем перед фактическим запросом к B2
                logger.info("Requesting from B2: {}, Range: {} to {}", fullPath, finalStart, finalEnd);
                try {
                    b2Client.downloadByName(requestB2, new B2ContentSink() {
                        @Override
                        public void readContent(B2Headers headers, InputStream inputStream) throws IOException, B2Exception {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int bytesRead;
                            try {
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    try {
                                        outputStream.write(buffer, 0, bytesRead);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        logger.debug("Client disconnected during streaming (OutputStream.write or flush failed): {}", e.getMessage());
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                // Эта ошибка может возникнуть, если inputStream.read() прерван из-за разрыва соединения
                                logger.warn("IO exception during streaming (InputStream.read failed): {}", e.getMessage());
                            } finally {
                                try {
                                    outputStream.flush(); // Последний flush
                                } catch (IOException e) {
                                    logger.debug("IOException during final flush (client likely disconnected): {}", e.getMessage());
                                }
                                // Закрывать outputStream здесь не нужно, Spring это сделает
                            }
                        }
                    });
                    logger.info("Successfully streamed range {} to {} for file {}", finalStart, finalEnd, fullPath);
                } catch (B2NotFoundException e) {
                    logger.error("File not found during streaming: {}", fullPath, e);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found in Backblaze B2", e);
                } catch (B2Exception e) {
                    // Check if this is due to client disconnect or timeout
                    if (e.getMessage().contains("AsyncRequestNotUsableException") ||
                            e.getMessage().contains("Broken pipe")) {
                        logger.debug("Client disconnected during streaming (normal behavior)", e);
                    } else {
                        logger.error("Error streaming file: {}", fullPath, e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error streaming from Backblaze B2", e);
                    }
                }
            };

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(responseStatus)
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header("Accept-Ranges", "bytes")
                    .header("Cache-Control", "public, max-age=31536000")
                    .contentLength(rangeLength);

            if (responseStatus == HttpStatus.PARTIAL_CONTENT) {
                responseBuilder.header("Content-Range", String.format("bytes %d-%d/%d", start, end, fileLength));
            }

            return responseBuilder.body(responseBody);
        };

        // Process asynchronously and set result to DeferredResult
        try {
            ResponseEntity<StreamingResponseBody> result = callable.call();
            deferredResult.setResult(result);
        } catch (Exception e) {
            logger.error("Error processing streaming request", e);
            deferredResult.setErrorResult(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error processing video streaming request")
            );
        }

        return deferredResult;
    }
}

 */