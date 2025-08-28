package org.hrachov.com.filmproject.exception;

public class OmdbException extends RuntimeException {
    public OmdbException(String title) {
        super("Omdb found nothing for " + title);
    }
}
