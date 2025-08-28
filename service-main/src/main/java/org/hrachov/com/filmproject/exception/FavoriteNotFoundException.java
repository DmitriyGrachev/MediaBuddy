package org.hrachov.com.filmproject.exception;

public class FavoriteNotFoundException extends RuntimeException {
    public FavoriteNotFoundException(Long filmId) {
        super("Could not find favorite with film id " + filmId);
    }
}
