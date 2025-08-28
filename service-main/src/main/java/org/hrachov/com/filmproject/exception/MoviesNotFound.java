package org.hrachov.com.filmproject.exception;


public class MoviesNotFound extends RuntimeException {
    public MoviesNotFound(String message) {
        super(message);
    }
}
