package com.hrachovcompressionservice.microserviceforvideocompression.strategy;

import org.springframework.stereotype.Service;

@Service
public class SD480pStrategy implements CompressionStrategy {
    @Override
    public String getFfmpegOptions() { return "-vf scale=-2:460 -crf 23 -preset medium"; }//-vf scale=-2:460 -b:v 800k
    //-acodec aac -b:a 128k - аудио
    @Override
    public String getQualityKey() { return "480p"; }
}
