package org.hrachov.com.filmproject.exception;

public class VideoDirectoryNotFoundException extends RuntimeException{
    public VideoDirectoryNotFoundException(Long id) {
        super("Could not find video directory with id " + id);
    }
}
