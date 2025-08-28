package org.hrachov.com.filmproject.service;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.VideoDirectoryDTO;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.hrachov.com.filmproject.repository.jpa.VideoDirectoryRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.impl.VideoDirectoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoDirectoryServiceImplTest {

    @Mock
    private VideoDirectoryRepository videoDirectoryRepository;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private VideoDirectoryServiceImpl videoDirectoryService;

    private User user;
    private UserDetails currentUserDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

    }

    @Test
    void getAllVideoDirectories() {
        long userId = 1L;
        VideoDirectory directory = new VideoDirectory();
        when(videoDirectoryRepository.getAllByUser_Id(userId)).thenReturn(Collections.singletonList(directory));

        List<VideoDirectory> result = videoDirectoryService.getAllVideoDirectories(userId);

        assertEquals(1, result.size());
        assertEquals(directory, result.get(0));
        verify(videoDirectoryRepository).getAllByUser_Id(userId);
    }

    @Test
    void getVideoDirectorieById() {
        int directoryId = 1;
        VideoDirectory directory = new VideoDirectory();
        when(videoDirectoryRepository.getVideoDirectoryById(directoryId)).thenReturn(directory);

        VideoDirectory result = videoDirectoryService.getVideoDirectorieById(directoryId);

        assertEquals(directory, result);
        verify(videoDirectoryRepository).getVideoDirectoryById(directoryId);
    }

    @Test
    void addVideoDirectory() {
        VideoDirectoryDTO dto = new VideoDirectoryDTO();
        dto.setDescription("My favorite videos");

        User user = new User();
        user.setUsername("testuser");
        user.setId(1L);
        user.setPassword("password");
        user.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));
        UserDetailsImpl currentUserDetails = new UserDetailsImpl(user);

        when(currentUserService.getCurrentUser()).thenReturn(currentUserDetails);
        when(userService.findByUsername("testuser")).thenReturn(user);
        when(userService.findUserById(1L)).thenReturn(user);

        videoDirectoryService.addVideoDirectory(dto);

        ArgumentCaptor<VideoDirectory> captor = ArgumentCaptor.forClass(VideoDirectory.class);
        verify(videoDirectoryRepository).save(captor.capture());

        VideoDirectory savedDirectory = captor.getValue();
        assertEquals("My favorite videos", savedDirectory.getDescription());
        assertEquals(user, savedDirectory.getUser());
    }
}