package org.hrachov.com.filmproject.exception;


public class MovieNotFoundInDb extends RuntimeException {
    public MovieNotFoundInDb(String message) {
        super(message);
    }
}
