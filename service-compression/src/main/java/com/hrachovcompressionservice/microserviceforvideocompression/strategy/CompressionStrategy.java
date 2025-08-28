package com.hrachovcompressionservice.microserviceforvideocompression.strategy;

public interface CompressionStrategy {

    String getFfmpegOptions();

    String getQualityKey();
}
