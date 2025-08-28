package org.hrachov.com.filmproject.controller;


import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.VideoFrameDTO;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.VideoFrameService;
import org.hrachov.com.filmproject.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;


import java.util.List;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = VideoFrameController.class)
@AutoConfigureMockMvc(addFilters = false) // Add this to disable security filters
public class VideoFrameControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private UserServiceImpl userService;

    @MockBean
    private VideoFrameService videoFrameService;

    // Тест 1: Успешное добавление видеокадра
    @Test
    void addVideo_shouldReturnCreated_whenSuccessful() throws Exception {
        // Arrange
        Long directoryId = 1L;
        VideoFrameDTO dto = new VideoFrameDTO(); // Предполагается, что у DTO есть конструктор или сеттеры
        doNothing().when(videoFrameService).addVideoFrame(dto, directoryId);

        // Act & Assert
        mockMvc.perform(post("/api/videos/{directoryId}/frames", directoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameId\":1,\"frameData\":\"testData\"}")) // Замените на реальные поля DTO
                .andExpect(status().isCreated());
    }

    // Тест 2: Ошибка при добавлении видеокадра
    @Test
    void addVideo_shouldReturnInternalServerError_whenExceptionThrown() throws Exception {
        // Arrange
        Long directoryId = 1L;
        VideoFrameDTO dto = new VideoFrameDTO();
        doThrow(new RuntimeException("Test exception")).when(videoFrameService).addVideoFrame(dto, directoryId);

        // Act & Assert
        mockMvc.perform(post("/api/videos/{directoryId}/frames", directoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameId\":1,\"frameData\":\"testData\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Ошибка добавления видео: Test exception"));
    }

    // Тест 3: Успешное получение списка видеокадров
    @Test
    void getVideos_shouldReturnOk_withListOfVideos() throws Exception {
        // Arrange
        Long directoryId = 1L;
        List<VideoFrameDTO> videos = List.of(
                new VideoFrameDTO(), // Предполагается, что у DTO есть конструктор
                new VideoFrameDTO()
        );
        when(videoFrameService.getAllVideoFramesByDirectoryId(directoryId)).thenReturn(videos);

        // Act & Assert
        mockMvc.perform(get("/api/videos/{directoryId}/frames", directoryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2)); // Проверяем, что возвращается список из 2 элементов
    }

    // Тест 4: Получение пустого списка видеокадров
    @Test
    void getVideos_shouldReturnOk_withEmptyList() throws Exception {
        // Arrange
        Long directoryId = 1L;
        when(videoFrameService.getAllVideoFramesByDirectoryId(directoryId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/videos/{directoryId}/frames", directoryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0)); // Проверяем, что возвращается пустой список
    }
}