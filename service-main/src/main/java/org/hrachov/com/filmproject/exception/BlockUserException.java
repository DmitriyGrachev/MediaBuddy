package org.hrachov.com.filmproject.exception;

public class BlockUserException extends RuntimeException {
    public BlockUserException(String message) {
        super(message);
    }
    public BlockUserException(Long userId) {
        super("User " + userId + " wasn't blocked. Exception thrown");
    }
}
