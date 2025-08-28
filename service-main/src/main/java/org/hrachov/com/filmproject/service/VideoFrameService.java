package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.hrachov.com.filmproject.model.youtube.VideoFrame;

import java.util.ArrayList;
import java.util.List;

public interface VideoFrameService {
    VideoFrame getVideoFrame(int videoId);
    void addVideoFrame(VideoFrameDTO videoFrameDTO, long videoDirectoryId);
    List<VideoFrameDTO> getAllVideoFramesByDirectoryId(long videoDirectoryId);
}
