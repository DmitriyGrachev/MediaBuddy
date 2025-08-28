package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.youtube.VideoDirectory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoDirectoryRepository extends JpaRepository<VideoDirectory, Integer> {

    VideoDirectory getVideoDirectoryById(int id);

    List<VideoDirectory> getAllByUser_Id(Long userId);
}
