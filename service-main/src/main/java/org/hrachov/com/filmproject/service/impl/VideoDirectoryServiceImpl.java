package org.hrachov.com.filmproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.VideoDirectoryDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;

import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.VideoDirectoryService;
import org.hrachov.com.filmproject.repository.jpa.VideoDirectoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class VideoDirectoryServiceImpl implements VideoDirectoryService {

    private VideoDirectoryRepository videoDirectoryRepository;
    private UserService userService;
    private CurrentUserService currentUserService;


    public List<VideoDirectory> getAllVideoDirectories(long id) {
        return videoDirectoryRepository.getAllByUser_Id(id);
    }

    public VideoDirectory getVideoDirectorieById(int id){
        return videoDirectoryRepository.getVideoDirectoryById(id);
    }
    public void addVideoDirectory(VideoDirectoryDTO videoDirectory) {
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        VideoDirectory videoDirectory1 = new VideoDirectory();
        videoDirectory1.setUser(userService.findUserById(user.getId()));
        videoDirectory1.setDescription(videoDirectory.getDescription());
        videoDirectoryRepository.save(videoDirectory1);
    }
}
