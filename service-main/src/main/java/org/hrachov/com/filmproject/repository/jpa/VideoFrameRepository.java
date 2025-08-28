package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.youtube.VideoFrame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoFrameRepository extends JpaRepository<VideoFrame, Integer> {
    List<VideoFrame> getAllByVideoDirectory_Id(int videoDirectoryId);
}
