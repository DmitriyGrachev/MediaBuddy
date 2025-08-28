package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.dto.VideoDirectoryDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;

import java.util.List;

public interface VideoDirectoryService {
    List<VideoDirectory> getAllVideoDirectories(long id);
    VideoDirectory getVideoDirectorieById(int id);
    void addVideoDirectory(VideoDirectoryDTO videoDirectory);
}
