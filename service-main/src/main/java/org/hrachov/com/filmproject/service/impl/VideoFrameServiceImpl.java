package org.hrachov.com.filmproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.exception.VideoDirectoryNotFoundException;
import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.hrachov.com.filmproject.model.youtube.VideoFrame;
import org.hrachov.com.filmproject.repository.jpa.VideoDirectoryRepository;
import org.hrachov.com.filmproject.repository.jpa.VideoFrameRepository;
import org.hrachov.com.filmproject.service.VideoFrameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoFrameServiceImpl implements VideoFrameService {
    private final ObjectMapper objectMapper;
    private VideoFrameRepository videoFrameRepository;
    private VideoDirectoryRepository videoDirectoryRepository;

    public VideoFrameServiceImpl(VideoFrameRepository videoFrameRepository, ObjectMapper objectMapper, VideoDirectoryRepository videoDirectoryRepository) {
        this.videoFrameRepository = videoFrameRepository;
        this.objectMapper = objectMapper;
        this.videoDirectoryRepository = videoDirectoryRepository;
    }

    public VideoFrame getVideoFrame(int videoId) {
        return videoFrameRepository.findById(videoId).get();
    }

    @Transactional
    public void addVideoFrame(VideoFrameDTO videoFrameDTO, long videoDirectoryId) {
        //https://www.youtube.com/watch?v=axVvZrDz60k
        //https://www.youtube.com/embed/yBrRpb8aLwk
        //VideoDirectory videoDirectory = videoDirectoryRepository.getVideoDirectoryById((int) videoDirectoryId);
        //VideoFrame videoFrame = objectMapper.convertValue(videoFrameDTO, VideoFrame.class);
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.setTitle(videoFrameDTO.getTitle());
        videoFrame.setDescription(videoFrameDTO.getDescription());
        if (videoFrameDTO.getVideoDirectoryId() > 0) {
            VideoDirectory videoDirectory = videoDirectoryRepository.findById(Math.toIntExact(videoFrameDTO.getVideoDirectoryId()))
                    .orElseThrow(() -> new VideoDirectoryNotFoundException(videoFrameDTO.getVideoDirectoryId()));

            videoFrame.setVideoDirectory(videoDirectory);
        }
        videoFrame.setUrl(urlCleaning(videoFrameDTO.getUrl()));
        videoFrameRepository.save(videoFrame);
    }

    public List<VideoFrameDTO> getAllVideoFramesByDirectoryId(long videoDirectoryId) {
        List<VideoFrame> videoFrames = videoFrameRepository.getAllByVideoDirectory_Id((int) videoDirectoryId);
        List<VideoFrameDTO> videoFrameDTOS = new ArrayList<>();
        for (VideoFrame videoFrame : videoFrames) {
            videoFrameDTOS.add(objectMapper.convertValue(videoFrame, VideoFrameDTO.class));
        }
        return videoFrameDTOS;
    }
    public static String urlCleaning(String url){

        if (url.contains("embed/")) {
            return url;
        }else{
            return url.replace("watch?v=", "embed/");
        }
    }
}
