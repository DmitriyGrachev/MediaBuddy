package org.hrachov.com.filmproject.exception;

public class EpisodeFoundException extends RuntimeException {
    public EpisodeFoundException(Long episodeId) {
        super("Episode " + episodeId + " was not found");
    }
}
