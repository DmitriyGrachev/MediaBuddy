package com.hrachovblackblaze.blackblazestreamingmicroservice.serivce;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2NotFoundException;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest; // <-- Убедитесь, что этот импорт есть
import com.backblaze.b2.client.structures.B2FileVersion;          // <-- Убедитесь, что этот импорт есть
import com.backblaze.b2.util.B2ByteRange;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.util.concurrent.CompletableFuture;

@Service
public class B2VideoService {
    private final B2StorageClient b2Client;
    private final String bucketName;

    public B2VideoService(B2StorageClient b2Client, @Value("${b2.bucket}") String bucketName) {
        this.b2Client = b2Client;
        this.bucketName = bucketName;
    }

    public InputStream getVideoStream(String fileName, long start, long end) throws B2Exception {
        B2ByteRange range = new B2ByteRange(start, end);
        B2DownloadByNameRequest request = B2DownloadByNameRequest.builder(bucketName, fileName)
                .setRange(range)
                .build();

        B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();
        b2Client.downloadByName(request, sink);
        byte[] data = sink.getBytes();
        return new ByteArrayInputStream(data);
    }

    public long getFileSize(String fileName) throws B2Exception {
        B2FileVersion fileInfo = b2Client.getFileInfoByName(bucketName, fileName);
        return fileInfo.getContentLength();
    }
}