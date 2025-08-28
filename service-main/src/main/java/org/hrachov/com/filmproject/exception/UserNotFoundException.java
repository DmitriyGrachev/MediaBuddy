package org.hrachov.com.filmproject.exception;


public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User with id " + id + " not found");
    }
    public UserNotFoundException(String message) {
        super("User with username " + message + " not found");
    }
}
