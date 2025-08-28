package org.hrachov.com.filmproject.exception;

public class FilmNotFoundException extends RuntimeException {
    public FilmNotFoundException(String message) {
        super(message);
    }
    public FilmNotFoundException(Long id) {
        super("Film with id " + id + " not found");
    }
}
