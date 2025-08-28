package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.VideoDirectoryDTO;
import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.hrachov.com.filmproject.model.youtube.VideoFrame;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.VideoDirectoryService;
import org.hrachov.com.filmproject.service.VideoFrameService;
import org.hrachov.com.filmproject.service.impl.TextSummarizerImpl;
import org.hrachov.com.filmproject.service.impl.YouTubeSubs;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@AllArgsConstructor
public class VideoDirectoryController {
    private ObjectMapper mapper = new ObjectMapper();
    private VideoDirectoryService videoDirectoryService;
    private VideoFrameService videoFrameService;
    private YouTubeSubs youTubeSubs;
    private TextSummarizerImpl textSummarizer;
    private final CurrentUserService currentUserService;
    private final UserService userService;


    @GetMapping
    public ResponseEntity<List<VideoDirectoryDTO>> getVideoDirectories() {
        //List<VideoDirectory> directories = videoDirectoryService.getAllVideoDirectories(currentUserService.getCurrentUser().getUser().getId());
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        List<VideoDirectory> directories = videoDirectoryService.getAllVideoDirectories(user.getId());
        List<VideoDirectoryDTO> directoryDTOS = directories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(directoryDTOS);
    }
    @PostMapping
    public ResponseEntity<VideoDirectoryDTO> createVideoDirectory(@RequestBody VideoDirectoryDTO videoDirectoryDTO) {
        try{
            videoDirectoryService.addVideoDirectory(videoDirectoryDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<VideoDirectoryDTO> getVideoDirectoryById(@PathVariable int id) {
        VideoDirectory directories = videoDirectoryService.getVideoDirectorieById(id);
        VideoDirectoryDTO directoryDTO = convertToDTO(directories);

        return ResponseEntity.ok(directoryDTO);
    }
    @GetMapping("/summary")
    public ResponseEntity<String> getVideoDirectorySummary(@RequestParam int videoId) throws Exception {

        VideoFrame videoFrame = videoFrameService.getVideoFrame(videoId);

        String url = videoFrame.getUrl();
        String prefix = "https://www.youtube.com/embed/";
        String indenty = url.substring(prefix.length());
        String subs = youTubeSubs.getSubsWithYtdlp(indenty);
        String text = textSummarizer.getSummary(subs);
        String result = cleanUrl(text);

        return ResponseEntity.ok(result);

    }
    private VideoDirectoryDTO convertToDTO(VideoDirectory directory) {
        VideoDirectoryDTO dto = new VideoDirectoryDTO();
        dto.setId(directory.getId());
        List<VideoFrameDTO> frameDTOs = directory.getVideoFrames().stream()
                .map(frame -> {
                    VideoFrameDTO frameDTO = new VideoFrameDTO();
                    frameDTO.setId(frame.getId());
                    frameDTO.setVideoDirectoryId(Long.valueOf(frame.getVideoDirectory() != null ? frame.getVideoDirectory().getId() : null));
                    frameDTO.setTitle(frame.getTitle());
                    frameDTO.setDescription(frame.getDescription());
                    frameDTO.setUrl(frame.getUrl());
                    return frameDTO;
                })
                .collect(Collectors.toList());
        dto.setVideoFrames(frameDTOs);
        return dto;
    }
    private static String cleanUrl(String input) {
        // Remove timestamps and alignment info
        input = input.replaceAll("(?m)^\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> .*", "");
        input = input.replaceAll("(?i)WEBVTT.*", "");
        input = input.replaceAll("(?i)Kind:.*", "");
        input = input.replaceAll("(?i)Language:.*", "");
        input = input.replaceAll("align:start position:\\d+%", "");

        // Remove embedded timestamps inside lines
        input = input.replaceAll("<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>", "");

        // Remove numbering (like 2., 3., 4.)
        input = input.replaceAll("\\d+\\.\\s*", "");

        // Remove multiple spaces and trim
        input = input.replaceAll("\\s+", " ").trim();

        return input;
    }
}
