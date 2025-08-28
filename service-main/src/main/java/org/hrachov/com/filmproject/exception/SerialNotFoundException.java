package org.hrachov.com.filmproject.exception;

public class SerialNotFoundException extends RuntimeException{
    public SerialNotFoundException(String message) {
        super(message);
    }
    public SerialNotFoundException(Long filmId) {
        super("Could not find serial with id " + filmId);
    }
    public SerialNotFoundException(Long filmId,String message) {
        super("Could not find serial with id = " + filmId + " " + message);
    }
}
