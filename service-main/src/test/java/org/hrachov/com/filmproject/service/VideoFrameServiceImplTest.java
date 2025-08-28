package org.hrachov.com.filmproject.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.hrachov.com.filmproject.model.youtube.VideoFrame;
import org.hrachov.com.filmproject.repository.jpa.VideoDirectoryRepository;
import org.hrachov.com.filmproject.repository.jpa.VideoFrameRepository;
import org.hrachov.com.filmproject.service.impl.VideoFrameServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoFrameServiceImplTest {

    @Mock
    private VideoFrameRepository videoFrameRepository;

    @Mock
    private VideoDirectoryRepository videoDirectoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VideoFrameServiceImpl videoFrameService;

    @BeforeEach
    void setUp() {
        videoFrameService = new VideoFrameServiceImpl(videoFrameRepository, objectMapper, videoDirectoryRepository);
    }

    @Test
    void getVideoFrame() {
        int videoId = 1;
        VideoFrame videoFrame = new VideoFrame();
        when(videoFrameRepository.findById(videoId)).thenReturn(Optional.of(videoFrame));

        VideoFrame result = videoFrameService.getVideoFrame(videoId);

        assertEquals(videoFrame, result);
        verify(videoFrameRepository).findById(videoId);
    }

    @Test
    void addVideoFrame() {
        long directoryId = 1L;
        VideoFrameDTO dto = new VideoFrameDTO();
        dto.setUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        dto.setTitle("Test Video");
        dto.setVideoDirectoryId(1L);

        VideoDirectory directory = new VideoDirectory();
        directory.setId(1);

        when(videoDirectoryRepository.findById(1)).thenReturn(Optional.of(directory));
        videoFrameService.addVideoFrame(dto, directoryId);


        verify(videoFrameRepository).save(any(VideoFrame.class));
        verify(videoDirectoryRepository,times(1)).findById(1);

    }

    @Test
    void getAllVideoFramesByDirectoryId() {
        long directoryId = 1L;
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.setTitle("Test Video");
        when(videoFrameRepository.getAllByVideoDirectory_Id((int) directoryId)).thenReturn(Collections.singletonList(videoFrame));

        List<VideoFrameDTO> result = videoFrameService.getAllVideoFramesByDirectoryId(directoryId);

        assertEquals(1, result.size());
        assertEquals("Test Video", result.get(0).getTitle());
        verify(videoFrameRepository).getAllByVideoDirectory_Id((int) directoryId);
    }

    @Test
    void urlCleaning_replacesWatchWithEmbed() {
        String originalUrl = "https://www.youtube.com/watch?v=12345";
        String expectedUrl = "https://www.youtube.com/embed/12345";
        assertEquals(expectedUrl, VideoFrameServiceImpl.urlCleaning(originalUrl));
    }

    @Test
    void urlCleaning_doesNotChangeEmbedUrl() {
        String originalUrl = "https://www.youtube.com/embed/12345";
        assertEquals(originalUrl, VideoFrameServiceImpl.urlCleaning(originalUrl));
    }
}