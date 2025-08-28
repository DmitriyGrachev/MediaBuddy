package org.hrachov.com.filmproject.controller;

import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.service.VideoFrameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoFrameController {

    private final VideoFrameService videoFrameService;

    @PostMapping("/{directoryId}/frames")
    public ResponseEntity<?> addVideo(@PathVariable Long directoryId,
                                      @RequestBody VideoFrameDTO dto) {
        try {
            videoFrameService.addVideoFrame(dto,directoryId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка добавления видео: " + e.getMessage());
        }
    }

    @GetMapping("/{directoryId}/frames")
    public ResponseEntity<List<VideoFrameDTO>> getVideos(@PathVariable Long directoryId) {
        List<VideoFrameDTO> frames = videoFrameService.getAllVideoFramesByDirectoryId(directoryId);
        return ResponseEntity.ok(frames);
    }
}
