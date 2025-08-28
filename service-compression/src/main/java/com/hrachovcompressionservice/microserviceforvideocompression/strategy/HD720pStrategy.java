package com.hrachovcompressionservice.microserviceforvideocompression.strategy;

import org.springframework.stereotype.Service;

@Service
public class HD720pStrategy implements CompressionStrategy {
    @Override
    public String getFfmpegOptions() { return "-vf scale=-2:720 -crf 23 -preset medium"; } //Сильнее -vf scale=-2:720 -b:v 1500k

    @Override
    public String getQualityKey() { return "720p"; }
}
